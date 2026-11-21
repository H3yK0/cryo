// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.h3yk0.cryo.data.InboxStore
import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.Bill
import io.github.h3yk0.cryo.data.db.Budget
import io.github.h3yk0.cryo.data.db.Category
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.data.db.Debt
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.Inbox
import io.github.h3yk0.cryo.domain.InboxKind
import io.github.h3yk0.cryo.domain.InboxLevel
import io.github.h3yk0.cryo.domain.InboxTarget
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Snapshot
import io.github.h3yk0.cryo.domain.engine.FinanceEngine
import io.github.h3yk0.cryo.domain.predict.MonthEnds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** RF-INI-19 e RF-INI-20 (reforma visual da 1.6, LA9): a central de avisos, o sino e o destaque no momento oportuno. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = TestApp::class)
class InboxTest {
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()
    private val today = LocalDate.of(2026, 11, 21)
    private val cats = listOf(Category(1, "Mercado", CategoryKind.EXPENSE, "cart", 0), Category(2, "Lazer", CategoryKind.EXPENSE, "movie", 0))

    private fun world(): Snapshot = Snapshot(
        accounts = listOf(Account(1, "Conta", initialBalance = 30_000, color = 0)),
        categories = cats,
        cards = listOf(CreditCard(1, "Roxo", limitAmount = 500_000, closingDay = 15, dueDay = 24, color = 0, payAccountId = 1)),
        bills = listOf(
            Bill(1, "Aluguel", 90_000, dueDay = 18, accountId = 1, startYm = 202611),   // venceu
            Bill(2, "Internet", 10_000, dueDay = 23, accountId = 1, startYm = 202611),  // em 2 dias
            Bill(3, "Academia", 9_000, dueDay = 30, accountId = 1, startYm = 202611),   // longe: não entra
        ),
        debts = listOf(Debt(1, "Empréstimo", color = 0, initialBalance = 100_000, installmentAmount = 20_000, installmentCount = 5, dueDay = 10, startYm = 202611, accountId = 1)),
        budgets = listOf(Budget(1, 10_000), Budget(2, 10_000)),
        txs = listOf(
            Tx(1, TxType.EXPENSE, 12_000, LocalDate.of(2026, 11, 3), "Mercado", accountId = 1, categoryId = 1),  // orçamento estourado
            Tx(2, TxType.EXPENSE, 8_500, LocalDate.of(2026, 11, 4), "Cinema", accountId = 1, categoryId = 2),    // 85%
            Tx(3, TxType.EXPENSE, 20_000, LocalDate.of(2026, 11, 1), "Compra", cardId = 1, invoiceYm = 202611),  // fatura fechada
        ),
    )

    private fun items(s: Snapshot = world()) = Ledger(s, today).let { l -> Inbox.items(l, null, MonthEnds.of(l), hide = false) }

    @Test fun gruposNaOrdemDaPrioridade() {
        val all = items()
        // Primeiro os alertas, depois a atenção; nada de "longe" (a academia do dia 30).
        assertEquals(all.sortedBy { it.level.ordinal }.map { it.level }, all.map { it.level })
        val alerts = Inbox.group(all, InboxLevel.ALERT)
        assertTrue(alerts.any { it.kind == InboxKind.BILL && it.key.startsWith("bill:1:") })
        assertTrue(alerts.any { it.kind == InboxKind.DEBT && it.target == InboxTarget.Debt(1) })
        assertTrue(alerts.any { it.kind == InboxKind.BUDGET && it.key == "budget:1:202611:over" })
        assertTrue("o mês negativo pelo que já é certo", alerts.any { it.kind == InboxKind.MONTH })
        val soon = Inbox.group(all, InboxLevel.ATTENTION)
        assertTrue(soon.any { it.key.startsWith("bill:2:") })
        assertTrue(soon.any { it.key == "budget:2:202611:near" })
        assertTrue(soon.any { it.kind == InboxKind.INVOICE && it.target == InboxTarget.Card(1) })
        assertTrue(all.none { it.key.startsWith("bill:3:") })
        // Nenhum texto escapou com o valor escondido.
        val hidden = Ledger(world(), today).let { l -> Inbox.items(l, null, MonthEnds.of(l), hide = true) }
        assertTrue(hidden.filter { it.kind == InboxKind.BILL }.all { it.text.contains(io.github.h3yk0.cryo.domain.Money.HIDDEN) })
    }

