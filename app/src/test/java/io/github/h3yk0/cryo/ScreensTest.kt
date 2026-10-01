// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import io.github.h3yk0.cryo.data.AppSettings
import io.github.h3yk0.cryo.data.Palette
import io.github.h3yk0.cryo.data.ThemeMode
import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.AccountType
import io.github.h3yk0.cryo.data.db.Bill
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.data.db.Goal
import io.github.h3yk0.cryo.data.db.Investment
import io.github.h3yk0.cryo.data.db.InvestmentKind
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.ParsedEntry
import io.github.h3yk0.cryo.domain.PhraseParser
import io.github.h3yk0.cryo.domain.key
import io.github.h3yk0.cryo.domain.ym
import io.github.h3yk0.cryo.ui.CryoRoot
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.Navigator
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.screens.CardDetailScreen
import io.github.h3yk0.cryo.ui.screens.InsightsScreen
import io.github.h3yk0.cryo.ui.screens.OnboardingScreen
import io.github.h3yk0.cryo.ui.screens.PlanScreen
import io.github.h3yk0.cryo.ui.screens.SettingsScreen
import io.github.h3yk0.cryo.ui.screens.TransactionsScreen
import io.github.h3yk0.cryo.ui.screens.WalletScreen
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import io.github.h3yk0.cryo.ui.tx.TxDraft
import io.github.h3yk0.cryo.ui.tx.TxEditorScreen
import io.github.h3yk0.cryo.ui.tx.TxForm
import io.github.h3yk0.cryo.ui.tx.parseContext
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import kotlin.random.Random

class TestApp : Application()

