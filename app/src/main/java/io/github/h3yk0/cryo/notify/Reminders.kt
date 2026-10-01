// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.h3yk0.cryo.CryoApp
import io.github.h3yk0.cryo.MainActivity
import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.domain.BillState
import io.github.h3yk0.cryo.domain.Dates
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.domain.ym
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object Reminders {
    const val CHANNEL = "bills"
    const val EXTRA_OPEN = "open"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(CHANNEL, "Lembretes de contas", NotificationManager.IMPORTANCE_DEFAULT)
            ch.description = "Avisos de contas fixas perto do vencimento"
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    /** Verificação diária, por volta das 9h. */
    fun schedule(context: Context) {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(9, 0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = Duration.between(now, next).toMinutes()
        val req = PeriodicWorkRequestBuilder<BillReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("bill-reminders", ExistingPeriodicWorkPolicy.KEEP, req)
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}

/** Quais contas fixas devem gerar aviso hoje (função pura, testável). */
fun dueReminders(l: Ledger): List<io.github.h3yk0.cryo.domain.BillStatus> {
    val today = l.today
    return (l.billStatuses(today.ym()) + l.billStatuses(today.ym().plusMonths(1)))
        .filter { it.bill.remind && it.bill.kind == CategoryKind.EXPENSE && it.state != BillState.PAID }
        .filter { it.daysUntil == it.bill.remindDaysBefore.toLong() || it.daysUntil == 0L || it.daysUntil == -1L }
}

fun reminderTitle(s: io.github.h3yk0.cryo.domain.BillStatus, today: LocalDate): String {
    val whenTxt = when {
        s.daysUntil < 0 -> "venceu ontem"
        s.daysUntil == 0L -> "vence hoje"
        s.daysUntil == 1L -> "vence amanhã"
        else -> "vence ${Dates.relative(s.due, today)}"
    }
    return "${s.bill.name} $whenTxt"
}

class BillReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as CryoApp
        val c = app.container
        if (!c.settings.current().remindersEnabled || !Reminders.canNotify(applicationContext)) return Result.success()
        val today = LocalDate.now()
        val ledger = Ledger(c.repo.loadOnce(), today)
        val statuses = dueReminders(ledger)

        val nm = NotificationManagerCompat.from(applicationContext)
        val open = Intent(applicationContext, MainActivity::class.java)
            .putExtra(Reminders.EXTRA_OPEN, "bills")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(applicationContext, 0, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        for (s in statuses) {
            val n = NotificationCompat.Builder(applicationContext, Reminders.CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(reminderTitle(s, today))
                .setContentText("${Money.format(s.bill.amount)} · toque para marcar como paga")
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build()
            try {
                nm.notify((s.bill.id * 100 + s.ym.monthValue).toInt(), n)
            } catch (_: SecurityException) {
            }
        }
        return Result.success()
    }
}
