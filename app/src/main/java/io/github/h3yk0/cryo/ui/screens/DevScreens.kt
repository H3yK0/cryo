// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PersistableBundle
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.h3yk0.cryo.AppContainer
import io.github.h3yk0.cryo.BuildConfig
import io.github.h3yk0.cryo.CryoApp
import io.github.h3yk0.cryo.data.SafFiles
import io.github.h3yk0.cryo.data.db.CryoDatabase
import io.github.h3yk0.cryo.importer.PdfText
import io.github.h3yk0.cryo.importer.SampleFiles
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.Routes
import io.github.h3yk0.cryo.ui.components.Banner
import io.github.h3yk0.cryo.ui.components.ConfirmDialog
import io.github.h3yk0.cryo.ui.components.CryoCard
import io.github.h3yk0.cryo.ui.components.CryoTopBar
import io.github.h3yk0.cryo.ui.components.EmptyState
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.Segmented
import io.github.h3yk0.cryo.ui.components.SwitchRow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** A tela (Activity) por trás de um Context do Compose. */
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Troca entre os dados de verdade e os de teste e recria a tela (tudo passa a usar o outro banco).
 * Ao entrar pela primeira vez, o modo de teste já vem com dados de exemplo. Roda fora da tela: sair dela no meio
 * não deixa nada pela metade. [done] é chamado no fim (deu certo ou não).
 */