    @Test fun sinoContaSoOImportanteNaoVisto() {
        val all = items()
        val important = all.count { it.level != InboxLevel.INFO }
        assertEquals(important, Inbox.unseenImportant(all) { false })
        val seen = all.take(2).map { it.key }.toSet()
        assertEquals(important - 2, Inbox.unseenImportant(all) { it in seen })
        assertEquals(0, Inbox.unseenImportant(all) { true })
    }

    @Test fun destaqueEhOMaisImportanteAindaNaoVisto() {
        val all = items()
        val first = Inbox.highlight(all) { false }!!
        assertEquals(InboxLevel.ALERT, first.level)
        // O fim do mês negativo não vira destaque: o cartão do mês, logo abaixo dele no Início, já mostra o número.
        assertEquals(all.first { it.kind != InboxKind.MONTH }.key, first.key)
        assertTrue(Inbox.highlight(all.filter { it.kind == InboxKind.MONTH }) { false } == null)
        // Dado por visto ("×"), vem o seguinte; tudo visto, nada aparece no Início.
        val second = Inbox.highlight(all) { it == first.key }!!
        assertTrue(second.key != first.key)
        assertNull(Inbox.highlight(all) { true })
        // Percepção nunca vira destaque (fica na central e acende só o ponto do sino).
        assertNull(Inbox.highlight(all.filter { it.level == InboxLevel.INFO }) { false })
    }

    @Test fun percepcoesDoMotorSemOsCenariosDoMes() {
        val s = Personas.generate(Personas.spec(Personas.Kind.STABLE), today, 3)
        val l = Ledger(s, today)
        val report = FinanceEngine.run(s, today)
        val all = Inbox.items(l, report, MonthEnds.of(l), hide = false)
        val info = Inbox.group(all, InboxLevel.INFO)
        assertTrue(info.isNotEmpty())
        assertTrue("as frases da faixa do mês (cenários) saíram", info.none { it.key.startsWith("ins:month:") })
        assertTrue(Inbox.hasNewInsight(all) { false })
        assertFalse(Inbox.hasNewInsight(all) { true })
        assertTrue(info.any { it.kind == InboxKind.RECURRING && it.target == InboxTarget.Recurring })
        // A porta genérica de "O que se repete" só entra quando nenhuma percepção já leva à tela.
        val doors = info.filter { it.target == InboxTarget.Recurring }
        if (doors.any { it.key.startsWith("ins:") }) assertTrue(doors.none { it.key.startsWith("rec:") })
    }

    /** A central não diz a mesma coisa duas vezes: a percepção "Há 1 conta fixa vencida" sai, o aviso da conta fica. */
    @Test fun percepcaoQueRepeteUmAvisoFicaDeFora() {
        val s = world()
        val l = Ledger(s, today)
        val report = FinanceEngine.run(s, today)
        val all = Inbox.items(l, report, MonthEnds.of(l), hide = false)
        assertTrue(all.any { it.kind == InboxKind.BILL && it.level == InboxLevel.ALERT })
        assertTrue(report.insights.any { it.topic == "alert:bills" } || report.insights.none { it.topic.startsWith("alert:") })
        assertTrue(all.none { it.key.startsWith("ins:alert:") })
        assertEquals(all.size, all.map { it.text }.distinct().size)
    }

    /** RF-SEG-02: só o código HMAC da chave fica guardado; o modo de teste tem o seu arquivo; o velho é esquecido. */
    @Test fun vistoGuardaSoOCodigo() {
        val store = InboxStore(ctx, sandbox = false) { "h(" + it.reversed() + ")" }
        store.clear()
        store.markSeen(listOf("bill:1:202611:late"), today = 20_000)
        assertTrue(store.seen()("bill:1:202611:late"))
        assertFalse(store.seen()("bill:2:202611:soon"))
        val raw = ctx.getSharedPreferences("cryo_inbox", Context.MODE_PRIVATE).all.values.joinToString()
        assertFalse("a chave não fica em texto", raw.contains("bill:1:202611:late"))
        assertFalse(InboxStore(ctx, sandbox = true) { "h(" + it.reversed() + ")" }.seen()("bill:1:202611:late"))
        // Visto há mais de 90 dias: esquecido na próxima gravação.
        store.markSeen(listOf("outro"), today = 20_200)
        assertFalse(store.seen()("bill:1:202611:late"))
        assertTrue(store.seen()("outro"))
        store.clear()
    }
}