/** Gera imagens das telas com dados de exemplo, para revisar o visual sem um celular. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = TestApp::class, qualifiers = "w393dp-h852dp-xxhdpi")
class ScreensTest {
    private lateinit var c: AppContainer
    private val today = LocalDate.now()

    @Before
    fun setUp() {
        c = AppContainer(ApplicationProvider.getApplicationContext())
        runBlocking { seed(c, today) }
        val deadline = System.currentTimeMillis() + 10_000
        while ((c.repo.snapshot.value?.txs?.size ?: 0) < 50 && System.currentTimeMillis() < deadline) Thread.sleep(50)
    }

    private val settings = AppSettings(onboardingDone = true, defaultAccountId = 1)

    @Composable
    private fun Wrap(dark: Boolean = false, dynamic: Boolean = false, s: AppSettings = settings, content: @Composable () -> Unit) {
        CryoTheme(mode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, dynamicColor = dynamic) {
            val nav = rememberNavController()
            val navigator = remember { Navigator(nav) }
            CompositionLocalProvider(LocalContainer provides c, LocalSettings provides s, LocalNav provides navigator) {
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) { content() }
            }
        }
    }

    private fun shot(name: String, dark: Boolean = false, dynamic: Boolean = false, content: @Composable () -> Unit) =
        captureRoboImage("screenshots/$name.png") { Wrap(dark, dynamic) { content() } }

    @Test fun home() = shot("01_inicio") { CryoRoot(null) }

    @Test fun homeDark() = shot("02_inicio_escuro", dark = true) { CryoRoot(null) }

    @Test fun homeDynamic() = shot("03_inicio_material_you", dynamic = true) { CryoRoot(null) }

    @Config(qualifiers = "w393dp-h1600dp-xxhdpi")
    @Test fun homeLong() = shot("04_inicio_completo") { CryoRoot(null) }

    @Test fun transactions() = shot("05_movimentos") { TransactionsScreen(null) }

    @Test fun budgets() = shot("06_orcamentos") { PlanScreen(0) }

    @Test fun bills() = shot("07_contas_fixas") { PlanScreen(1) }

    @Test fun goals() = shot("08_metas") { PlanScreen(2) }

    @Config(qualifiers = "w393dp-h1500dp-xxhdpi")
    @Test fun wallet() = shot("09_carteira") { WalletScreen() }

    @Config(qualifiers = "w393dp-h3000dp-xxhdpi")
    @Test fun insights() = shot("10_analises") { InsightsScreen() }

    @Config(qualifiers = "w393dp-h1400dp-xxhdpi")
    @Test fun editor() = shot("11_novo_lancamento") { TxEditorScreen(null, TxType.EXPENSE, null, null) {} }

    @Config(qualifiers = "w393dp-h1300dp-xxhdpi")
    @Test fun quickSheet() = shot("12_registro_rapido") {
        val l = Ledger(c.repo.snapshot.value!!, today)
        val p: ParsedEntry = PhraseParser(l.parseContext(settings)).parse("tv 1200 em 10x no cartão")
        val d = remember { TxDraft.fromParsed(p) }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) { TxForm(d, l, compact = true) }
    }

    @Test fun card() = shot("13_cartao") { CardDetailScreen(1) }

    @Config(qualifiers = "w393dp-h1900dp-xxhdpi")
    @Test fun settingsScreen() = shot("14_ajustes") { SettingsScreen() }

    @Test fun onboarding() = shot("15_boas_vindas") { OnboardingScreen() }

    @Config(qualifiers = "w393dp-h2400dp-xxhdpi")
    @Test fun insightsDark() = shot("16_analises_escuro", dark = true) { InsightsScreen() }
}

/** Dados de exemplo realistas: 4 meses de vida financeira. */
suspend fun seed(c: AppContainer, today: LocalDate) {
    val r = c.repo
    r.ensureSeeded()
    val cats = r.loadOnce().categories
    fun cat(n: String) = cats.first { it.name == n }.id
    val rnd = Random(42)

    val principal = r.saveAccount(Account(name = "Conta principal", type = AccountType.CHECKING, initialBalance = 150000, color = Palette.pick(3)))
    val carteira = r.saveAccount(Account(name = "Carteira", type = AccountType.CASH, initialBalance = 15000, color = Palette.pick(9), sortOrder = 1))
    r.saveAccount(Account(name = "Poupança", type = AccountType.SAVINGS, initialBalance = 120000, color = Palette.pick(5), sortOrder = 2))
    r.saveCard(CreditCard(name = "Roxo", limitAmount = 350000, closingDay = 3, dueDay = 10, color = Palette.pick(2), payAccountId = principal))
    val card = r.loadOnce().cards.first()

    val tesouro = r.saveInvestment(Investment(name = "Tesouro Selic 2029", kind = InvestmentKind.TREASURY, color = Palette.pick(4)))
    r.saveTx(Tx(type = TxType.INVEST_IN, amount = 200000, date = today.minusMonths(4), description = "Saldo inicial", investmentId = tesouro))
    r.saveTx(Tx(type = TxType.INVEST_IN, amount = 30000, date = today.minusDays(20), description = "Aporte", accountId = principal, investmentId = tesouro))
    val cdb = r.saveInvestment(Investment(name = "CDB 110% do CDI", kind = InvestmentKind.FIXED_INCOME, color = Palette.pick(14)))
    r.saveTx(Tx(type = TxType.INVEST_IN, amount = 100000, date = today.minusMonths(3), description = "Saldo inicial", investmentId = cdb))
    val invs = r.loadOnce().investments
    r.updateInvestmentValue(invs.first { it.id == tesouro }, 230000, 238530, today.minusDays(3))
    r.updateInvestmentValue(invs.first { it.id == cdb }, 100000, 101240, today.minusDays(3))

    val viagem = r.saveGoal(Goal(name = "Viagem para a praia", target = 400000, deadline = today.plusMonths(8), emoji = "🏖️", color = Palette.pick(7)))
    r.saveTx(Tx(type = TxType.GOAL_IN, amount = 125000, date = today.minusMonths(2), description = "Guardado", accountId = principal, goalId = viagem))
    val reserva = r.saveGoal(Goal(name = "Reserva de emergência", target = 1000000, emoji = "🛡️", color = Palette.pick(8)))
    r.saveTx(Tx(type = TxType.GOAL_IN, amount = 320000, date = today.minusMonths(3), description = "Saldo inicial", goalId = reserva))

    val start = today.ym().minusMonths(3).key()
    val ids = listOf(
        Bill(name = "Salário", amount = 320000, dueDay = 5, kind = CategoryKind.INCOME, categoryId = cat("Salário"), accountId = principal, remind = false, startYm = start),
        Bill(name = "Aluguel", amount = 90000, dueDay = 5, categoryId = cat("Moradia"), accountId = principal, startYm = start),
        Bill(name = "Conta de luz", amount = 18000, dueDay = 10, categoryId = cat("Contas da casa"), accountId = principal, startYm = start),
        Bill(name = "Internet", amount = 9990, dueDay = 15, categoryId = cat("Contas da casa"), accountId = principal, startYm = start),
        Bill(name = "Streaming", amount = 3990, dueDay = 20, categoryId = cat("Assinaturas"), cardId = card.id, startYm = start),
        Bill(name = "Academia", amount = 8990, dueDay = 28, categoryId = cat("Saúde"), accountId = principal, startYm = start),
        Bill(name = "Celular", amount = 4990, dueDay = 3, categoryId = cat("Contas da casa"), accountId = principal, startYm = start),
    ).map { it to r.saveBill(it) }
    val bills = ids.map { (b, id) -> b.copy(id = id) }

    data class T(val desc: String, val cat: String, val min: Int, val max: Int, val cash: Boolean = false, val onCard: Boolean = false)
    val templates = listOf(
        T("Almoço", "Alimentação", 1800, 3500), T("Padaria", "Alimentação", 600, 1800, cash = true),
        T("Mercado", "Mercado", 6000, 22000), T("Corrida de app", "Transporte", 1200, 3200, onCard = true),
        T("Gasolina", "Transporte", 8000, 15000), T("Farmácia", "Saúde", 1500, 6000),
        T("Delivery", "Alimentação", 3500, 7000, onCard = true), T("Cinema", "Lazer", 3000, 6000, onCard = true),
        T("Açaí", "Alimentação", 1200, 2500, cash = true), T("Roupa", "Compras", 8000, 18000, onCard = true),
    )

    for (back in 3 downTo 0) {
        val ym = today.ym().minusMonths(back.toLong())
        val lastDay = if (back == 0) today.dayOfMonth else ym.lengthOfMonth()
        for (b in bills) {
            if (b.dueDay > lastDay) continue
            if (back == 0 && b.name == "Academia") continue // fica vencida, para mostrar o alerta
            val amount = if (b.name == "Conta de luz") 15000L + rnd.nextInt(6000) else b.amount
            r.markBillPaid(b, ym, amount, ym.atDay(b.dueDay), b.accountId, if (b.cardId != null) card else null)
        }
        for (d in 1..lastDay) {
            repeat(if (rnd.nextInt(100) < 55) 1 else 0) {
                val t = templates[rnd.nextInt(templates.size)]
                val base = Tx(
                    type = TxType.EXPENSE, amount = (t.min + rnd.nextInt(t.max - t.min)).toLong(), date = ym.atDay(d),
                    description = t.desc, categoryId = cat(t.cat), accountId = if (t.cash) carteira else principal,
                )
                if (t.onCard) r.saveCardPurchase(base, card, 1) else r.saveTx(base)
            }
        }
        if (back % 2 == 1) r.saveTx(Tx(type = TxType.INCOME, amount = 60000, date = ym.atDay(18), description = "Freela de design", categoryId = cat("Extras e freelas"), accountId = principal))
        r.saveTx(Tx(type = TxType.EXPENSE, amount = 32000, date = ym.atDay(minOf(lastDay, 7)), description = "Dízimo", categoryId = cat("Dízimo e doações"), accountId = principal))
    }
    r.saveCardPurchase(
        Tx(type = TxType.EXPENSE, amount = 180000, date = today.minusMonths(2).withDayOfMonth(12), description = "Celular novo", categoryId = cat("Compras")),
        card, 10,
    )
    r.saveTx(Tx(type = TxType.TRANSFER, amount = 20000, date = today.minusDays(4), description = "Transferência", accountId = principal, toAccountId = carteira))

    r.saveBudget(cat("Alimentação"), 60000)
    r.saveBudget(cat("Mercado"), 80000)
    r.saveBudget(cat("Transporte"), 30000)
    r.saveBudget(cat("Lazer"), 20000)

    // paga as faturas que já venceram
    val l = Ledger(r.loadOnce(), today)
    for (m in l.invoiceMonths(card)) {
        val inv = l.invoice(card, m)
        if (inv.due.isBefore(today) && inv.remaining > 0) r.payInvoice(card, m, inv.remaining, principal, inv.due)
    }
}
