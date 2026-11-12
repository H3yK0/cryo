// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.annotation.StringRes
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.GuideCount
import io.github.h3yk0.cryo.domain.GuidePlace
import io.github.h3yk0.cryo.domain.Lesson
import io.github.h3yk0.cryo.ui.components.guideTarget
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
import io.github.h3yk0.cryo.ui.components.Motion
import io.github.h3yk0.cryo.ui.components.rememberHaptics
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
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
import io.github.h3yk0.cryo.ui.screens.DevScreen
import io.github.h3yk0.cryo.ui.screens.ImportReportScreen
import io.github.h3yk0.cryo.ui.screens.ImportScreen
import io.github.h3yk0.cryo.ui.screens.TestModeStrip
import io.github.h3yk0.cryo.ui.screens.GoalEditorScreen
import io.github.h3yk0.cryo.ui.screens.GoalsArchiveScreen
import io.github.h3yk0.cryo.ui.screens.WalletArchiveScreen
import io.github.h3yk0.cryo.ui.screens.HomeScreen
import io.github.h3yk0.cryo.ui.screens.InsightsScreen
import io.github.h3yk0.cryo.ui.screens.InvestmentDetailScreen
import io.github.h3yk0.cryo.ui.screens.InvestmentEditorScreen
import io.github.h3yk0.cryo.ui.screens.PlanScreen
import io.github.h3yk0.cryo.ui.screens.SettingsScreen
import io.github.h3yk0.cryo.ui.screens.TourScreen
import io.github.h3yk0.cryo.ui.screens.DebtPlanScreen
import io.github.h3yk0.cryo.ui.screens.RecurringScreen
import io.github.h3yk0.cryo.ui.screens.HealthScreen
import io.github.h3yk0.cryo.ui.screens.TransactionsScreen
import io.github.h3yk0.cryo.ui.screens.WalletScreen
import io.github.h3yk0.cryo.ui.tx.TxEditorScreen
import io.github.h3yk0.cryo.ui.screens.AlertSettingsScreen
import io.github.h3yk0.cryo.ui.screens.BudgetMonthScreen
import io.github.h3yk0.cryo.ui.screens.ReminderTimeSheet
import io.github.h3yk0.cryo.ui.screens.saveReminder
import io.github.h3yk0.cryo.ui.components.Moment
import io.github.h3yk0.cryo.ui.components.MomentHost
import io.github.h3yk0.cryo.ui.components.pressScale
import io.github.h3yk0.cryo.ui.components.raised
import io.github.h3yk0.cryo.ui.components.rememberLedger
import io.github.h3yk0.cryo.ui.tx.rememberVoiceLauncher
import io.github.h3yk0.cryo.notify.AlertState
import io.github.h3yk0.cryo.domain.ym
import io.github.h3yk0.cryo.domain.key
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.h3yk0.cryo.domain.Res

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
    const val IMPORT = "import?account={account}&card={card}"
    const val IMPORT_REPORT = "import/report"
    const val DEV = "settings/dev"
    const val CATEGORIES = "categories"
    const val CATEGORY_EDIT = "category/edit/{id}?kind={kind}"
    const val TOUR = "tour"
    const val HEALTH = "insights/health"
    const val RECURRING = "insights/recurring"
    const val DEBT_PLAN = "debts/plan"
    const val BUDGET_MONTH = "plan/budgets/month"
    const val ALERT_SETTINGS = "settings/alerts"
    const val PROFILE = "profile"
    const val LEARN = "learn"

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
    fun import(account: Long = 0, card: Long = 0) = "import?account=$account&card=$card"
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

    /**
     * Volta para o Início fechando o que estiver aberto por cima, **sem guardar para restaurar**. (O [tab] para o Início
     * guardaria as telas fechadas e as reabriria em seguida: é assim que a aba "lembra onde a pessoa estava".)
     */
    fun home() { nav.popBackStack(nav.graph.findStartDestination().id, inclusive = false) }

    /** Escolhe sozinho: rotas de aba abrem como aba; o resto empilha uma tela nova. */
    fun smart(route: String) = if (route.startsWith("plan") || route.startsWith("txs")) open(route) else go(route)
}