fun switchTestMode(ctx: Context, on: Boolean, done: () -> Unit = {}) {
    val app = ctx.applicationContext as? CryoApp ?: return done()
    val activity = ctx.findActivity()
    app.real.launch {
        try {
            if (on) app.prepareTestData()
            withContext(Dispatchers.Main) {
                app.switchData(on)
                activity?.takeIf { !it.isFinishing && !it.isDestroyed }?.recreate()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            app.container.message("Não consegui preparar o modo de teste. Tente de novo.")
        } finally {
            withContext(NonCancellable + Dispatchers.Main) { done() }
        }
    }
}

/** Mexe nos dados de teste (recriar, começar do zero), um pedido de cada vez. */
private fun testDataTask(ctx: Context, c: AppContainer, doneMessage: String, done: () -> Unit, block: suspend (AppContainer) -> Unit) {
    val app = ctx.applicationContext as? CryoApp ?: return done()
    app.real.launch {
        try {
            app.withTestData(block)
            c.message(doneMessage)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            c.message("Não deu certo. Tente de novo.")
        } finally {
            withContext(NonCancellable + Dispatchers.Main) { done() }
        }
    }
}

/* ================================ modo desenvolvedor ================================ */

@Composable
fun DevScreen() {
    val c = LocalContainer.current
    val nav = LocalNav.current
    val ctx = LocalContext.current
    val report by c.lastImport.collectAsState()
    var confirmEnter by remember { mutableStateOf(false) }
    var confirmRefill by remember { mutableStateOf(false) }
    var confirmFresh by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    val today = remember { LocalDate.now() }

    val saveFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) c.launch {
            val n = withContext(Dispatchers.IO) { saveSamples(ctx, uri, today) }
            c.message(
                when {
                    n == 1 -> "1 arquivo de exemplo salvo na pasta"
                    n > 1 -> "$n arquivos de exemplo salvos na pasta"
                    else -> "Não consegui salvar os arquivos nessa pasta"
                },
            )
        }
    }

    Scaffold(topBar = { CryoTopBar("Modo desenvolvedor", onBack = nav::back) }, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp)) {
            item {
                Text(
                    "Ferramentas para testar o Cryo e ajudar a melhorar a leitura de extratos, sem mexer nos seus dados de verdade.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item { DevHeader("Modo de teste") }
            item {
                SwitchRow(
                    "Usar dados de teste", c.sandbox,
                    { on ->
                        if (working) return@SwitchRow
                        if (on) confirmEnter = true else { working = true; switchTestMode(ctx, false) { working = false } }
                    },
                    "As telas passam a usar outro banco de dados, com dados de exemplo. Os seus dados ficam guardados, " +
                        "do jeito que estão, até você voltar.",
                )
                if (working) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp))
                if (c.sandbox) {
                    DevLink(Icons.Rounded.Refresh, "Recriar os dados de exemplo", "Apaga o que tem no modo de teste e cria 4 meses de exemplo") {
                        if (!working) confirmRefill = true
                    }
                    DevLink(Icons.Rounded.DeleteSweep, "Começar do zero", "Modo de teste vazio, com a apresentação inicial") {
                        if (!working) confirmFresh = true
                    }
                } else {
                    Text(
                        "Lembretes e backup automático sempre cuidam dos seus dados de verdade, mesmo no modo de teste.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }

            item { DevHeader("Importação de extratos") }
            item {
                Text("Testar com um arquivo de exemplo", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 4.dp))
                if (c.sandbox) {
                    Text(
                        "Abre a importação já com o arquivo (inventado). Bom para ver cada formato e a conferência.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    )
                    val samples = remember(today) { SampleFiles.all(today) }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 4.dp)) {
                        samples.forEach { s ->
                            AssistChip(
                                onClick = { c.pendingSample.value = s; nav.go(Routes.import()) },
                                label = { Text(s.label) },
                                leadingIcon = { Icon(Icons.Rounded.UploadFile, null, Modifier.size(18.dp)) },
                            )
                        }
                    }
                } else {
                    Text(
                        "Ligue os dados de teste acima: assim os exemplos nunca entram nas suas contas de verdade.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (c.sandbox) {
                    DevLink(Icons.Rounded.FolderOpen, "Salvar os exemplos numa pasta", "OFX, CSV de conta e de fatura, XLSX, JSON e PDF") {
                        saveFolder.launch(null)
                    }
                }
                DevLink(
                    Icons.Rounded.Description, "Relatório da última leitura",
                    report?.let { "${it.fileName} · como o arquivo foi lido, linha por linha" } ?: "Nenhum arquivo lido ainda",
                ) { nav.go(Routes.IMPORT_REPORT) }
                DevLink(
                    Icons.Rounded.UploadFile, "Importar extrato",
                    if (c.sandbox) "No modo de teste, aceita também o JSON de teste" else "Para os seus dados de verdade, como em Ajustes",
                ) { nav.go(Routes.import()) }
                TextButton(onClick = { openLink(ctx, "$SOURCE_URL/blob/main/docs/IMPORTACAO.md") }) {
                    Text("Formatos, JSON de teste e como mandar um extrato anônimo")
                }
            }

            item { DevHeader("Este celular") }
            item {
                CryoCard {
                    InfoLine("Versão do Cryo", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    InfoLine("Android", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                    InfoLine("Leitura de PDF", if (PdfText.isSupported()) "disponível" else "indisponível neste Android")
                    InfoLine("Banco de dados em uso", if (c.sandbox) CryoDatabase.TEST_NAME else CryoDatabase.NAME)
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                DevLink(Icons.Rounded.PowerSettingsNew, "Desligar o modo desenvolvedor", "Volta aos seus dados e esconde estas opções", danger = true) {
                    if (working) return@DevLink
                    val wasTest = c.sandbox
                    c.dev.enabled = false
                    if (wasTest) {
                        working = true
                        switchTestMode(ctx, false) { working = false }
                    } else {
                        c.message("Modo desenvolvedor desligado")
                        nav.back()
                    }
                }
            }
        }
    }

    if (confirmEnter) {
        ConfirmDialog(
            "Usar dados de teste?",
            "O Cryo vai mostrar dados de exemplo num banco separado. Os seus dados ficam guardados, sem mudança nenhuma, " +
                "e voltam quando você sair do modo de teste (pela faixa na parte de baixo da tela ou aqui).",
            "Entrar no modo de teste",
            onConfirm = {
                confirmEnter = false
                working = true
                switchTestMode(ctx, true) { working = false }
            },
            onDismiss = { confirmEnter = false },
        )
    }
    if (confirmRefill) {
        ConfirmDialog(
            "Recriar os dados de exemplo?", "Tudo o que está no modo de teste é apagado e trocado pelos exemplos. Os seus dados de verdade não mudam.",
            "Recriar",
            onConfirm = {
                confirmRefill = false
                working = true
                testDataTask(ctx, c, "Dados de exemplo recriados", { working = false }) { t -> CryoApp.fillWithSamples(t) }
            },
            onDismiss = { confirmRefill = false },
        )
    }
    if (confirmFresh) {
        ConfirmDialog(
            "Começar do zero?", "O modo de teste fica vazio e a apresentação inicial aparece de novo. Os seus dados de verdade não mudam.",
            "Começar do zero",
            onConfirm = {
                confirmFresh = false
                working = true
                testDataTask(ctx, c, "Modo de teste vazio", { working = false }) { t ->
                    t.repo.eraseAll()
                    t.settings.setDefaultAccount(null)
                    t.settings.setOnboardingDone(false)
                }
            },
            onDismiss = { confirmFresh = false },
        )
    }
}

/** Grava todos os arquivos de exemplo na pasta escolhida. Devolve quantos deram certo. */
private fun saveSamples(ctx: Context, tree: Uri, today: LocalDate): Int {
    val resolver = ctx.contentResolver
    val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
    return SampleFiles.all(today).count { s ->
        runCatching {
            val doc = DocumentsContract.createDocument(resolver, parent, s.mime, s.name) ?: error("sem arquivo")
            SafFiles.writeBytes(resolver, doc, s.bytes)
        }.isSuccess
    }
}

@Composable
private fun DevHeader(text: String) {
    Text(
        text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 24.dp, bottom = 4.dp, start = 4.dp).semantics { heading() },
    )
}

@Composable
private fun DevLink(icon: ImageVector, title: String, subtitle: String, danger: Boolean = false, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title, color = if (danger) MaterialTheme.colorScheme.error else Color.Unspecified) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, null, tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/* ============================ faixa do modo de teste ============================ */

/**
 * Faixa acima da barra de abas: lembra que os dados na tela são de teste e deixa sair com um toque.
 * [insetTop]: no alto da tela (na apresentação inicial do modo de teste), abaixo da barra de status.
 */
@Composable
fun TestModeStrip(insetTop: Boolean = false) {
    val ctx = LocalContext.current
    var leaving by remember { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer) {
        Row(
            Modifier.fillMaxWidth().then(if (insetTop) Modifier.statusBarsPadding() else Modifier).padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Science, null, Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text("Modo de teste: dados de exemplo", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = { if (!leaving) { leaving = true; switchTestMode(ctx, false) { leaving = false } } }) {
                Text("Sair", color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }
    }
}

/* ============================== relatório da leitura ============================== */

/** Tamanho máximo do relatório na tela, para copiar e para compartilhar (o Android tem um teto). */
private const val MAX_REPORT = 100_000

@Composable
fun ImportReportScreen() {
    val c = LocalContainer.current
    val nav = LocalNav.current
    val ctx = LocalContext.current
    val report by c.lastImport.collectAsState()
    var anonymous by remember { mutableStateOf(true) }
    var confirmShare by remember { mutableStateOf(false) }
    var confirmCopy by remember { mutableStateOf(false) }
    // Limite de tamanho: o texto passa pelo Android para copiar e compartilhar, e lá há um teto.
    val text = remember(report, anonymous) {
        report?.text(anonymous, BuildConfig.VERSION_NAME)?.let { t ->
            if (t.length <= MAX_REPORT) t else t.take(MAX_REPORT) + "\n... (relatório cortado: o arquivo é muito grande)"
        }
    }

    fun share(t: String) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, "Relatório de leitura de extrato · Cryo")
            .putExtra(Intent.EXTRA_TEXT, t)
        try {
            ctx.startActivity(Intent.createChooser(send, "Compartilhar relatório"))
        } catch (_: ActivityNotFoundException) {
            c.message("Nenhum app para compartilhar")
        } catch (_: Exception) {
            c.message("Não consegui compartilhar o relatório")
        }
    }

    /** [sensitive]: o relatório completo. No Android 13 ou mais novo, ele não aparece na prévia do que foi copiado. */
    fun copy(t: String, sensitive: Boolean) {
        val copied = runCatching {
            val clip = ClipData.newPlainText("Relatório do Cryo", t)
            if (sensitive && Build.VERSION.SDK_INT >= 33) {
                clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
            }
            ctx.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(clip)
        }.isSuccess
        if (!copied) c.message("Não consegui copiar o relatório") else if (Build.VERSION.SDK_INT < 33) c.message("Relatório copiado")
    }

    Scaffold(topBar = { CryoTopBar("Relatório da leitura", onBack = nav::back) }, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { pad ->
        if (text == null) {
            Column(Modifier.fillMaxSize().padding(pad)) {
                EmptyState(
                    Icons.Rounded.Description, "Nenhuma leitura ainda",
                    "Importe um extrato e volte aqui para ver como o Cryo leu o arquivo, linha por linha.",
                )
            }
            return@Scaffold
        }
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
            Segmented(listOf(true to "Anônimo", false to "Completo"), anonymous, { anonymous = it })
            Spacer(Modifier.height(10.dp))
            if (anonymous) {
                Banner(
                    Icons.Rounded.Science,
                    "Letras viram x e números viram 9, mas o formato de cada linha continua igual. É esta versão que ajuda " +
                        "a corrigir a leitura: mande numa issue no GitHub do Cryo.",
                    MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer,
                )
            } else {
                Banner(
                    Icons.Rounded.Description,
                    "Esta versão tem as suas movimentações de verdade. Não publique em lugar nenhum.",
                    MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { if (anonymous) copy(text, sensitive = false) else confirmCopy = true }) {
                    Icon(Icons.Rounded.ContentCopy, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Copiar")
                }
                OutlinedButton(onClick = { if (anonymous) share(text) else confirmShare = true }) {
                    Icon(Icons.Rounded.Share, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Compartilhar")
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                SelectionContainer {
                    Text(
                        text, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace,
                        modifier = Modifier.background(Color.Transparent).padding(12.dp),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (confirmShare && text != null) {
        ConfirmDialog(
            "Compartilhar o relatório completo?",
            "Ele tem as suas movimentações de verdade (descrições e valores). Para pedir ajuda, use a versão anônima.",
            "Compartilhar mesmo assim",
            onConfirm = { confirmShare = false; share(text) },
            onDismiss = { confirmShare = false },
        )
    }
    if (confirmCopy && text != null) {
        ConfirmDialog(
            "Copiar o relatório completo?",
            "Ele tem as suas movimentações de verdade (descrições e valores), e o app em que você colar vai ver tudo. " +
                "Para pedir ajuda, use a versão anônima.",
            "Copiar mesmo assim",
            onConfirm = { confirmCopy = false; copy(text, sensitive = true) },
            onDismiss = { confirmCopy = false },
        )
    }
}
