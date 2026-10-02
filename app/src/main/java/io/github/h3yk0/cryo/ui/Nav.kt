// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.screens.AccountDetailScreen
import io.github.h3yk0.cryo.ui.screens.AccountEditorScreen
import io.github.h3yk0.cryo.ui.screens.AutoBackupScreen
import io.github.h3yk0.cryo.ui.screens.BillEditorScreen
import io.github.h3yk0.cryo.ui.screens.CardDetailScreen
import io.github.h3yk0.cryo.ui.screens.CardEditorScreen
import io.github.h3yk0.cryo.ui.screens.CategoriesScreen
import io.github.h3yk0.cryo.ui.screens.CategoryEditorScreen
import io.github.h3yk0.cryo.ui.screens.DebtDetailScreen
import io.github.h3yk0.cryo.ui.screens.DebtEditorScreen
import io.github.h3yk0.cryo.ui.screens.DebtsScreen
import io.github.h3yk0.cryo.ui.screens.GoalEditorScreen
import io.github.h3yk0.cryo.ui.screens.GoalsArchiveScreen
import io.github.h3yk0.cryo.ui.screens.WalletArchiveScreen
import io.github.h3yk0.cryo.ui.screens.HomeScreen
import io.github.h3yk0.cryo.ui.screens.InsightsScreen
import io.github.h3yk0.cryo.ui.screens.InvestmentDetailScreen
import io.github.h3yk0.cryo.ui.screens.InvestmentEditorScreen
import io.github.h3yk0.cryo.ui.screens.PlanScreen
import io.github.h3yk0.cryo.ui.screens.SettingsScreen
import io.github.h3yk0.cryo.ui.screens.TransactionsScreen
import io.github.h3yk0.cryo.ui.screens.WalletScreen
import io.github.h3yk0.cryo.ui.tx.TxEditorScreen

object Routes {
    const val HOME = "home"
    const val TXS = "txs?category={category}"
    const val PLAN = "plan?tab={tab}"
    const val WALLET = "wallet"
    const val INSIGHTS = "insights"
    const val TX_NEW = "tx/new?type={type}&account={account}&card={card}"
    const val TX_EDIT = "tx/edit/{id}"
    const val ACCOUNT = "account/{id}"
    const val ACCOUNT_EDIT = "account/edit/{id}"
    const val CARD = "card/{id}"
    const val CARD_EDIT = "card/edit/{id}"
    const val INVESTMENT = "investment/{id}"
    const val INVESTMENT_EDIT = "investment/edit/{id}"
    const val GOAL_EDIT = "goal/edit/{id}"
    const val GOALS_ARCHIVE = "goals/archive"
    const val BILL_EDIT = "bill/edit/{id}?kind={kind}"
    const val DEBTS = "debts"
    const val DEBT = "debt/{id}"
    const val DEBT_EDIT = "debt/edit/{id}"
    const val WALLET_ARCHIVE = "wallet/archive"
    const val SETTINGS = "settings"
    const val AUTO_BACKUP = "settings/backup"
    const val CATEGORIES = "categories"
    const val CATEGORY_EDIT = "category/edit/{id}?kind={kind}"

    fun txs(category: Long = 0) = "txs?category=$category"
    fun plan(tab: Int = 0) = "plan?tab=$tab"
    fun txNew(type: TxType = TxType.EXPENSE, account: Long = 0, card: Long = 0) = "tx/new?type=${type.name}&account=$account&card=$card"
    fun txEdit(id: Long) = "tx/edit/$id"
    fun account(id: Long) = "account/$id"
    fun accountEdit(id: Long = 0) = "account/edit/$id"
    fun card(id: Long) = "card/$id"
    fun cardEdit(id: Long = 0) = "card/edit/$id"
    fun investment(id: Long) = "investment/$id"
    fun investmentEdit(id: Long = 0) = "investment/edit/$id"
    fun goalEdit(id: Long = 0) = "goal/edit/$id"
    fun billEdit(id: Long = 0, kind: CategoryKind = CategoryKind.EXPENSE) = "bill/edit/$id?kind=${kind.name}"
    fun debt(id: Long) = "debt/$id"
    fun debtEdit(id: Long = 0) = "debt/edit/$id"
    fun categoryEdit(id: Long = 0, kind: CategoryKind = CategoryKind.EXPENSE) = "category/edit/$id?kind=${kind.name}"
}

