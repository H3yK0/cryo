// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import io.github.h3yk0.cryo.MainActivity
import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.ui.components.Art
import io.github.h3yk0.cryo.ui.components.drawArtTile
import io.github.h3yk0.cryo.ui.components.renderArt
import io.github.h3yk0.cryo.ui.theme.ArtColors
import java.io.File
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * RF-AVI: os avisos no Android. Cada aviso vem como **uma mensagem do Cryo com a figurinha animada da situação** (RF-AVI-09):
 * o Android só dá "play" em imagem animada dentro do estilo de conversa, com a cortina aberta ou com o aviso aparecendo por
 * cima da tela, e para quando fecha. Na tela de bloqueio, só "Novo aviso do Cryo" (RF-AVI-05).
 */
object Alerts {
    const val EXTRA_KIND = "alertKind"

    /** RF-AVI-06: um canal por tipo, cada um com o seu som (RF-SOM); a pessoa ajusta cada um no próprio Android. */
    enum class Channel(val id: String, val nameRes: Int, val descRes: Int, val importance: Int, val sound: String?) {
        BILLS("avisos_contas", R.string.channel_bills, R.string.channel_bills_desc, NotificationManager.IMPORTANCE_HIGH, "som_notificacao"),
        BUDGET("avisos_orcamento", R.string.channel_budget, R.string.channel_budget_desc, NotificationManager.IMPORTANCE_HIGH, "som_atencao"),
        DAILY_LIFE("avisos_dia_a_dia", R.string.channel_everyday, R.string.channel_everyday_desc, NotificationManager.IMPORTANCE_DEFAULT, "som_notificacao"),
        WINS("avisos_conquistas", R.string.channel_wins, R.string.channel_wins_desc, NotificationManager.IMPORTANCE_DEFAULT, "som_conquista"),
        SUMMARIES("avisos_resumos", R.string.channel_summaries, R.string.channel_summaries_desc, NotificationManager.IMPORTANCE_DEFAULT, "som_notificacao"),
        HABIT("avisos_lembrete", R.string.channel_habit, R.string.channel_habit_desc, NotificationManager.IMPORTANCE_HIGH, "som_sequencia"),
        SHOPPING("compras_ao_vivo", R.string.channel_shopping, R.string.channel_shopping_desc, NotificationManager.IMPORTANCE_LOW, null),
    }

    fun channelOf(kind: AlertKind): Channel = when (kind.group) {
        AlertGroup.BILLS -> Channel.BILLS
        AlertGroup.BUDGET -> Channel.BUDGET
        AlertGroup.CARDS, AlertGroup.RECURRING, AlertGroup.WATCH -> Channel.DAILY_LIFE
        AlertGroup.GOALS -> Channel.WINS
        AlertGroup.SUMMARIES -> Channel.SUMMARIES
        AlertGroup.DAILY -> Channel.HABIT
    }

    /** A ilustração de cada situação (RF-ILU-02). */
    fun artOf(kind: AlertKind): Art = when (kind) {
        AlertKind.BILL_DUE, AlertKind.DEBT_DUE -> Art.BILL
        AlertKind.BUDGET_OVER -> Art.LIMIT
        AlertKind.BUDGET_80, AlertKind.BUDGET_PACE -> Art.BUDGET
        AlertKind.NEGATIVE -> Art.NEGATIVE
        AlertKind.SALARY_DAY, AlertKind.SALARY_LATE -> Art.SALARY
        AlertKind.DUPLICATE -> Art.DUPLICATE
        AlertKind.INVOICE_CLOSES -> Art.INVOICE
        AlertKind.PRICE_UP -> Art.PRICE_UP
        AlertKind.OUTLIER -> Art.OUTLIER
        AlertKind.MONTH_CLOSE -> Art.MONTH
        AlertKind.LAST_INSTALLMENT -> Art.LAST_INSTALLMENT
        AlertKind.GOAL_NEAR -> Art.GOAL_NEAR
        AlertKind.HEALTH_UP -> Art.HEALTH
        AlertKind.WEEK -> Art.WEEK
        AlertKind.DAILY -> Art.STREAK
    }

    fun soundUri(ctx: Context, name: String): Uri =
        Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${ctx.packageName}/raw/$name")

