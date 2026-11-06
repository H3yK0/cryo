// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.screens

import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.Res
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import io.github.h3yk0.cryo.BuildConfig
import io.github.h3yk0.cryo.data.Backup
import io.github.h3yk0.cryo.data.DefaultData
import io.github.h3yk0.cryo.data.Palette
import io.github.h3yk0.cryo.data.SafFiles
import io.github.h3yk0.cryo.data.ThemeMode
import io.github.h3yk0.cryo.data.db.Category
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.notify.Reminders
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.Routes
import io.github.h3yk0.cryo.ui.components.AccountChips
import io.github.h3yk0.cryo.ui.components.ColorChooser
import io.github.h3yk0.cryo.ui.components.ConfirmDialog
import io.github.h3yk0.cryo.ui.components.CryoCard
import io.github.h3yk0.cryo.ui.components.CryoTopBar
import io.github.h3yk0.cryo.ui.components.FieldLabel
import io.github.h3yk0.cryo.ui.components.IconBadge
import io.github.h3yk0.cryo.ui.components.IconChooser
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.components.Segmented
import io.github.h3yk0.cryo.ui.components.SwitchRow
import io.github.h3yk0.cryo.ui.components.categoryIcon
import io.github.h3yk0.cryo.ui.components.rememberLedger
import io.github.h3yk0.cryo.ui.tx.LoadingBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

fun canUseDeviceLock(ctx: Context): Boolean =
    BiometricManager.from(ctx).canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL,
    ) == BiometricManager.BIOMETRIC_SUCCESS