class Navigator(private val nav: NavHostController) {
    fun go(route: String) = nav.navigate(route) { launchSingleTop = true }
    fun back() { nav.popBackStack() }
    /** Troca de aba pela barra inferior (lembra onde a pessoa estava). */
    fun tab(route: String) = nav.navigate(route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    /** Abre uma aba já num ponto específico (ex.: Planejar > Metas), sem restaurar o estado antigo. */
    fun open(route: String) = nav.navigate(route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
    }

    /** Escolhe sozinho: rotas de aba abrem como aba; o resto empilha uma tela nova. */
    fun smart(route: String) = if (route.startsWith("plan") || route.startsWith("txs")) open(route) else go(route)
}

val LocalNav = staticCompositionLocalOf<Navigator> { error("Navigator ausente") }

private data class TabItem(val route: String, val pattern: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem(Routes.HOME, Routes.HOME, "Início", Icons.Rounded.Home),
    TabItem(Routes.txs(), Routes.TXS, "Extrato", Icons.AutoMirrored.Rounded.ReceiptLong),
    TabItem(Routes.plan(), Routes.PLAN, "Planejar", Icons.AutoMirrored.Rounded.EventNote),
    TabItem(Routes.WALLET, Routes.WALLET, "Carteira", Icons.Rounded.AccountBalanceWallet),
    TabItem(Routes.INSIGHTS, Routes.INSIGHTS, "Análises", Icons.Rounded.Insights),
)

@Composable
fun CryoRoot(openOnStart: String?) {
    val nav = rememberNavController()
    val navigator = remember(nav) { Navigator(nav) }
    val c = LocalContainer.current
    val snackbar = remember { SnackbarHostState() }
    val entry by nav.currentBackStackEntryAsState()
    val currentPattern = entry?.destination?.route
    // Sem rota ainda = tela inicial (evita a barra "piscar" ao abrir o app)
    val showBar = currentPattern == null || tabs.any { it.pattern == currentPattern }

    LaunchedEffect(Unit) {
        c.messages.collect { m ->
            val r = snackbar.showSnackbar(
                m.text, actionLabel = m.actionLabel, withDismissAction = m.actionLabel == null,
                duration = if (m.actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            if (r == SnackbarResult.ActionPerformed) m.action?.let { c.launch { it() } }
        }
    }
    LaunchedEffect(openOnStart) {
        when (openOnStart) {
            "bills" -> navigator.open(Routes.plan(1))
            "debts" -> navigator.go(Routes.DEBTS)
            "backup" -> navigator.go(Routes.AUTO_BACKUP)
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalNav provides navigator) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (showBar) {
                    NavigationBar {
                        tabs.forEach { t ->
                            NavigationBarItem(
                                selected = currentPattern == t.pattern,
                                onClick = { navigator.tab(t.route) },
                                icon = { Icon(t.icon, null) },
                                label = { Text(t.label, maxLines = 1) },
                            )
                        }
                    }
                }
            },
        ) { pad ->
            NavHost(
                navController = nav,
                startDestination = Routes.HOME,
                modifier = Modifier.padding(bottom = pad.calculateBottomPadding()),
                enterTransition = { fadeIn() + slideInHorizontally { it / 12 } },
                exitTransition = { fadeOut() },
                popEnterTransition = { fadeIn() },
                popExitTransition = { fadeOut() + slideOutHorizontally { it / 12 } },
            ) {
                composable(Routes.HOME) { HomeScreen() }
                composable(
                    Routes.TXS,
                    arguments = listOf(navArgument("category") { type = NavType.LongType; defaultValue = 0L }),
                ) { e -> TransactionsScreen(categoryFilter = e.arguments?.getLong("category")?.takeIf { it > 0 }) }
                composable(
                    Routes.PLAN,
                    arguments = listOf(navArgument("tab") { type = NavType.IntType; defaultValue = 0 }),
                ) { e -> PlanScreen(initialTab = e.arguments?.getInt("tab") ?: 0) }
                composable(Routes.WALLET) { WalletScreen() }
                composable(Routes.INSIGHTS) { InsightsScreen() }
                composable(
                    Routes.TX_NEW,
                    arguments = listOf(
                        navArgument("type") { type = NavType.StringType; defaultValue = TxType.EXPENSE.name },
                        navArgument("account") { type = NavType.LongType; defaultValue = 0L },
                        navArgument("card") { type = NavType.LongType; defaultValue = 0L },
                    ),
                ) { e ->
                    val a = e.arguments
                    TxEditorScreen(
                        txId = null,
                        initialType = runCatching { TxType.valueOf(a?.getString("type") ?: "") }.getOrDefault(TxType.EXPENSE),
                        presetAccountId = a?.getLong("account")?.takeIf { it > 0 },
                        presetCardId = a?.getLong("card")?.takeIf { it > 0 },
                        onDone = navigator::back,
                    )
                }
                composable(Routes.TX_EDIT, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                    TxEditorScreen(e.arguments!!.getLong("id"), TxType.EXPENSE, null, null, onDone = navigator::back)
                }
                composable(Routes.ACCOUNT, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                    AccountDetailScreen(e.arguments!!.getLong("id"))
                }
                composable(Routes.ACCOUNT_EDIT, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                    AccountEditorScreen(e.arguments!!.getLong("id").takeIf { it > 0 })
                }
                composable(Routes.CARD, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                    CardDetailScreen(e.arguments!!.getLong("id"))
                }
                composable(Routes.CARD_EDIT, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                    CardEditorScreen(e.arguments!!.getLong("id").takeIf { it > 0 })
                }
                composable(Routes.INVESTMENT, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                    InvestmentDetailScreen(e.arguments!!.getLong("id"))
                }
                composable(Routes.INVESTMENT_EDIT, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                    InvestmentEditorScreen(e.arguments!!.getLong("id").takeIf { it > 0 })
                }
                composable(Routes.GOAL_EDIT, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                    GoalEditorScreen(e.arguments!!.getLong("id").takeIf { it > 0 })
                }
                composable(
                    Routes.BILL_EDIT,
                    arguments = listOf(
                        navArgument("id") { type = NavType.LongType },
                        navArgument("kind") { type = NavType.StringType; defaultValue = CategoryKind.EXPENSE.name },
                    ),
                ) { e ->
                    BillEditorScreen(
                        e.arguments!!.getLong("id").takeIf { it > 0 },
                        runCatching { CategoryKind.valueOf(e.arguments?.getString("kind") ?: "") }.getOrDefault(CategoryKind.EXPENSE),
                    )
                }
                composable(Routes.GOALS_ARCHIVE) { GoalsArchiveScreen() }
                composable(Routes.DEBTS) { DebtsScreen() }
                composable(Routes.DEBT, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                    DebtDetailScreen(e.arguments!!.getLong("id"))
                }
                composable(Routes.DEBT_EDIT, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                    DebtEditorScreen(e.arguments!!.getLong("id").takeIf { it > 0 })
                }
                composable(Routes.WALLET_ARCHIVE) { WalletArchiveScreen() }
                composable(Routes.SETTINGS) { SettingsScreen() }
                composable(Routes.AUTO_BACKUP) { AutoBackupScreen() }
                composable(Routes.CATEGORIES) { CategoriesScreen() }
                composable(
                    Routes.CATEGORY_EDIT,
                    arguments = listOf(
                        navArgument("id") { type = NavType.LongType },
                        navArgument("kind") { type = NavType.StringType; defaultValue = CategoryKind.EXPENSE.name },
                    ),
                ) { e ->
                    CategoryEditorScreen(
                        e.arguments!!.getLong("id").takeIf { it > 0 },
                        runCatching { CategoryKind.valueOf(e.arguments?.getString("kind") ?: "") }.getOrDefault(CategoryKind.EXPENSE),
                    )
                }
            }
        }
    }
}
