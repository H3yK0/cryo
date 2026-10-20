// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import android.app.Application
import androidx.compose.foundation.layout.Box
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
import io.github.h3yk0.cryo.data.Backup
import io.github.h3yk0.cryo.data.BackupTarget
import io.github.h3yk0.cryo.data.FolderMode
import io.github.h3yk0.cryo.data.FoundBackup
import io.github.h3yk0.cryo.data.Palette
import io.github.h3yk0.cryo.data.SampleData
import io.github.h3yk0.cryo.data.ThemeMode
import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.AccountType
import io.github.h3yk0.cryo.data.db.Bill
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.data.db.Debt
import io.github.h3yk0.cryo.data.db.DebtKind
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
import io.github.h3yk0.cryo.ui.screens.AutoBackupScreen
import io.github.h3yk0.cryo.ui.screens.CardDetailScreen
import io.github.h3yk0.cryo.ui.screens.ExistingBackupContent
import io.github.h3yk0.cryo.ui.screens.DebtDetailScreen
import io.github.h3yk0.cryo.ui.screens.DebtEditorScreen
import io.github.h3yk0.cryo.ui.screens.DebtsScreen
import io.github.h3yk0.cryo.ui.screens.GoalsArchiveScreen
import io.github.h3yk0.cryo.ui.screens.InsightsScreen
import io.github.h3yk0.cryo.ui.screens.WalletArchiveScreen
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
        runBlocking {
            seed(c, today)
            c.autoBackup.clear()
        }
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

    @Config(qualifiers = "w393dp-h1700dp-xxhdpi")
    @Test fun debts() = shot("17_dividas") { DebtsScreen() }

    @Config(qualifiers = "w393dp-h1900dp-xxhdpi")
    @Test fun debtDetail() = shot("18_divida_detalhe") { DebtDetailScreen(1) }

    @Config(qualifiers = "w393dp-h2600dp-xxhdpi")
    @Test fun debtEditor() = shot("19_divida_cadastro") { DebtEditorScreen(null) }

    @Config(qualifiers = "w393dp-h1300dp-xxhdpi")
    @Test fun goalsArchive() = shot("22_metas_arquivadas") { GoalsArchiveScreen() }

    @Test fun walletArchive() = shot("23_carteira_arquivados") { WalletArchiveScreen() }

    @Config(qualifiers = "w393dp-h1700dp-xxhdpi")
    @Test fun debtsDark() = shot("24_dividas_escuro", dark = true) { DebtsScreen() }

    @Config(qualifiers = "w393dp-h1600dp-xxhdpi")
    @Test fun billsWithDebts() = shot("25_contas_fixas_com_parcelas") { PlanScreen(1) }

    @Config(qualifiers = "w393dp-h1400dp-xxhdpi")
    @Test fun debtDetailPaidOff() = shot("26_divida_quitada") { DebtDetailScreen(4) }

    @Config(qualifiers = "w393dp-h1500dp-xxhdpi")
    @Test fun autoBackupEmpty() = shot("29_backup_automatico") { AutoBackupScreen() }

    @Config(qualifiers = "w393dp-h1900dp-xxhdpi")
    @Test fun autoBackupConfigured() {
        val hour = 3_600_000L
        val now = System.currentTimeMillis()
        runBlocking {
            val b = c.autoBackup
            b.setTarget(BackupTarget.FOLDER, "content://com.android.externalstorage.documents/tree/primary%3ADocuments%2FCryo", "Documents/Cryo · no celular")
            b.recordWritten(BackupTarget.FOLDER, now - 26 * hour, "a", 120)
            b.recordUnchanged(BackupTarget.FOLDER, now - 2 * hour)
            b.setTarget(BackupTarget.CLOUD, "content://com.google.android.apps.docs.storage/document/x", "cryo-backup.json · Google Drive")
            b.recordWritten(BackupTarget.CLOUD, now - 50 * hour, "a", 120)
            b.recordFailure(BackupTarget.CLOUD, now - 2 * hour, "Não consegui gravar (sem espaço ou a nuvem não respondeu). Vou tentar de novo mais tarde.")
            b.setEnabled(true)
        }
        shot("30_backup_automatico_configurado") { AutoBackupScreen() }
    }

    @Config(qualifiers = "w393dp-h1700dp-xxhdpi")
    @Test fun autoBackupDark() {
        runBlocking {
            c.autoBackup.setTarget(BackupTarget.FOLDER, "content://com.android.externalstorage.documents/tree/primary%3ADocuments%2FCryo", "Documents/Cryo · no celular")
            c.autoBackup.setFolderMode(FolderMode.REPLACE)
            c.autoBackup.recordWritten(BackupTarget.FOLDER, System.currentTimeMillis() - 3_600_000L, "a", 120)
            c.autoBackup.setEnabled(true)
        }
        shot("31_backup_automatico_escuro", dark = true) { AutoBackupScreen() }
    }

    @Config(qualifiers = "w393dp-h900dp-xxhdpi")
    @Test fun existingBackupRestore() = shot("32_backup_encontrado_restaurar") {
        val snap = c.repo.snapshot.value!!
        val found = FoundBackup(Backup.Restored(snap, Backup.VERSION, System.currentTimeMillis() - 30 * 3_600_000L), currentTxs = 0, currentAccounts = 1)
        Box(Modifier.padding(24.dp)) { ExistingBackupContent(found, BackupTarget.CLOUD, overwrites = true, replaceMode = false, {}, {}, {}) }
    }

    @Config(qualifiers = "w393dp-h900dp-xxhdpi")
    @Test fun existingBackupKeep() = shot("33_backup_encontrado_continuar", dark = true) {
        val snap = c.repo.snapshot.value!!
        val old = snap.copy(txs = snap.txs.take(40), accounts = snap.accounts.take(2))
        val found = FoundBackup(Backup.Restored(old, Backup.VERSION, System.currentTimeMillis() - 400 * 3_600_000L), currentTxs = snap.txs.size, currentAccounts = snap.accounts.size)
        Box(Modifier.padding(24.dp)) { ExistingBackupContent(found, BackupTarget.FOLDER, overwrites = false, replaceMode = false, {}, {}, {}) }
    }
}

/** Dados de exemplo realistas: 4 meses de vida financeira (os mesmos do modo de teste do app). */
suspend fun seed(c: AppContainer, today: LocalDate) = SampleData.fill(c.repo, today)