    /** Cria os canais (e tira o canal antigo dos lembretes, que agora é "Contas e dívidas"). */
    fun createChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        for (c in Channel.entries) {
            val ch = NotificationChannel(c.id, Res.s(c.nameRes), c.importance)
            ch.description = Res.s(c.descRes)
            if (c.sound != null) ch.setSound(soundUri(ctx, c.sound), attrs) else ch.setSound(null, null)
            nm.createNotificationChannel(ch)
        }
        runCatching { nm.deleteNotificationChannel(Reminders.CHANNEL) }
    }

    /**
     * O horário da noite (RF-AVI-11): o lembrete do dia no horário da pessoa. Sem lembrete, o resumo de domingo continua no
     * horário padrão; sem os dois, nada roda à noite.
     */
    fun scheduleEvening(ctx: Context, state: AlertState) {
        val wm = WorkManager.getInstance(ctx)
        val minutes = state.reminderMinutes ?: AlertState.DEFAULT_REMINDER.takeIf { state.enabled(AlertGroup.SUMMARIES) }
        if (minutes == null) { wm.cancelUniqueWork(EVENING); return }
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(minutes / 60, minutes % 60)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val req = PeriodicWorkRequestBuilder<EveningAlertWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(Duration.between(now, next).toMinutes(), TimeUnit.MINUTES)
            .build()
        wm.enqueueUniquePeriodicWork(EVENING, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    private const val EVENING = "evening-alerts"

    /* ------------------------------------------ figurinhas ------------------------------------------ */

    /** A figurinha de cada ilustração (lista escrita, conferida na compilação: se faltar um arquivo, o app nem compila). */
    fun stickerRes(art: Art): Int = when (art) {
        Art.BUDGET -> R.raw.sticker_budget
        Art.LIMIT -> R.raw.sticker_limit
        Art.INVOICE -> R.raw.sticker_invoice
        Art.BILL -> R.raw.sticker_bill
        Art.SALARY -> R.raw.sticker_salary
        Art.NO_SPEND -> R.raw.sticker_no_spend
        Art.STREAK -> R.raw.sticker_streak
        Art.WEEK -> R.raw.sticker_week
        Art.MONTH -> R.raw.sticker_month
        Art.PRICE_UP -> R.raw.sticker_price_up
        Art.DUPLICATE -> R.raw.sticker_duplicate
        Art.OUTLIER -> R.raw.sticker_outlier
        Art.NEGATIVE -> R.raw.sticker_negative
        Art.LAST_INSTALLMENT -> R.raw.sticker_last_installment
        Art.HEALTH -> R.raw.sticker_health
        Art.GOAL_NEAR -> R.raw.sticker_goal_near
        Art.DEBT_FREE -> R.raw.sticker_debt_free
        Art.CELEBRATE -> R.raw.sticker_celebrate
        Art.SHOPPING -> R.raw.sticker_shopping
    }

    /** A figurinha animada guardada no cache e o endereço que o Android usa para lê-la (RF-AVI-09). */
    fun stickerUri(ctx: Context, art: Art): Uri? = runCatching {
        val res = stickerRes(art)
        val dir = File(ctx.cacheDir, "stickers").apply { mkdirs() }
        val file = File(dir, "sticker_${art.name.lowercase()}.webp")
        // Copia de novo se o arquivo do cache não é o desta versão do app (figurinha redesenhada numa atualização).
        val size = runCatching { ctx.resources.openRawResourceFd(res).use { it.length } }.getOrDefault(-1L)
        if (!file.exists() || file.length() == 0L || (size > 0 && file.length() != size)) {
            ctx.resources.openRawResource(res).use { input -> file.outputStream().use { input.copyTo(it) } }
        }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.stickers", file)
        // O Android já dá a permissão sozinho para quem desenha a notificação; aqui, por garantia.
        ctx.grantUriPermission("com.android.systemui", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        uri
    }.getOrNull()

    private fun badge(ctx: Context): Bitmap? = runCatching {
        ContextCompat.getDrawable(ctx, R.drawable.ic_cryo_badge)?.toBitmap(192, 192)
    }.getOrNull()

    /** O gráfico da semana (RF-AVI-07), desenhado no aparelho com os dados da pessoa. */
    fun weekChart(values: List<Long>, normal: Long): Bitmap {
        val w = 960f
        val h = 420f
        val img = androidx.compose.ui.graphics.ImageBitmap(w.toInt(), h.toInt())
        val canvas = androidx.compose.ui.graphics.Canvas(img)
        androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(
            androidx.compose.ui.unit.Density(1f), androidx.compose.ui.unit.LayoutDirection.Ltr, canvas, Size(w, h),
        ) {
            drawRoundRect(ArtColors.ice, Offset.Zero, Size(w, h), CornerRadius(36f))
            val max = (values + normal).max().coerceAtLeast(1L).toFloat()
            val bw = w / values.size
            val base = h - 40f
            val top = 40f
            val ny = base - (base - top) * normal / max
            drawLine(ArtColors.iceMid, Offset(30f, ny), Offset(w - 30f, ny), 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f)))
            values.forEachIndexed { i, v ->
                val bh = ((base - top) * v / max).coerceAtLeast(10f)
                drawRoundRect(
                    if (v > normal && normal > 0) ArtColors.gold else ArtColors.deep,
                    Offset(i * bw + bw * 0.24f, base - bh), Size(bw * 0.52f, bh), CornerRadius(16f),
                )
            }
        }
        return img.asAndroidBitmap()
    }

    /* ------------------------------------------- enviar ------------------------------------------- */

    fun idOf(a: Alert): Int = 0x43_0000 + (a.key.hashCode() and 0xFFFF)

    /** Monta e mostra o aviso. Devolve false se o Android não deixou (sem permissão). [record] = false nos exemplos. */
    fun post(ctx: Context, a: Alert, state: AlertState, record: Boolean = true): Boolean {
        if (!Reminders.canNotify(ctx)) return false
        val channel = channelOf(a.kind)
        val id = idOf(a)
        // Ação única por aviso: o Android compara dois pedidos pelo Intent **sem** os extras; dois avisos com códigos
        // vizinhos podiam trocar o destino um do outro.
        val open = Intent(ctx, MainActivity::class.java)
            .setAction("${ctx.packageName}.alert.open.$id")
            .putExtra(Reminders.EXTRA_OPEN, a.open)
            .putExtra(EXTRA_KIND, a.kind.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val contentPi = PendingIntent.getActivity(ctx, id, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val dismissPi = PendingIntent.getBroadcast(
            ctx, id + 1, AlertActionReceiver.intent(ctx, AlertActionReceiver.DISMISSED, a, id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val art = artOf(a.kind)
        val b = NotificationCompat.Builder(ctx, channel.id)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(a.title)
            .setContentText(a.text)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion(ctx, channel))
            .setContentIntent(contentPi)
            .setDeleteIntent(dismissPi)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setColor(ArtColors.deep.toArgb())
            .setCategory(if (a.kind.urgent) NotificationCompat.CATEGORY_REMINDER else NotificationCompat.CATEGORY_RECOMMENDATION)

        if (a.kind == AlertKind.WEEK && a.week != null) {
            // O resumo da semana: o gráfico com os dados da pessoa (imagem parada, RF-AVI-07).
            val chart = weekChart(a.week, a.weekNormal ?: 0L)
            runCatching { renderArt(192) { drawArtTile(art) } }.getOrNull()?.let(b::setLargeIcon)
            b.setStyle(NotificationCompat.BigPictureStyle().bigPicture(chart).setSummaryText(a.text))
        } else {
            // RF-AVI-09: a mensagem do Cryo com a figurinha animada.
            val now = System.currentTimeMillis()
            val cryo = Person.Builder().setName(a.title).setKey("cryo").also { p ->
                badge(ctx)?.let { p.setIcon(IconCompat.createWithBitmap(it)) }
            }.build()
            val you = Person.Builder().setName(Res.s(R.string.alert_you)).setKey("you").build()
            val style = NotificationCompat.MessagingStyle(you).setGroupConversation(false)
                .addMessage(NotificationCompat.MessagingStyle.Message(a.text, now, cryo))
            stickerUri(ctx, art)?.let { uri ->
                style.addMessage(NotificationCompat.MessagingStyle.Message(a.title, now + 1, cryo).setData("image/webp", uri))
            }
            b.setStyle(style)
        }
        a.actions.forEach { act -> action(ctx, a, act, id)?.let(b::addAction) }
        return try {
            NotificationManagerCompat.from(ctx).notify(id, b.build())
            if (record) state.markShown(a.key, a.kind, java.time.LocalDate.now())
            true
        } catch (_: SecurityException) {
            false
        }
    }

    private fun action(ctx: Context, a: Alert, act: AlertAction, id: Int): NotificationCompat.Action? {
        val (label, changesData) = when (act) {
            AlertAction.PAID -> Res.s(R.string.alert_action_paid) to true
            AlertAction.SNOOZE -> Res.s(R.string.alert_action_snooze) to false
            AlertAction.NO_SPEND -> Res.s(R.string.alert_action_no_spend) to true
            AlertAction.LOG -> Res.s(R.string.alert_action_log) to false
            AlertAction.REGISTER -> Res.s(R.string.alert_action_register, io.github.h3yk0.cryo.domain.Money.format(a.amount ?: return null)) to true
            AlertAction.OPEN -> Res.s(R.string.alert_action_open) to false
        }
        val pi = if (act == AlertAction.LOG || act == AlertAction.OPEN) {
            val open = Intent(ctx, MainActivity::class.java)
                .setAction("${ctx.packageName}.alert.${act.name}.$id")
                .putExtra(Reminders.EXTRA_OPEN, if (act == AlertAction.LOG) "voice" else a.open)
                .putExtra(EXTRA_KIND, a.kind.name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            PendingIntent.getActivity(ctx, id + 10 + act.ordinal, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        } else {
            PendingIntent.getBroadcast(
                ctx, id + 10 + act.ordinal, AlertActionReceiver.intent(ctx, act.name, a, id),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
        // RF-AVI-03: o que muda os dados pede o desbloqueio do celular (Android 12+).
        return NotificationCompat.Action.Builder(0, label, pi).setAuthenticationRequired(changesData).build()
    }

    /** RF-AVI-05: o que aparece com o celular bloqueado. */
    private fun publicVersion(ctx: Context, channel: Channel) = NotificationCompat.Builder(ctx, channel.id)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(Res.s(R.string.alert_public_title))
        .setContentText(Res.s(R.string.alert_public_text))
        .build()
}
