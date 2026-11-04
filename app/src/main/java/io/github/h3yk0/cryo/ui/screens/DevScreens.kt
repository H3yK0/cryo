// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.screens

import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.Res
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
            app.container.message(Res.s(R.string.dev_nao_consegui_preparar_modo))
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
            c.message(Res.s(R.string.dev_nao_deu_certo_tente))
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
                    n > 0 -> Res.p(R.plurals.dev_samples_saved, n, n)
                    else -> Res.s(R.string.dev_nao_consegui_salvar_arquivos)
                },
            )
        }
    }

    Scaffold(topBar = { CryoTopBar(Res.s(R.string.settings_modo_desenvolvedor), onBack = nav::back) }, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp)) {
            item {
                Text(
                    Res.s(R.string.dev_ferramentas_para_testar_cryo),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item { DevHeader(Res.s(R.string.dev_modo_teste)) }
            item {
                SwitchRow(
                    Res.s(R.string.dev_usar_dados_teste_2), c.sandbox,
                    { on ->
                        if (working) return@SwitchRow
                        if (on) confirmEnter = true else { working = true; switchTestMode(ctx, false) { working = false } }
                    },
                    Res.s(R.string.dev_telas_passam_usar_outro),
                )
                if (working) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp))
                if (c.sandbox) {
                    DevLink(Icons.Rounded.Refresh, Res.s(R.string.dev_recriar_dados_exemplo_2), Res.s(R.string.dev_apaga_que_tem_modo)) {
                        if (!working) confirmRefill = true
                    }
                    DevLink(Icons.Rounded.DeleteSweep, Res.s(R.string.settings_comecar_zero), Res.s(R.string.dev_modo_teste_vazio_com)) {
                        if (!working) confirmFresh = true
                    }
                } else {
                    Text(
                        Res.s(R.string.dev_lembretes_backup_automatico_sempre),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }

            item { DevHeader(Res.s(R.string.dev_importacao_extratos)) }
            item {
                Text(Res.s(R.string.dev_testar_com_arquivo_exemplo), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 4.dp))
                if (c.sandbox) {
                    Text(
                        Res.s(R.string.dev_abre_importacao_ja_com),
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
                        Res.s(R.string.dev_ligue_dados_teste_acima),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (c.sandbox) {
                    DevLink(Icons.Rounded.FolderOpen, Res.s(R.string.dev_salvar_exemplos_numa_pasta), Res.s(R.string.dev_ofx_csv_conta_fatura)) {
                        saveFolder.launch(null)
                    }
                }
                DevLink(
                    Icons.Rounded.Description, Res.s(R.string.import_relatorio_ultima_leitura),
                    report?.let { Res.s(R.string.dev_como_arquivo_foi_lido, it.fileName) } ?: Res.s(R.string.dev_nenhum_arquivo_lido_ainda),
                ) { nav.go(Routes.IMPORT_REPORT) }
                DevLink(
                    Icons.Rounded.UploadFile, Res.s(R.string.import_importar_extrato),
                    if (c.sandbox) Res.s(R.string.dev_modo_teste_aceita_tambem) else Res.s(R.string.dev_para_seus_dados_verdade),
                ) { nav.go(Routes.import()) }
                TextButton(onClick = { openLink(ctx, "$SOURCE_URL/blob/main/docs/IMPORTACAO.md") }) {
                    Text(Res.s(R.string.dev_formatos_json_teste_como))
                }
            }

            item { DevHeader(Res.s(R.string.dev_este_celular)) }
            item {
                CryoCard {
                    InfoLine(Res.s(R.string.dev_versao_cryo), "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    InfoLine(Res.s(R.string.dev_android), Res.s(R.string.dev_api, Build.VERSION.RELEASE, Build.VERSION.SDK_INT))
                    InfoLine(Res.s(R.string.dev_leitura_pdf), if (PdfText.isSupported()) Res.s(R.string.dev_disponivel) else Res.s(R.string.dev_indisponivel_neste_android))
                    InfoLine(Res.s(R.string.dev_banco_dados_uso), if (c.sandbox) CryoDatabase.TEST_NAME else CryoDatabase.NAME)
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                DevLink(Icons.Rounded.PowerSettingsNew, Res.s(R.string.dev_desligar_modo_desenvolvedor), Res.s(R.string.dev_volta_aos_seus_dados), danger = true) {
                    if (working) return@DevLink
                    val wasTest = c.sandbox
                    c.dev.enabled = false
                    if (wasTest) {
                        working = true
                        switchTestMode(ctx, false) { working = false }
                    } else {
                        c.message(Res.s(R.string.dev_modo_desenvolvedor_desligado))
                        nav.back()
                    }
                }
            }
        }
    }

    if (confirmEnter) {
        ConfirmDialog(
            Res.s(R.string.dev_usar_dados_teste),
            Res.s(R.string.dev_cryo_vai_mostrar_dados),
            Res.s(R.string.dev_entrar_modo_teste),
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
            Res.s(R.string.dev_recriar_dados_exemplo), Res.s(R.string.dev_tudo_que_esta_modo),
            Res.s(R.string.dev_recriar),
            onConfirm = {
                confirmRefill = false
                working = true
                testDataTask(ctx, c, Res.s(R.string.dev_dados_exemplo_recriados), { working = false }) { t -> CryoApp.fillWithSamples(t) }
            },
            onDismiss = { confirmRefill = false },
        )
    }
    if (confirmFresh) {
        ConfirmDialog(
            Res.s(R.string.dev_comecar_zero), Res.s(R.string.dev_modo_teste_fica_vazio),
            Res.s(R.string.settings_comecar_zero),
            onConfirm = {
                confirmFresh = false
                working = true
                testDataTask(ctx, c, Res.s(R.string.dev_modo_teste_vazio), { working = false }) { t ->
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
            Text(Res.s(R.string.dev_modo_teste_dados_exemplo), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = { if (!leaving) { leaving = true; switchTestMode(ctx, false) { leaving = false } } }) {
                Text(Res.s(R.string.import_sair), color = MaterialTheme.colorScheme.onTertiaryContainer)
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
            if (t.length <= MAX_REPORT) t else t.take(MAX_REPORT) + Res.s(R.string.dev_relatorio_cortado_arquivo_muito)
        }
    }

    fun share(t: String) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, "Relatório de leitura de extrato · Cryo")
            .putExtra(Intent.EXTRA_TEXT, t)
        try {
            ctx.startActivity(Intent.createChooser(send, Res.s(R.string.dev_compartilhar_relatorio)))
        } catch (_: ActivityNotFoundException) {
            c.message(Res.s(R.string.dev_nenhum_app_para_compartilhar))
        } catch (_: Exception) {
            c.message(Res.s(R.string.dev_nao_consegui_compartilhar_relatorio))
        }
    }

    /** [sensitive]: o relatório completo. No Android 13 ou mais novo, ele não aparece na prévia do que foi copiado. */
    fun copy(t: String, sensitive: Boolean) {
        val copied = runCatching {
            val clip = ClipData.newPlainText(Res.s(R.string.dev_relatorio_cryo), t)
            if (sensitive && Build.VERSION.SDK_INT >= 33) {
                clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
            }
            ctx.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(clip)
        }.isSuccess
        if (!copied) c.message(Res.s(R.string.dev_nao_consegui_copiar_relatorio)) else if (Build.VERSION.SDK_INT < 33) c.message(Res.s(R.string.dev_relatorio_copiado))
    }

    Scaffold(topBar = { CryoTopBar(Res.s(R.string.import_relatorio_leitura), onBack = nav::back) }, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { pad ->
        if (text == null) {
            Column(Modifier.fillMaxSize().padding(pad)) {
                EmptyState(
                    Icons.Rounded.Description, Res.s(R.string.dev_nenhuma_leitura_ainda),
                    Res.s(R.string.dev_importe_extrato_volte_aqui),
                )
            }
            return@Scaffold
        }
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
            Segmented(listOf(true to Res.s(R.string.dev_anonimo), false to Res.s(R.string.dev_completo)), anonymous, { anonymous = it })
            Spacer(Modifier.height(10.dp))
            if (anonymous) {
                Banner(
                    Icons.Rounded.Science,
                    Res.s(R.string.dev_letras_viram_x_numeros),
                    MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer,
                )
            } else {
                Banner(
                    Icons.Rounded.Description,
                    Res.s(R.string.dev_esta_versao_tem_suas),
                    MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { if (anonymous) copy(text, sensitive = false) else confirmCopy = true }) {
                    Icon(Icons.Rounded.ContentCopy, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(Res.s(R.string.dev_copiar))
                }
                OutlinedButton(onClick = { if (anonymous) share(text) else confirmShare = true }) {
                    Icon(Icons.Rounded.Share, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(Res.s(R.string.dev_compartilhar))
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
            Res.s(R.string.dev_compartilhar_relatorio_completo),
            Res.s(R.string.dev_ele_tem_suas_movimentacoes_2),
            Res.s(R.string.dev_compartilhar_mesmo_assim),
            onConfirm = { confirmShare = false; share(text) },
            onDismiss = { confirmShare = false },
        )
    }
    if (confirmCopy && text != null) {
        ConfirmDialog(
            Res.s(R.string.dev_copiar_relatorio_completo),
            Res.s(R.string.dev_ele_tem_suas_movimentacoes),
            Res.s(R.string.dev_copiar_mesmo_assim),
            onConfirm = { confirmCopy = false; copy(text, sensitive = true) },
            onDismiss = { confirmCopy = false },
        )
    }
}
