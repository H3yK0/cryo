// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.Bill
import io.github.h3yk0.cryo.data.db.Budget
import io.github.h3yk0.cryo.data.db.Category
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.NoSpendDay
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Snapshot
import io.github.h3yk0.cryo.notify.AlertAction
import io.github.h3yk0.cryo.notify.AlertGroup
import io.github.h3yk0.cryo.notify.AlertKind
import io.github.h3yk0.cryo.notify.AlertPlanner
import io.github.h3yk0.cryo.notify.AlertSlot
import io.github.h3yk0.cryo.notify.AlertState
import io.github.h3yk0.cryo.notify.AlertTone
import io.github.h3yk0.cryo.notify.Alerts
import io.github.h3yk0.cryo.ui.screens.AlertExamples
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * RF-AVI-01..04, 08 e 11: cada situação vira o aviso certo e as regras anti-chatice valem (um aviso comum por dia, só dois
 * horários, nada para quem acabou de chegar, tipo desligado, dispensado 3 vezes = espera o dobro). Dados inventados e um
 * dia fixo: segunda, 09/11/2026.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = TestApp::class)
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class AlertPlannerTest {
    private val today = LocalDate.of(2026, 11, 9)
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()
    private lateinit var state: AlertState

    private val acc = Account(id = 1, name = "Conta", color = 0)
    private val food = Category(id = 10, name = "Mercado", kind = CategoryKind.EXPENSE, icon = "shopping_cart", color = 0)
    private val fun_ = Category(id = 11, name = "Lazer", kind = CategoryKind.EXPENSE, icon = "celebration", color = 0)

    @Before fun limpa() {
        ctx.getSharedPreferences("cryo_alerts", Context.MODE_PRIVATE).edit().clear().commit()
        state = AlertState(ctx)
    }

    private fun tx(amount: Long, date: LocalDate, cat: Long? = food.id) =
        Tx(type = TxType.EXPENSE, amount = amount, date = date, categoryId = cat, accountId = acc.id)

    /** Alguém que já usa o app há um tempo (tem movimentação de 20 dias atrás). */
    private fun settled(vararg more: Tx) = listOf(tx(1_000, today.minusDays(20))) + more

    private fun ledger(txs: List<Tx> = settled(), budgets: List<Budget> = emptyList(), bills: List<Bill> = emptyList(), noSpend: List<NoSpendDay> = emptyList(), day: LocalDate = today) =
        Ledger(Snapshot(accounts = listOf(acc), categories = listOf(food, fun_), txs = txs, budgets = budgets, bills = bills, noSpendDays = noSpend), day)

    private fun plan(l: Ledger, slot: AlertSlot = AlertSlot.MORNING) = AlertPlanner(l, null, state).plan(slot)

    @Test fun contaQueVenceAmanhaAvisaDeManhaUmaVez() {
        val bill = Bill(id = 5, name = "Internet", amount = 12_990, dueDay = 10, startYm = 202601)
        val alerts = plan(ledger(bills = listOf(bill)))
        val a = alerts.single { it.kind == AlertKind.BILL_DUE }
        assertTrue(a.title, a.title.startsWith("Internet"))
        assertEquals(listOf(AlertAction.PAID, AlertAction.SNOOZE), a.actions)
        assertEquals(5L, a.billId)
        state.markShown(a.key, a.kind, today)
        assertTrue(plan(ledger(bills = listOf(bill))).none { it.kind == AlertKind.BILL_DUE })
    }

    @Test fun lembrarAmanhaAdiaAConta() {
        val bill = Bill(id = 5, name = "Internet", amount = 12_990, dueDay = 10, startYm = 202601)
        state.snooze("bill:5:202611", today)
        assertTrue(plan(ledger(bills = listOf(bill))).none { it.kind == AlertKind.BILL_DUE })
    }

    @Test fun soUmAvisoComumPorDia() {
        val l = ledger(
            txs = settled(tx(60_000, today.minusDays(2)), tx(40_000, today.minusDays(1), fun_.id)),
            budgets = listOf(Budget(food.id, 50_000), Budget(fun_.id, 30_000)),
        )
        val all = AlertPlanner(l, null, state).candidates()
        assertEquals(2, all.count { it.kind == AlertKind.BUDGET_OVER })
        val morning = plan(l)
        assertEquals(1, morning.count { !it.kind.urgent })
        val a = morning.single()
        state.markShown(a.key, a.kind, today)
        // O mesmo dia: mais nenhum aviso comum.
        assertTrue(plan(l).isEmpty())
        // No dia seguinte sai o outro (e nunca de novo o mesmo).
        val next = plan(ledger(txs = l.s.txs, budgets = l.s.budgets, day = today.plusDays(1))).single()
        assertTrue(next.key != a.key)
    }

    @Test fun pessoaNovaSoRecebeContas() {
        val bill = Bill(id = 5, name = "Internet", amount = 12_990, dueDay = 10, startYm = 202601)
        val l = ledger(txs = listOf(tx(60_000, today)), budgets = listOf(Budget(food.id, 50_000)), bills = listOf(bill))
        val alerts = plan(l)
        assertEquals(listOf(AlertKind.BILL_DUE), alerts.map { it.kind })
    }

    @Test fun tipoDesligadoNaoAvisa() {
        val l = ledger(txs = settled(tx(60_000, today.minusDays(2))), budgets = listOf(Budget(food.id, 50_000)))
        state.setEnabled(AlertGroup.BUDGET, false)
        assertTrue(plan(l).isEmpty())
        state.setEnabled(AlertGroup.BUDGET, true)
        assertEquals(AlertKind.BUDGET_OVER, plan(l).single().kind)
    }

    @Test fun avisosComunsSoDeManha() {
        val l = ledger(txs = settled(tx(60_000, today)), budgets = listOf(Budget(food.id, 50_000)))
        // À noite: só o lembrete do dia ou o resumo de domingo (aqui, já anotou hoje: nada).
        assertTrue(plan(l, AlertSlot.EVENING).isEmpty())
    }

    @Test fun lembreteDoDiaSoSemNadaAnotado() {
        val evening = plan(ledger(), AlertSlot.EVENING).single()
        assertEquals(AlertKind.DAILY, evening.kind)
        assertEquals(listOf(AlertAction.NO_SPEND, AlertAction.LOG), evening.actions)
        assertEquals("voice", evening.open)
        assertTrue(plan(ledger(txs = settled(tx(500, today))), AlertSlot.EVENING).isEmpty())
        assertTrue(plan(ledger(noSpend = listOf(NoSpendDay(today))), AlertSlot.EVENING).isEmpty())
        state.reminderMinutes = null
        assertTrue(plan(ledger(), AlertSlot.EVENING).isEmpty())
    }

    @Test fun lembreteLembraASequencia() {
        val l = ledger(txs = settled(tx(500, today.minusDays(1)), tx(500, today.minusDays(2)), tx(500, today.minusDays(3))))
        assertEquals(3, AlertPlanner(l, null, state).streak())
        val a = plan(l, AlertSlot.EVENING).single()
        assertTrue(a.text, a.text.contains("3"))
    }

    @Test fun resumoDaSemanaNoDomingo() {
        val sunday = LocalDate.of(2026, 11, 8)
        val l = ledger(txs = settled(tx(4_000, sunday), tx(2_000, sunday.minusDays(3))), day = sunday)
        val a = plan(l, AlertSlot.EVENING).single()
        assertEquals(AlertKind.WEEK, a.kind)
        assertEquals(7, a.week!!.size)
        assertEquals(6_000L, a.week!!.sum())
    }

    @Test fun dispensadoTresVezesEsperaODobro() {
        val l = ledger(txs = settled(tx(45_000, today.minusDays(2))), budgets = listOf(Budget(food.id, 50_000)))
        assertEquals(AlertKind.BUDGET_80, plan(l).single().kind)
        repeat(3) { state.markIgnored(AlertKind.BUDGET_80) }
        state.markShown("outro", AlertKind.BUDGET_80, today.minusDays(3))
        assertTrue(plan(l).isEmpty())
        state.markShown("outro2", AlertKind.BUDGET_80, today.minusDays(4))
        assertEquals(AlertKind.BUDGET_80, plan(l).single().kind)
        // Abrir um aviso desse tipo zera a conta.
        state.markOpened(AlertKind.BUDGET_80)
        assertEquals(0, state.ignored(AlertKind.BUDGET_80))
    }

    @Test fun tomDiscretoEhCurto() {
        val l = ledger(txs = settled(tx(60_000, today.minusDays(2))), budgets = listOf(Budget(food.id, 50_000)))
        state.tone = AlertTone.DISCREET
        val a = plan(l).single()
        assertTrue(a.text, a.text.length < 40)
        assertFalse(a.text.contains("?"))
    }

    /** RF-ORC-05: quando o motor prevê que a categoria passa do limite antes do fim do mês, um aviso por mês, com o dia. */
    @Test fun avisoPeloRitmo() {
        val l = ledger(txs = settled(tx(20_000, today.minusDays(2))), budgets = listOf(Budget(food.id, 50_000)))
        val report = io.github.h3yk0.cryo.domain.engine.FinanceEngine.run(l.s, today)
            .copy(pace = listOf(io.github.h3yk0.cryo.domain.engine.PaceAlert(food.id, 50_000, 20_000, 70_000, 24)))
        val a = AlertPlanner(l, report, state).plan(AlertSlot.MORNING).single()
        assertEquals(AlertKind.BUDGET_PACE, a.kind)
        assertTrue(a.text, a.text.contains("24"))
        state.markShown(a.key, a.kind, today)
        assertTrue(AlertPlanner(ledger(txs = l.s.txs, budgets = l.s.budgets, day = today.plusDays(1)), report, state).plan(AlertSlot.MORNING).none { it.kind == AlertKind.BUDGET_PACE })
    }

    @Test fun exemplosDeTodosOsTiposSemGuardarNada() {
        shadowOf(ctx as android.app.Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        Alerts.createChannels(ctx)
        for (tone in AlertTone.entries) for (g in AlertGroup.entries) {
            val a = AlertExamples.of(g, tone)
            assertEquals(g, a.kind.group)
            assertTrue("$g: ${a.title} / ${a.text}", a.title.isNotBlank() && a.text.isNotBlank() && !Regex("%\\d\\$").containsMatchIn(a.title + a.text))
            assertTrue(Alerts.post(ctx, a, state, record = false))
            assertFalse(state.shown(a.key))
        }
        val nm = ctx.getSystemService(NotificationManager::class.java)
        assertTrue(shadowOf(nm).allNotifications.size >= AlertGroup.entries.size)
        assertNotNull(nm.getNotificationChannel(Alerts.channelOf(AlertKind.DAILY).id))
    }

    @Test fun lembreteNovoReagendaANoite() {
        assertEquals(AlertState.DEFAULT_REMINDER, state.reminderMinutes)
        assertFalse(state.reminderAsked)
        io.github.h3yk0.cryo.ui.screens.saveReminder(ctx, 12 * 60)
        val s = AlertState(ctx)
        assertEquals(12 * 60, s.reminderMinutes)
        assertTrue(s.reminderAsked)
        io.github.h3yk0.cryo.ui.screens.saveReminder(ctx, null)
        assertEquals(null, AlertState(ctx).reminderMinutes)
        assertFalse(AlertState(ctx).enabled(AlertGroup.DAILY))
    }
}
