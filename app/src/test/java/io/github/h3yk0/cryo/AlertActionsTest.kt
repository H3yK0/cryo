// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import android.Manifest
import android.app.NotificationManager
import android.media.AudioManager
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.Bill
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.notify.Alert
import io.github.h3yk0.cryo.notify.AlertAction
import io.github.h3yk0.cryo.notify.AlertActionReceiver
import io.github.h3yk0.cryo.notify.AlertGroup
import io.github.h3yk0.cryo.notify.AlertKind
import io.github.h3yk0.cryo.notify.AlertState
import io.github.h3yk0.cryo.notify.AlertTone
import io.github.h3yk0.cryo.notify.Alerts
import io.github.h3yk0.cryo.notify.Sound
import io.github.h3yk0.cryo.notify.Sounds
import io.github.h3yk0.cryo.ui.screens.AlertExamples
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * RF-AVI-03/05/09 e RF-SOM-02, com o app de verdade (CryoApp): os botões da notificação mudam os dados de verdade uma vez
 * só, os que mudam dados pedem o desbloqueio, a versão da tela de bloqueio não mostra nome nem valor, a figurinha vai no
 * estilo de conversa e os sons respeitam o celular.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class AlertActionsTest {
    private val app: CryoApp get() = ApplicationProvider.getApplicationContext()
    private val today = LocalDate.now()

    @Before fun setUp() {
        runBlocking { app.real.repo.eraseAll() }
        app.getSharedPreferences("cryo_alerts", 0).edit().clear().commit()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        Alerts.createChannels(app)
    }

    @After fun tearDown() { Sounds.testLog = null }

    private fun send(action: String, a: Alert) = runBlocking {
        AlertActionReceiver().handle(app, AlertActionReceiver.intent(app, action, a, Alerts.idOf(a)))
    }

    private fun waitFor(cond: () -> Boolean) {
        val end = System.currentTimeMillis() + 5_000
        while (!cond() && System.currentTimeMillis() < end) { Thread.sleep(20); shadowOf(android.os.Looper.getMainLooper()).idle() }
        assertTrue(cond())
    }

    @Test fun jaPagueiMarcaAContaUmaVezSo() {
        val acc = runBlocking { app.real.repo.saveAccount(Account(name = "Conta", color = 0)) }
        val billId = runBlocking { app.real.repo.saveBill(Bill(name = "Internet", amount = 12_990, dueDay = today.dayOfMonth, startYm = 202601, accountId = acc)) }
        val ym = today.year * 100 + today.monthValue
        val a = Alert(AlertKind.BILL_DUE, "bill:$billId", "Internet", "", listOf(AlertAction.PAID), billId = billId, ym = ym)
        send(AlertAction.PAID.name, a)
        waitFor { runBlocking { app.real.repo.loadOnce() }.txs.any { it.billId == billId } }
        send(AlertAction.PAID.name, a)
        val paid = runBlocking { app.real.repo.loadOnce() }.txs.filter { it.billId == billId }
        assertEquals(1, paid.size)
        assertEquals(12_990L, paid.single().amount)
    }

    /** ERR-045 (RF-CFX-06): com o pagamento já no extrato, "Já paguei" na notificação liga a ele; não nasce um segundo. */
    @Test fun jaPagueiComOPagamentoNoExtratoSoLiga() {
        val acc = runBlocking { app.real.repo.saveAccount(Account(name = "Conta", color = 0)) }
        val billId = runBlocking { app.real.repo.saveBill(Bill(name = "Internet", amount = 12_990, dueDay = today.dayOfMonth, startYm = 202601, accountId = acc)) }
        // A linha do banco, importada antes de a pessoa tocar no aviso (que já estava na cortina).
        val imported = runBlocking {
            app.real.repo.saveTx(io.github.h3yk0.cryo.data.db.Tx(type = io.github.h3yk0.cryo.data.db.TxType.EXPENSE, amount = 12_990, date = today, description = "INTERNET FIBRA SA", accountId = acc, importKey = "a$acc|ofx.id:77"))
        }
        val ym = today.year * 100 + today.monthValue
        send(AlertAction.PAID.name, Alert(AlertKind.BILL_DUE, "bill:$billId", "Internet", "", listOf(AlertAction.PAID), billId = billId, ym = ym))
        waitFor { runBlocking { app.real.repo.loadOnce() }.txs.any { it.billId == billId } }
        val txs = runBlocking { app.real.repo.loadOnce() }.txs
        assertEquals("uma movimentação só", 1, txs.size)
        assertEquals(imported, txs.single().id)
        assertEquals(ym, txs.single().billYm)
        assertEquals("a$acc|ofx.id:77", txs.single().importKey)
    }

    @Test fun diaSemGastosERegistrarSalario() {
        runBlocking { app.real.repo.saveAccount(Account(name = "Conta", color = 0)) }
        send(AlertAction.NO_SPEND.name, Alert(AlertKind.DAILY, "daily", "", ""))
        waitFor { runBlocking { app.real.repo.loadOnce() }.noSpendDays.any { it.date == today } }
        send(AlertAction.REGISTER.name, Alert(AlertKind.SALARY_DAY, "salary", "", "", amount = 420_000, label = "Salário"))
        waitFor { runBlocking { app.real.repo.loadOnce() }.txs.any { it.type == TxType.INCOME && it.amount == 420_000L && it.description == "Salário" } }
    }

    @Test fun dispensarSemAbrirContaParaEspacar() {
        send(AlertActionReceiver.DISMISSED, Alert(AlertKind.OUTLIER, "x", "", ""))
        waitFor { AlertState(app).ignored(AlertKind.OUTLIER) == 1 }
    }

    @Test fun botoesQueMudamDadosPedemDesbloqueioEFigurinhaNaConversa() {
        val state = AlertState(app)
        val bills = AlertExamples.of(AlertGroup.BILLS, AlertTone.FRIEND)
        assertTrue(Alerts.post(app, bills, state, record = false))
        val nm = app.getSystemService(NotificationManager::class.java)
        val n = shadowOf(nm).getNotification(Alerts.idOf(bills))
        // "Já paguei" pede o desbloqueio; "Lembrar amanhã" não muda dados.
        assertTrue(n.actions[0].isAuthenticationRequired)
        assertFalse(n.actions[1].isAuthenticationRequired)
        // RF-AVI-05: na tela de bloqueio, nem nome nem valor.
        assertEquals(android.app.Notification.VISIBILITY_PRIVATE, n.visibility)
        val pub = n.publicVersion
        val pubText = "${pub.extras.getCharSequence(android.app.Notification.EXTRA_TITLE)} ${pub.extras.getCharSequence(android.app.Notification.EXTRA_TEXT)}"
        assertFalse(pubText, pubText.contains("Internet") || pubText.contains("129"))
        // RF-AVI-09: uma mensagem com a figurinha animada (WebP), servida pelo FileProvider do app.
        val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(n)!!
        val sticker = style.messages.last()
        assertEquals("image/webp", sticker.dataMimeType)
        assertEquals("${app.packageName}.stickers", sticker.dataUri!!.authority)
    }

    /** ERR-011: dois avisos diferentes não podem dividir o mesmo `PendingIntent` (o segundo abriria a tela do primeiro). */
    @Test fun cadaAvisoAbreASuaTela() {
        val state = AlertState(app)
        val nm = app.getSystemService(NotificationManager::class.java)
        val actions = listOf(AlertGroup.BILLS, AlertGroup.BUDGET, AlertGroup.GOALS).map { g ->
            val a = AlertExamples.of(g, AlertTone.FRIEND)
            assertTrue(Alerts.post(app, a, state, record = false))
            shadowOf(shadowOf(nm).getNotification(Alerts.idOf(a)).contentIntent).savedIntent.action
        }
        assertEquals(actions.toString(), 3, actions.filterNotNull().toSet().size)
    }

    @Test fun sonsRespeitamOCelularEOAjuste() {
        Sounds.testLog = mutableListOf()
        val audio = app.getSystemService(AudioManager::class.java)
        audio.ringerMode = AudioManager.RINGER_MODE_NORMAL
        Sounds.play(app, Sound.SAVE)
        assertEquals(listOf(Sound.SAVE), Sounds.testLog)
        audio.ringerMode = AudioManager.RINGER_MODE_SILENT
        Sounds.play(app, Sound.SAVE)
        audio.ringerMode = AudioManager.RINGER_MODE_VIBRATE
        Sounds.play(app, Sound.LIMIT)
        audio.ringerMode = AudioManager.RINGER_MODE_NORMAL
        AlertState(app).sounds = false
        Sounds.play(app, Sound.ACHIEVEMENT)
        assertEquals(listOf(Sound.SAVE), Sounds.testLog)
    }
}