@Composable
fun SettingsScreen() {
    val c = LocalContainer.current
    val s = LocalSettings.current
    val nav = LocalNav.current
    val ctx = LocalContext.current
    val l = rememberLedger() ?: return LoadingBox()
    var editName by rememberSaveable { mutableStateOf(false) }
    var showLanguage by rememberSaveable { mutableStateOf(false) }
    var showCurrency by rememberSaveable { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<Backup.Restored?>(null) }
    var confirmErase by rememberSaveable { mutableStateOf(false) }
    val autoBackup by c.autoBackup.flow.collectAsState(initial = null)
    // O bloqueio vale para o app inteiro (no modo de teste, vem dos ajustes de verdade).
    val lock by c.lockSettings.flow.collectAsState(initial = null)
    val devMode by c.dev.enabledFlow.collectAsState(initial = c.dev.enabled)
    var versionTaps by remember { mutableIntStateOf(0) }

    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) c.launch {
            val ok = writeText(ctx, uri, Backup.export(c.repo.loadOnce(), testMode = c.sandbox))
            c.message(if (ok) Res.s(R.string.settings_backup_salvo) else Res.s(R.string.settings_nao_foi_possivel_salvar))
        }
    }
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) c.launch {
            val ok = writeText(ctx, uri, Backup.csv(c.repo.loadOnce(), LocalDate.now()))
            c.message(if (ok) Res.s(R.string.settings_planilha_exportada) else Res.s(R.string.settings_nao_foi_possivel_salvar))
        }
    }
    val importJson = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) c.launch {
            try {
                val text = withContext(Dispatchers.IO) { ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }
                    ?: throw IllegalArgumentException(Res.s(R.string.settings_nao_consegui_ler_arquivo))
                val restored = Backup.import(text)
                if (restored.testMode && !c.sandbox) {
                    throw IllegalArgumentException(Res.s(R.string.settings_este_backup_modo_teste))
                }
                pendingImport = restored
            } catch (e: Exception) {
                c.message(e.message ?: Res.s(R.string.settings_arquivo_invalido))
            }
        }
    }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) c.message(Res.s(R.string.settings_sem_permissao_lembretes_nao))
    }

    Scaffold(topBar = { CryoTopBar(Res.s(R.string.settings), onBack = nav::back) }, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp)) {
            item { Header(Res.s(R.string.settings_perfil)) }
            item {
                SettingLink(Icons.Rounded.Person, Res.s(R.string.settings_seu_nome), s.userName.ifBlank { Res.s(R.string.settings_toque_para_dizer_como) }) { editName = true }
            }

            item { Header(Res.s(R.string.settings_idioma_moeda)) }
            item {
                SettingLink(Icons.Rounded.Language, Res.s(R.string.locale_language_title), languageSummary(ctx)) { showLanguage = true }
                SettingLink(Icons.Rounded.Payments, Res.s(R.string.locale_currency_title), currencySummary(ctx)) { showCurrency = true }
            }

            item { Header(Res.s(R.string.settings_aparencia)) }
            item {
                FieldLabel(Res.s(R.string.settings_tema))
                Segmented(
                    listOf(ThemeMode.SYSTEM to Res.s(R.string.settings_automatico), ThemeMode.LIGHT to Res.s(R.string.settings_claro), ThemeMode.DARK to Res.s(R.string.settings_escuro)), s.themeMode,
                    { m -> c.launch { c.settings.setTheme(m) } },
                )
                Spacer(Modifier.height(8.dp))
                if (Build.VERSION.SDK_INT >= 31) {
                    SwitchRow(
                        Res.s(R.string.settings_cores_papel_parede_material), s.dynamicColor,
                        { v -> c.launch { c.settings.setDynamic(v) } }, Res.s(R.string.settings_app_combina_com_cores),
                    )
                } else {
                    Text(
                        Res.s(R.string.settings_cores_papel_parede_precisam),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item { Header(Res.s(R.string.settings_privacidade_seguranca)) }
            item {
                SwitchRow(
                    Res.s(R.string.settings_bloquear_com_digital_ou), lock?.lockEnabled ?: s.lockEnabled,
                    { v ->
                        if (v && !canUseDeviceLock(ctx)) {
                            c.message(Res.s(R.string.settings_cadastre_digital_rosto_ou))
                        } else {
                            c.launch { c.lockSettings.setLock(v) }
                        }
                    },
                    Res.s(R.string.settings_pede_desbloqueio_ao_abrir),
                )
                SwitchRow(Res.s(R.string.hide_values), s.hideValues, { v -> c.launch { c.settings.setHideValues(v) } }, Res.s(R.string.settings_hide_values_hint, Money.HIDDEN))
            }

            item { Header(Res.s(R.string.settings_lembretes)) }
            if (c.sandbox) {
                item { TestModeNote(Res.s(R.string.settings_modo_teste_lembretes_continuam)) }
            } else item {
                SwitchRow(
                    Res.s(R.string.settings_avisar_contas_vencer), s.remindersEnabled,
                    { v ->
                        c.launch { c.settings.setReminders(v) }
                        if (v && Build.VERSION.SDK_INT >= 33 && !Reminders.canNotify(ctx)) {
                            notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    Res.s(R.string.settings_notificacao_dia_que_voce),
                )
            }

            item { Header(Res.s(R.string.settings_registro_rapido)) }
            item {
                FieldLabel(Res.s(R.string.settings_conta_principal_usada_quando))
                AccountChips(l.activeAccounts, s.defaultAccountId, { id -> c.launch { c.settings.setDefaultAccount(id) } })
                Spacer(Modifier.height(8.dp))
                SettingLink(Icons.Rounded.Category, Res.s(R.string.settings_categorias), Res.s(R.string.settings_nomes_icones_cores_palavras)) { nav.go(Routes.CATEGORIES) }
            }

            item { Header(Res.s(R.string.settings_seus_dados)) }
            item {
                if (c.sandbox) {
                    TestModeNote(
                        Res.s(R.string.settings_modo_teste_dados_abaixo),
                    )
                } else {
                    Text(
                        Res.s(R.string.settings_tudo_fica_guardado_so),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    SettingLink(
                        Icons.Rounded.Backup, Res.s(R.string.settings_backup_automatico),
                        autoBackup?.let { autoBackupSummary(it) } ?: Res.s(R.string.settings_numa_pasta_celular_ou),
                        warning = autoBackup?.hasProblem == true,
                    ) { nav.go(Routes.AUTO_BACKUP) }
                }
                SettingLink(Icons.Rounded.UploadFile, Res.s(R.string.settings_importar_extrato_banco), Res.s(R.string.settings_ofx_csv_planilha_ou)) {
                    nav.go(Routes.import())
                }
                SettingLink(Icons.Rounded.FileDownload, Res.s(R.string.settings_salvar_copia_agora), Res.s(R.string.settings_arquivo_json_com_tudo)) {
                    exportJson.launch(if (c.sandbox) "cryo-teste-backup-${LocalDate.now()}.json" else "cryo-backup-${LocalDate.now()}.json")
                }
                SettingLink(Icons.Rounded.FileUpload, Res.s(R.string.settings_restaurar_backup_2), Res.s(R.string.settings_substitui_dados_atuais_pelos)) {
                    importJson.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                }
                SettingLink(Icons.Rounded.TableChart, Res.s(R.string.settings_exportar_planilha_csv), Res.s(R.string.settings_abre_excel_ou_planilhas)) {
                    exportCsv.launch("cryo-movimentacoes-${LocalDate.now()}.csv")
                }
                SettingLink(Icons.Rounded.DeleteForever, Res.s(R.string.settings_apagar_todos_dados), Res.s(R.string.settings_comecar_zero), danger = true) { confirmErase = true }
            }

            if (devMode) {
                item { Header(Res.s(R.string.settings_desenvolvedor)) }
                item {
                    SettingLink(
                        Icons.Rounded.Science, Res.s(R.string.settings_modo_desenvolvedor),
                        if (c.sandbox) Res.s(R.string.settings_usando_dados_teste_arquivos) else Res.s(R.string.settings_dados_teste_arquivos_exemplo),
                    ) { nav.go(Routes.DEV) }
                }
            }

            item { Header(Res.s(R.string.settings_sobre)) }
            item {
                // RF-AJU-11, RF-TOUR-03: o passeio animado, para ver de novo quando quiser.
                SettingLink(Icons.Rounded.Explore, Res.s(R.string.tour_how_to_use), Res.s(R.string.tour_how_to_use_sub)) { nav.go(Routes.TOUR) }
                SettingLink(Icons.Rounded.Code, Res.s(R.string.settings_codigo_fonte), Res.s(R.string.settings_github_com_h3yk0_cryo)) {
                    openLink(ctx, SOURCE_URL)
                }
                SettingLink(Icons.Rounded.Gavel, Res.s(R.string.settings_licenca), Res.s(R.string.settings_gnu_agpl_v3_ou)) {
                    openLink(ctx, "$SOURCE_URL/blob/main/LICENSE")
                }
                // 7 toques na versão ligam o modo desenvolvedor (como no Android).
                CryoCard(
                    Modifier.padding(top = 12.dp),
                    onClick = {
                        if (devMode) {
                            c.message(Res.s(R.string.settings_modo_desenvolvedor_ja_esta))
                        } else {
                            versionTaps++
                            if (versionTaps >= DEV_TAPS) {
                                versionTaps = 0
                                c.dev.enabled = true
                                c.message(Res.s(R.string.settings_modo_desenvolvedor_ligado))
                            }
                        }
                    },
                ) {
                    Text(Res.s(R.string.settings_cryo, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.titleSmall)
                    if (!devMode && versionTaps >= 3) {
                        Text(
                            (DEV_TAPS - versionTaps).let { n -> if (n == 1) Res.s(R.string.settings_mais_toque_para_modo) else Res.s(R.string.settings_mais_toques_para_modo, n) },
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        Res.s(R.string.settings_organizacao_financeira_com_registro),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        Res.s(R.string.settings_2026_hanry_franco_este),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }

    if (showLanguage) LanguageSheet(onDismiss = { showLanguage = false })
    if (showCurrency) CurrencySheet(onDismiss = { showCurrency = false })
    if (editName) {
        var name by remember { mutableStateOf(s.userName) }
        AlertDialog(
            onDismissRequest = { editName = false },
            title = { Text(Res.s(R.string.settings_como_quer_ser_chamado)) },
            text = {
                OutlinedTextField(
                    name, { name = it }, singleLine = true, label = { Text(Res.s(R.string.wallet_nome)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
            },
            confirmButton = { TextButton(onClick = { editName = false; c.launch { c.settings.setName(name) } }) { Text(Res.s(R.string.wallet_salvar)) } },
            dismissButton = { TextButton(onClick = { editName = false }) { Text(Res.s(R.string.wallet_cancelar)) } },
        )
    }
    pendingImport?.let { restored ->
        val snap = restored.snapshot
        ConfirmDialog(
            Res.s(R.string.settings_restaurar_backup),
            Res.s(R.string.settings_restore_counts, Res.p(R.plurals.count_transactions, snap.txs.size, snap.txs.size), Res.p(R.plurals.count_accounts, snap.accounts.size, snap.accounts.size)),
            Res.s(R.string.wallet_restaurar),
            onConfirm = {
                pendingImport = null
                c.launch {
                    c.repo.replaceAll(snap, fromOldVersion = restored.version < Backup.VERSION)
                    c.message(Res.s(R.string.settings_backup_restaurado))
                }
            },
            onDismiss = { pendingImport = null },
        )
    }
    if (confirmErase) {
        ConfirmDialog(
            Res.s(R.string.settings_apagar_tudo_2), Res.s(R.string.settings_todas_contas_cartoes_movimentacoes),
            Res.s(R.string.settings_apagar_tudo),
            onConfirm = {
                confirmErase = false
                c.launch {
                    c.repo.eraseAll()
                    c.settings.setDefaultAccount(null)
                    c.settings.setOnboardingDone(false)
                }
            },
            onDismiss = { confirmErase = false },
        )
    }
}

const val SOURCE_URL = "https://github.com/H3yK0/cryo"

/** Toques na versão do app para ligar o modo desenvolvedor. */
private const val DEV_TAPS = 7

@Composable
private fun TestModeNote(text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Science, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.tertiary)
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Abre um link no navegador (o Cryo não tem permissão de internet: quem abre é o navegador). */
fun openLink(ctx: Context, url: String) {
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
    }
}

private suspend fun writeText(ctx: Context, uri: Uri, text: String): Boolean = withContext(Dispatchers.IO) {
    runCatching { SafFiles.write(ctx.contentResolver, uri, text) }.isSuccess
}

@Composable
private fun Header(text: String) {
    Text(
        text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 24.dp, bottom = 4.dp).semantics { heading() },
    )
}

@Composable
private fun SettingLink(
    icon: ImageVector,
    title: String,
    subtitle: String,
    danger: Boolean = false,
    warning: Boolean = false,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title, color = if (danger) MaterialTheme.colorScheme.error else Color.Unspecified) },
        supportingContent = { Text(subtitle, color = if (warning) MaterialTheme.colorScheme.error else Color.Unspecified) },
        leadingContent = { Icon(icon, null, tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

/* ============================== Categorias ============================== */

@Composable
fun CategoriesScreen() {
    val l = rememberLedger() ?: return LoadingBox()
    val nav = LocalNav.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val kind = if (tab == 0) CategoryKind.EXPENSE else CategoryKind.INCOME
    Scaffold(
        topBar = { CryoTopBar(Res.s(R.string.settings_categorias), onBack = nav::back) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { nav.go(Routes.categoryEdit(kind = kind)) }, icon = { Icon(Icons.Rounded.Add, null) }, text = { Text(Res.s(R.string.wallet_nova)) })
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(tab == 0, { tab = 0 }, text = { Text(Res.s(R.string.settings_despesas)) })
                Tab(tab == 1, { tab = 1 }, text = { Text(Res.s(R.string.settings_receitas)) })
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                items(l.categories(kind), key = { it.id }) { cat ->
                    ListItem(
                        headlineContent = { Text(cat.name) },
                        supportingContent = {
                            val n = cat.keywords.split(',').count { it.isNotBlank() }
                            Text(if (n > 0) Res.p(R.plurals.category_keywords, n, n) else Res.s(R.string.settings_sem_palavras_chave))
                        },
                        leadingContent = { IconBadge(categoryIcon(cat.icon), Color(cat.color)) },
                        modifier = Modifier.clickable { nav.go(Routes.categoryEdit(cat.id, kind)) },
                    )
                    HorizontalDivider(Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }
    }
}

@Composable
fun CategoryEditorScreen(id: Long?, kindArg: CategoryKind) {
    val l = rememberLedger() ?: return LoadingBox()
    val c = LocalContainer.current
    val nav = LocalNav.current
    val existing = id?.let { l.category[it] }
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var icon by rememberSaveable { mutableStateOf(existing?.icon ?: "star") }
    var color by rememberSaveable { mutableIntStateOf(existing?.color ?: Palette.pick(l.s.categories.size)) }
    var keywords by rememberSaveable { mutableStateOf(existing?.keywords?.replace(",", ", ") ?: "") }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val kind = existing?.kind ?: kindArg

    EditorScaffold(
        title = if (existing == null) Res.s(R.string.settings_nova_categoria) else Res.s(R.string.settings_editar_categoria),
        canSave = name.isNotBlank(),
        onSave = {
            val kw = keywords.split(',').map { it.trim() }.filter { it.isNotEmpty() }.joinToString(",")
            val cat = (existing ?: Category(name = name, kind = kind, icon = icon, color = color, sortOrder = 100))
                .copy(name = name.trim(), icon = icon, color = color, keywords = kw)
            c.launch { c.repo.saveCategory(cat) }
            nav.back()
        },
        onDelete = existing?.let { { confirmDelete = true } },
    ) {
        OutlinedTextField(
            name, { name = it }, label = { Text(Res.s(R.string.wallet_nome)) }, singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), modifier = Modifier.fillMaxWidth(),
        )
        FieldLabel(Res.s(R.string.plan_icone))
        IconChooser(DefaultData.categoryIcons, icon, Color(color)) { icon = it }
        FieldLabel(Res.s(R.string.wallet_cor))
        ColorChooser(color) { color = it }
        FieldLabel(Res.s(R.string.settings_palavras_chave_registro_rapido))
        OutlinedTextField(
            keywords, { keywords = it }, modifier = Modifier.fillMaxWidth(), minLines = 3,
            placeholder = { Text(Res.s(R.string.settings_ex_padaria_lanche_cafe)) },
        )
        Text(
            Res.s(R.string.settings_quando_voce_digitar_ou),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (confirmDelete && existing != null) {
        ConfirmDialog(
            Res.s(R.string.settings_apagar_categoria), Res.s(R.string.settings_se_ela_ja_foi), Res.s(R.string.wallet_apagar),
            onConfirm = {
                confirmDelete = false
                c.launch { if (c.repo.deleteCategory(existing)) c.message(Res.s(R.string.settings_categoria_apagada)) else c.message(Res.s(R.string.settings_categoria_arquivada)) }
                nav.back()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
