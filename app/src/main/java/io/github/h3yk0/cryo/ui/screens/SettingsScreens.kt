// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.TableChart
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import io.github.h3yk0.cryo.data.ThemeMode
import io.github.h3yk0.cryo.data.db.Category
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.notify.Reminders
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
    var pendingImport by remember { mutableStateOf<Backup.Restored?>(null) }
    var confirmErase by rememberSaveable { mutableStateOf(false) }

    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) c.launch {
            val ok = writeText(ctx, uri, Backup.export(c.repo.loadOnce()))
            c.message(if (ok) "Backup salvo" else "Não foi possível salvar o arquivo")
        }
    }
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) c.launch {
            val ok = writeText(ctx, uri, Backup.csv(c.repo.loadOnce(), LocalDate.now()))
            c.message(if (ok) "Planilha exportada" else "Não foi possível salvar o arquivo")
        }
    }
    val importJson = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) c.launch {
            try {
                val text = withContext(Dispatchers.IO) { ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }
                    ?: throw IllegalArgumentException("Não consegui ler o arquivo.")
                pendingImport = Backup.import(text)
            } catch (e: Exception) {
                c.message(e.message ?: "Arquivo inválido")
            }
        }
    }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) c.message("Sem permissão, os lembretes não aparecem. Você pode liberar nas configurações do Android.")
    }

    Scaffold(topBar = { CryoTopBar("Ajustes", onBack = nav::back) }, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp)) {
            item { Header("Perfil") }
            item {
                SettingLink(Icons.Rounded.Person, "Seu nome", s.userName.ifBlank { "Toque para dizer como quer ser chamado" }) { editName = true }
            }

            item { Header("Aparência") }
            item {
                FieldLabel("Tema")
                Segmented(
                    listOf(ThemeMode.SYSTEM to "Automático", ThemeMode.LIGHT to "Claro", ThemeMode.DARK to "Escuro"), s.themeMode,
                    { m -> c.launch { c.settings.setTheme(m) } },
                )
                Spacer(Modifier.height(8.dp))
                if (Build.VERSION.SDK_INT >= 31) {
                    SwitchRow(
                        "Cores do papel de parede (Material You)", s.dynamicColor,
                        { v -> c.launch { c.settings.setDynamic(v) } }, "O app combina com as cores do seu celular",
                    )
                } else {
                    Text(
                        "As cores do papel de parede precisam do Android 12 ou mais novo. Usando a paleta Cryo.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item { Header("Privacidade e segurança") }
            item {
                SwitchRow(
                    "Bloquear com digital ou senha", s.lockEnabled,
                    { v ->
                        if (v && !canUseDeviceLock(ctx)) {
                            c.message("Cadastre uma digital, rosto ou senha de tela no Android primeiro")
                        } else {
                            c.launch { c.settings.setLock(v) }
                        }
                    },
                    "Pede o desbloqueio ao abrir o app",
                )
                SwitchRow("Esconder valores", s.hideValues, { v -> c.launch { c.settings.setHideValues(v) } }, "Mostra R$ •••• no lugar dos números")
            }

            item { Header("Lembretes") }
            item {
                SwitchRow(
                    "Avisar contas a vencer", s.remindersEnabled,
                    { v ->
                        c.launch { c.settings.setReminders(v) }
                        if (v && Build.VERSION.SDK_INT >= 33 && !Reminders.canNotify(ctx)) {
                            notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    "Notificação no dia que você escolher em cada conta fixa",
                )
            }

            item { Header("Registro rápido") }
            item {
                FieldLabel("Conta principal (usada quando você não diz a conta)")
                AccountChips(l.activeAccounts, s.defaultAccountId, { id -> c.launch { c.settings.setDefaultAccount(id) } })
                Spacer(Modifier.height(8.dp))
                SettingLink(Icons.Rounded.Category, "Categorias", "Nomes, ícones, cores e palavras-chave") { nav.go(Routes.CATEGORIES) }
            }

            item { Header("Seus dados") }
            item {
                Text(
                    "Tudo fica guardado só neste celular. Faça backups de vez em quando e guarde o arquivo no Drive ou no computador.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                SettingLink(Icons.Rounded.FileDownload, "Fazer backup", "Salva um arquivo .json com tudo") {
                    exportJson.launch("cryo-backup-${LocalDate.now()}.json")
                }
                SettingLink(Icons.Rounded.FileUpload, "Restaurar backup", "Substitui os dados atuais pelos do arquivo") {
                    importJson.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                }
                SettingLink(Icons.Rounded.TableChart, "Exportar planilha (CSV)", "Abre no Excel ou Planilhas Google") {
                    exportCsv.launch("cryo-movimentacoes-${LocalDate.now()}.csv")
                }
                SettingLink(Icons.Rounded.DeleteForever, "Apagar todos os dados", "Começar do zero", danger = true) { confirmErase = true }
            }

            item { Header("Sobre") }
            item {
                SettingLink(Icons.Rounded.Code, "Código-fonte", "github.com/H3yK0/cryo · sugestões e problemas são bem-vindos") {
                    openLink(ctx, SOURCE_URL)
                }
                SettingLink(Icons.Rounded.Gavel, "Licença", "GNU GPL v3 ou posterior · software livre") {
                    openLink(ctx, "$SOURCE_URL/blob/main/LICENSE")
                }
                CryoCard(Modifier.padding(top = 12.dp)) {
                    Text("Cryo ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Organização financeira com registro por voz, previsão do mês e fluxo do dinheiro. " +
                            "Funciona sem internet, sem anúncios e sem rastreamento.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "© 2026 Hanry Franco. Este programa é software livre: você pode redistribuí-lo e modificá-lo " +
                            "sob os termos da GNU GPL, versão 3 ou posterior. Ele é distribuído sem nenhuma garantia.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }

    if (editName) {
        var name by remember { mutableStateOf(s.userName) }
        AlertDialog(
            onDismissRequest = { editName = false },
            title = { Text("Como quer ser chamado?") },
            text = {
                OutlinedTextField(
                    name, { name = it }, singleLine = true, label = { Text("Nome") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
            },
            confirmButton = { TextButton(onClick = { editName = false; c.launch { c.settings.setName(name) } }) { Text("Salvar") } },
            dismissButton = { TextButton(onClick = { editName = false }) { Text("Cancelar") } },
        )
    }
    pendingImport?.let { restored ->
        val snap = restored.snapshot
        ConfirmDialog(
            "Restaurar backup?",
            "O arquivo tem ${snap.txs.size} movimentações e ${snap.accounts.size} contas. Os dados atuais deste celular serão substituídos.",
            "Restaurar",
            onConfirm = {
                pendingImport = null
                c.launch {
                    c.repo.replaceAll(snap, fromOldVersion = restored.version < Backup.VERSION)
                    c.message("Backup restaurado")
                }
            },
            onDismiss = { pendingImport = null },
        )
    }
    if (confirmErase) {
        ConfirmDialog(
            "Apagar tudo?", "Todas as contas, cartões, movimentações, metas e orçamentos serão apagados. Não dá para desfazer.",
            "Apagar tudo",
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

/** Abre um link no navegador (o Cryo não tem permissão de internet: quem abre é o navegador). */
fun openLink(ctx: Context, url: String) {
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
    }
}

private suspend fun writeText(ctx: Context, uri: Uri, text: String): Boolean = withContext(Dispatchers.IO) {
    try {
        ctx.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray(Charsets.UTF_8)) } != null
    } catch (_: Exception) {
        false
    }
}

@Composable
private fun Header(text: String) {
    Text(
        text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 24.dp, bottom = 4.dp).semantics { heading() },
    )
}

@Composable
private fun SettingLink(icon: ImageVector, title: String, subtitle: String, danger: Boolean = false, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title, color = if (danger) MaterialTheme.colorScheme.error else Color.Unspecified) },
        supportingContent = { Text(subtitle) },
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
        topBar = { CryoTopBar("Categorias", onBack = nav::back) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { nav.go(Routes.categoryEdit(kind = kind)) }, icon = { Icon(Icons.Rounded.Add, null) }, text = { Text("Nova") })
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(tab == 0, { tab = 0 }, text = { Text("Despesas") })
                Tab(tab == 1, { tab = 1 }, text = { Text("Receitas") })
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                items(l.categories(kind), key = { it.id }) { cat ->
                    ListItem(
                        headlineContent = { Text(cat.name) },
                        supportingContent = {
                            val n = cat.keywords.split(',').count { it.isNotBlank() }
                            Text(if (n > 0) "$n palavras-chave" else "Sem palavras-chave")
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
        title = if (existing == null) "Nova categoria" else "Editar categoria",
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
            name, { name = it }, label = { Text("Nome") }, singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), modifier = Modifier.fillMaxWidth(),
        )
        FieldLabel("Ícone")
        IconChooser(DefaultData.categoryIcons, icon, Color(color)) { icon = it }
        FieldLabel("Cor")
        ColorChooser(color) { color = it }
        FieldLabel("Palavras-chave do registro rápido")
        OutlinedTextField(
            keywords, { keywords = it }, modifier = Modifier.fillMaxWidth(), minLines = 3,
            placeholder = { Text("Ex.: padaria, lanche, café") },
        )
        Text(
            "Quando você digitar ou falar uma destas palavras, o Cryo escolhe esta categoria. Separe por vírgula.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (confirmDelete && existing != null) {
        ConfirmDialog(
            "Apagar categoria?", "Se ela já foi usada, será só arquivada para não bagunçar o histórico.", "Apagar",
            onConfirm = {
                confirmDelete = false
                c.launch { if (c.repo.deleteCategory(existing)) c.message("Categoria apagada") else c.message("Categoria arquivada") }
                nav.back()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
