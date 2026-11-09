// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.notify

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.h3yk0.cryo.MainActivity
import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.domain.Snapshot
import io.github.h3yk0.cryo.ui.components.Art
import io.github.h3yk0.cryo.ui.components.drawArtTile
import io.github.h3yk0.cryo.ui.components.renderArt
import io.github.h3yk0.cryo.ui.theme.ArtColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/** Uma sessão de compras (RF-AVI-10): começou quando, com que teto. */
data class ShoppingSession(val cap: Long, val startMillis: Long) {
    /** O que foi gasto desde que a sessão começou (o que foi anotado depois, com data de hoje). */
    fun spent(s: Snapshot, today: LocalDate): Long =
        s.txs.filter { it.type == TxType.EXPENSE && it.createdAt >= startMillis && it.date == today }.sumOf { it.amount }

    /** Termina sozinha à meia-noite ou depois de 6 horas (o que vier primeiro). */
    fun endsAt(): Long {
        val zone = ZoneId.systemDefault()
        val start = java.time.Instant.ofEpochMilli(startMillis).atZone(zone)
        val midnight = start.toLocalDate().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return minOf(startMillis + Duration.ofHours(6).toMillis(), midnight)
    }
}

/**
 * RF-AVI-10: o modo compras "ao vivo". O Google só deixa "ao vivo" (Live Update, Android 16, e a Now Bar da One UI 8) o
 * que a pessoa começou, está acontecendo e tem fim: aqui é a ida às compras com um teto. No Android 16+, a barra com cores,
 * as marcas de 80% e 100% e o carrinho andando (`ProgressStyle`); antes disso, uma notificação fixa com barra de progresso.
 */
object Shopping {
    private const val ID = 0x5C0001
    private val state = MutableStateFlow<ShoppingSession?>(null)
    val session: StateFlow<ShoppingSession?> = state

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("cryo_shopping", Context.MODE_PRIVATE)

    /** Lê a sessão guardada (o app pode ter sido fechado no meio das compras). */
    fun restore(ctx: Context): ShoppingSession? {
        val p = prefs(ctx)
        val s = if (p.contains("cap")) ShoppingSession(p.getLong("cap", 0), p.getLong("start", 0)) else null
        state.value = s?.takeIf { System.currentTimeMillis() < it.endsAt() }
        if (s != null && state.value == null) stop(ctx)
        return state.value
    }

    fun start(ctx: Context, cap: Long, snapshot: Snapshot?) {
        val s = ShoppingSession(cap, System.currentTimeMillis())
        prefs(ctx).edit().putLong("cap", cap).putLong("start", s.startMillis).apply()
        state.value = s
        val delay = s.endsAt() - s.startMillis
        runCatching {
            WorkManager.getInstance(ctx).enqueueUniqueWork(
                "shopping-end", ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<ShoppingEndWorker>().setInitialDelay(delay, TimeUnit.MILLISECONDS).build(),
            )
        }
        refresh(ctx, snapshot)
    }

    fun stop(ctx: Context) {
        prefs(ctx).edit().clear().apply()
        state.value = null
        runCatching { NotificationManagerCompat.from(ctx).cancel(ID) }
        runCatching { WorkManager.getInstance(ctx).cancelUniqueWork("shopping-end") }
    }

    /** Atualiza a notificação com o que foi anotado (chamado a cada mudança dos dados de verdade). */
    fun refresh(ctx: Context, snapshot: Snapshot?) {
        val s = state.value ?: return
        if (System.currentTimeMillis() >= s.endsAt()) { stop(ctx); return }
        if (snapshot == null || !Reminders.canNotify(ctx)) return
        val spent = s.spent(snapshot, LocalDate.now())
        runCatching { NotificationManagerCompat.from(ctx).notify(ID, build(ctx, s, spent)) }
    }

    fun build(ctx: Context, s: ShoppingSession, spent: Long): android.app.Notification {
        val left = s.cap - spent
        val open = Intent(ctx, MainActivity::class.java).putExtra(Reminders.EXTRA_OPEN, "voice")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val logPi = PendingIntent.getActivity(ctx, ID, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val endPi = PendingIntent.getBroadcast(
            ctx, ID + 1, Intent(ctx, AlertActionReceiver::class.java).setAction("${ctx.packageName}.shopping.end").putExtra("action", AlertActionReceiver.SHOP_END),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val text = if (left >= 0) Res.s(R.string.shop_left, Money.format(left)) else Res.s(R.string.shop_over, Money.format(-left))
        val b = NotificationCompat.Builder(ctx, Alerts.Channel.SHOPPING.id)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(Res.s(R.string.shop_title, Money.format(spent), Money.format(s.cap)))
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(
                NotificationCompat.Builder(ctx, Alerts.Channel.SHOPPING.id).setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(Res.s(R.string.shop_public)).build(),
            )
            .setContentIntent(logPi)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .addAction(0, Res.s(R.string.alert_action_log), logPi)
            .addAction(0, Res.s(R.string.shop_end), endPi)
            // O chip da barra de status e a Now Bar mostram o quanto falta (no máximo ~7 letras cabem inteiras).
            .setShortCriticalText(Money.compact(maxOf(left, 0)))
            .setRequestPromotedOngoing(true)
        val max = 1200
        val progress = (spent.toDouble() / s.cap.coerceAtLeast(1) * 1000).toInt().coerceIn(0, max)
        if (Build.VERSION.SDK_INT >= 36) {
            val style = NotificationCompat.ProgressStyle()
                .setProgressSegments(
                    listOf(
                        NotificationCompat.ProgressStyle.Segment(800).setColor(ArtColors.green.toArgb()),
                        NotificationCompat.ProgressStyle.Segment(200).setColor(ArtColors.amber.toArgb()),
                        NotificationCompat.ProgressStyle.Segment(200).setColor(ArtColors.red.toArgb()),
                    ),
                )
                .setProgressPoints(
                    listOf(
                        NotificationCompat.ProgressStyle.Point(800).setColor(ArtColors.amber.toArgb()),
                        NotificationCompat.ProgressStyle.Point(1000).setColor(ArtColors.redDeep.toArgb()),
                    ),
                )
                .setStyledByProgress(true)
                .setProgress(progress)
                .setProgressTrackerIcon(IconCompat.createWithBitmap(renderArt(96) { drawArtTile(Art.SHOPPING) }))
            b.setStyle(style)
        } else {
            b.setProgress(max, progress, false)
        }
        return b.build()
    }
}

/** Fim automático da sessão (meia-noite ou 6 horas). */
class ShoppingEndWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Shopping.stop(applicationContext)
        return Result.success()
    }
}

