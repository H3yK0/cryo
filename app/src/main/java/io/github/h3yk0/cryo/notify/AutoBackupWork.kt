// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.h3yk0.cryo.CryoApp
import io.github.h3yk0.cryo.MainActivity
import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.data.BackupOutcome
import io.github.h3yk0.cryo.data.BackupStatus
import io.github.h3yk0.cryo.data.BackupTarget
import java.util.concurrent.TimeUnit

/** Agenda o backup automático e avisa quando ele falha. */
object AutoBackupWork {
    const val CHANNEL = "backup"
    private const val PERIODIC = "auto-backup"
    private const val SOON = "auto-backup-soon"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(CHANNEL, "Backup automático", NotificationManager.IMPORTANCE_DEFAULT)
            ch.description = "Avisa quando o backup automático não consegue salvar ou precisa de você"
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    /**
     * Liga ou desliga a conferência periódica. A cada 6 horas o Cryo vê se está na hora do backup
     * (todo dia, toda semana ou todo mês); assim ele acontece mesmo que o celular estivesse desligado no horário.
     */
    fun sync(context: Context, enabled: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (enabled) {
            val req = PeriodicWorkRequestBuilder<AutoBackupWorker>(6, TimeUnit.HOURS).build()
            wm.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, req)
        } else {
            wm.cancelUniqueWork(PERIODIC)
            wm.cancelUniqueWork(SOON)
        }
    }

    /** Ao abrir o app: se o backup estiver atrasado, faz em seguida. */
    fun checkSoon(context: Context) {
        val req = OneTimeWorkRequestBuilder<AutoBackupWorker>().setInitialDelay(30, TimeUnit.SECONDS).build()
        WorkManager.getInstance(context).enqueueUniqueWork(SOON, ExistingWorkPolicy.KEEP, req)
    }

    /** Aviso de backup que não foi salvo: falhou, ou o Cryo segurou para proteger o arquivo. */
    fun notifyProblem(context: Context, outcome: BackupOutcome) {
        if (!Reminders.canNotify(context)) return
        val open = Intent(context, MainActivity::class.java)
            .putExtra(Reminders.EXTRA_OPEN, "backup")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(context, 2, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val where = if (outcome.target == BackupTarget.FOLDER) "na pasta" else "na nuvem"
        val title = if (outcome.status == BackupStatus.HELD) "O Cryo protegeu seu backup $where" else "O backup automático $where falhou"
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(outcome.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(outcome.message))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(if (outcome.target == BackupTarget.FOLDER) 900_001 else 900_002, n)
        } catch (_: SecurityException) {
        }
    }
}

class AutoBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val c = (applicationContext as CryoApp).container
        val before = c.autoBackup.current()
        val outcomes = c.backupRunner.run(force = false)
        // Avisa só quando um destino passa a ter problema; se continuar assim, não repete o aviso a cada tentativa.
        outcomes.filter { (it.status == BackupStatus.FAILED || it.status == BackupStatus.HELD) && before.target(it.target).lastError == null }
            .forEach { AutoBackupWork.notifyProblem(applicationContext, it) }
        return Result.success()
    }
}