val LocalNav = staticCompositionLocalOf<Navigator> { error("Navigator ausente") }

private data class TabItem(val route: String, val pattern: String, @StringRes val label: Int, val icon: ImageVector)

/**
 * RF-INI-14: 4 abas. A Carteira (contas, cartões, investimentos, dívidas) fica no Início, e a Carteira completa abre por lá.
 */
private val tabs = listOf(
    TabItem(Routes.HOME, Routes.HOME, R.string.tab_home, Icons.Rounded.Home),
    TabItem(Routes.txs(), Routes.TXS, R.string.tab_transactions, Icons.AutoMirrored.Rounded.ReceiptLong),
    TabItem(Routes.plan(), Routes.PLAN, R.string.tab_plan, Icons.AutoMirrored.Rounded.EventNote),
    TabItem(Routes.INSIGHTS, Routes.INSIGHTS, R.string.tab_insights, Icons.Rounded.Insights),
)

/** DS-MOV-03: entre duas abas a troca é "fade through"; o resto desliza no eixo X (DS-MOV-02). */
private fun isTab(route: String?) = tabs.any { it.pattern == route }

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
    // Uma vez só: o app sai da tela enquanto está bloqueado e, ao desbloquear, volta para onde a pessoa estava, e não
    // de novo para a tela do aviso.
    var opened by rememberSaveable { mutableStateOf(false) }
    var voiceOnStart by remember { mutableStateOf(false) }
    // RF-ALC-01: a voz (tocar) e a frase escrita (segurar) a partir do botão do meio, em qualquer aba.
    val ledger = rememberLedger()
    val voice = rememberVoiceLauncher(ledger)
    LaunchedEffect(voiceOnStart, ledger != null) {
        if (voiceOnStart && ledger != null) { voiceOnStart = false; voice.listen() }
    }
    // RF-AVI-11: pergunta o horário do lembrete do dia, uma vez (não no modo de teste: os avisos são dos dados de verdade).
    val context = androidx.compose.ui.platform.LocalContext.current
    val alertState = remember { AlertState(context) }
    var askReminder by rememberSaveable { mutableStateOf(!alertState.reminderAsked && !c.sandbox && openOnStart == null) }
    val notifPermission = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }
    // RF-MOM-04: do dia 1 ao 3, na primeira abertura, como o mês passado fechou (uma vez por mês).
    LaunchedEffect(ledger != null) {
        val l = ledger ?: return@LaunchedEffect
        if (c.sandbox || l.today.dayOfMonth > 3) return@LaunchedEffect
        val prev = l.today.ym().minusMonths(1)
        val key = "month:${prev.key()}"
        val sum = l.monthSummary(prev)
        if (alertState.momentShown(key) || (sum.income == 0L && sum.expense == 0L)) return@LaunchedEffect
        alertState.markMoment(key)
        c.moments.show(Moment.MonthClosed(prev, sum.result))
    }
    LaunchedEffect(openOnStart) {
        if (opened) return@LaunchedEffect
        opened = true
        when (openOnStart) {
            "bills" -> navigator.open(Routes.plan(1))
            "debts" -> navigator.go(Routes.DEBTS)
            "backup" -> navigator.go(Routes.AUTO_BACKUP)
            // 1.7 (RF-AVI): para onde cada aviso leva.
            "budgets" -> navigator.go(Routes.BUDGET_MONTH)
            "goals" -> navigator.open(Routes.plan(2))
            "insights" -> navigator.tab(Routes.INSIGHTS)
            "wallet" -> navigator.go(Routes.WALLET)
            "voice" -> voiceOnStart = true
        }
    }

    // RF-GUI: os pontos que o guia sabe destacar e as poucas telas que ele conta (o que não dá para deduzir dos dados).
    val guideSpots = remember { io.github.h3yk0.cryo.ui.components.GuideSpots() }
    LaunchedEffect(currentPattern) {
        when (currentPattern) {
            Routes.TX_EDIT -> GuideCount.EDIT
            Routes.CATEGORIES, Routes.CATEGORY_EDIT -> GuideCount.CATEGORIES
            Routes.BUDGET_MONTH -> GuideCount.MONTH
            Routes.DEBTS -> GuideCount.DEBTS
            Routes.INSIGHTS -> GuideCount.INSIGHTS
            Routes.HEALTH -> GuideCount.HEALTH
            Routes.RECURRING -> GuideCount.RECURRING
            Routes.IMPORT -> GuideCount.IMPORT
            else -> null
        }?.let(c::guideCount)
    }
    val guidePlace = when (currentPattern) {
        Routes.HOME -> GuidePlace.HOME
        Routes.TXS -> GuidePlace.TXS
        Routes.PLAN -> GuidePlace.PLAN
        Routes.INSIGHTS -> GuidePlace.INSIGHTS
        else -> null
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalNav provides navigator, io.github.h3yk0.cryo.ui.components.LocalGuideSpots provides guideSpots) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                // DS-MOV-04: a barra some e volta deslizando, junto com a troca de tela.
                AnimatedVisibility(
                    showBar,
                    enter = expandVertically(tween(Motion.MEDIUM, easing = Motion.EmphasizedDecelerate), expandFrom = Alignment.Top) +
                        fadeIn(tween(Motion.MEDIUM)),
                    exit = shrinkVertically(tween(Motion.MEDIUM, easing = Motion.EmphasizedAccelerate), shrinkTowards = Alignment.Top) +
                        fadeOut(tween(Motion.SHORT)),
                ) {
                    Column {
                        // Modo de teste (modo desenvolvedor): sempre à vista nas abas, para ninguém confundir com os dados de verdade.
                        if (c.sandbox) TestModeStrip()
                        VoiceTabBar(currentPattern, navigator, onListen = voice.listen, onType = voice.type)
                    }
                }
            },
        ) { pad ->
          androidx.compose.foundation.layout.Box {
            NavHost(
                navController = nav,
                startDestination = Routes.HOME,
                // O botão de voz sobe acima da barra (RF-ALC-01): o conteúdo continua por baixo dessa parte.
                modifier = Modifier.padding(bottom = (pad.calculateBottomPadding() - if (showBar) MIC_RISE else 0.dp).coerceAtLeast(0.dp)),
                enterTransition = {
                    if (isTab(initialState.destination.route) && isTab(targetState.destination.route)) Motion.tabEnter() else Motion.forwardEnter()
                },
                exitTransition = {
                    if (isTab(initialState.destination.route) && isTab(targetState.destination.route)) Motion.tabExit() else Motion.forwardExit()
                },
                popEnterTransition = {
                    if (isTab(initialState.destination.route) && isTab(targetState.destination.route)) Motion.tabEnter() else Motion.backEnter()
                },
                popExitTransition = {
                    if (isTab(initialState.destination.route) && isTab(targetState.destination.route)) Motion.tabExit() else Motion.backExit()
                },
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
                composable(Routes.PROFILE) { io.github.h3yk0.cryo.ui.screens.ProfileScreen() }
                composable(Routes.LEARN) { io.github.h3yk0.cryo.ui.screens.LearnScreen() }
                composable(Routes.AUTO_BACKUP) { AutoBackupScreen() }
                composable(
                    Routes.IMPORT,
                    arguments = listOf(
                        navArgument("account") { type = NavType.LongType; defaultValue = 0L },
                        navArgument("card") { type = NavType.LongType; defaultValue = 0L },
                    ),
                ) { e ->
                    ImportScreen(
                        presetAccountId = e.arguments?.getLong("account")?.takeIf { it > 0 },
                        presetCardId = e.arguments?.getLong("card")?.takeIf { it > 0 },
                    )
                }
                composable(Routes.IMPORT_REPORT) { ImportReportScreen() }
                composable(Routes.DEV) { DevScreen() }
                composable(Routes.CATEGORIES) { CategoriesScreen() }
                composable(Routes.TOUR) { TourScreen() }
                composable(Routes.HEALTH) { HealthScreen() }
                composable(Routes.RECURRING) { RecurringScreen() }
                composable(Routes.DEBT_PLAN) { DebtPlanScreen() }
                composable(Routes.BUDGET_MONTH) { BudgetMonthScreen() }
                composable(Routes.ALERT_SETTINGS) { AlertSettingsScreen() }
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
            // RF-GUI-05: a dica sobe logo acima da barra de abas, por cima do conteúdo (como uma snackbar), sem travar a tela.
            io.github.h3yk0.cryo.ui.components.GuideHost(
                guidePlace, ready = ledger != null, blocked = askReminder,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = pad.calculateBottomPadding()),
            )
          }
        }
        MomentHost(c.moments)
        io.github.h3yk0.cryo.ui.components.GuideSpotlight(guideSpots)
        if (askReminder) ReminderTimeSheet(alertState.reminderMinutes, onDismiss = { askReminder = false; alertState.reminderAsked = true }) { m ->
            askReminder = false
            saveReminder(context, m)
            if (m != null && android.os.Build.VERSION.SDK_INT >= 33 && !io.github.h3yk0.cryo.notify.Reminders.canNotify(context)) {
                notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

/** Quanto o botão de voz sobe acima da linha da barra. */
private val MIC_RISE = 22.dp

/**
 * RF-ALC-01: a barra de baixo com o botão de voz no centro (como o "Fácil acesso" da One UI: o que mais se usa fica onde o
 * polegar alcança). Duas abas de cada lado; o botão tem 68 dp, volume (DS-PROF-05) e sobe um pouco acima da barra.
 * Tocar = falar; segurar = escrever.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VoiceTabBar(currentPattern: String?, navigator: Navigator, onListen: () -> Unit, onType: () -> Unit) {
    val d = CryoTheme.depth
    val haptics = rememberHaptics()
    Box(Modifier.fillMaxWidth()) {
        // DS-PROF-06: barra elevada, na cor dos cartões, com sombra para cima; trocar de aba vibra ("tique").
        NavigationBar(
            containerColor = d.card,
            tonalElevation = 0.dp,
            modifier = Modifier.padding(top = MIC_RISE).dropShadow(RectangleShape, Shadow(radius = 16.dp, color = d.shadow, offset = DpOffset(0.dp, (-2).dp))),
        ) {
            tabs.forEachIndexed { i, t ->
                if (i == 2) Spacer(Modifier.weight(1f))
                NavigationBarItem(
                    selected = currentPattern == t.pattern,
                    onClick = { if (currentPattern != t.pattern) haptics.tick(); navigator.tab(t.route) },
                    icon = { Icon(t.icon, null) },
                    label = { io.github.h3yk0.cryo.ui.components.CappedFontScale { Text(stringResource(t.label), maxLines = 1, softWrap = false) } },
                )
            }
        }
        val primary = MaterialTheme.colorScheme.primary
        val interaction = remember { MutableInteractionSource() }
        val label = Res.s(R.string.quick_registrar_falando)
        val typeLabel = Res.s(R.string.bar_type_title)
        Box(
            Modifier.align(Alignment.TopCenter)
                .size(68.dp)
                .guideTarget(Lesson.VOICE)
                .pressScale(interaction, 0.92f)
                .raised(CircleShape, primary, shadowColor = primary.copy(alpha = 0.42f), glow = 14.dp)
                .combinedClickable(
                    interactionSource = interaction, indication = androidx.compose.material3.ripple(),
                    role = Role.Button, onClickLabel = label, onLongClickLabel = typeLabel,
                    onClick = { haptics.tap(); onListen() },
                    onLongClick = { haptics.confirm(); onType() },
                )
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Mic, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(30.dp))
        }
    }
}
