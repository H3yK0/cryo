// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.screens

import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.Res
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.CardMath
import io.github.h3yk0.cryo.domain.Dates
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.domain.ymOf
import io.github.h3yk0.cryo.importer.Cell
import io.github.h3yk0.cryo.importer.ColumnMap
import io.github.h3yk0.cryo.importer.Columns
import io.github.h3yk0.cryo.importer.ImportPlanner
import io.github.h3yk0.cryo.importer.ImportRow
import io.github.h3yk0.cryo.importer.ImportTarget
import io.github.h3yk0.cryo.importer.PdfText
import io.github.h3yk0.cryo.importer.RowFlag
import io.github.h3yk0.cryo.importer.Statement
import io.github.h3yk0.cryo.importer.StatementLine
import io.github.h3yk0.cryo.importer.Values
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.Routes
import io.github.h3yk0.cryo.ui.components.Banner
import io.github.h3yk0.cryo.ui.components.CategoryGrid
import io.github.h3yk0.cryo.ui.components.ConfirmDialog
import io.github.h3yk0.cryo.ui.components.CryoCard
import io.github.h3yk0.cryo.ui.components.CryoTopBar
import io.github.h3yk0.cryo.ui.components.FieldLabel
import io.github.h3yk0.cryo.ui.components.IconBadge
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.MoneyText
import io.github.h3yk0.cryo.ui.components.Segmented
import io.github.h3yk0.cryo.ui.components.Source
import io.github.h3yk0.cryo.ui.components.SourceChips
import io.github.h3yk0.cryo.ui.components.categoryIcon
import io.github.h3yk0.cryo.ui.components.incomeColor
import io.github.h3yk0.cryo.ui.components.rememberLedger
import io.github.h3yk0.cryo.ui.components.typeIcon
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import io.github.h3yk0.cryo.ui.tx.LoadingBox
import java.time.LocalDate

/**
 * Importar extrato: escolher o arquivo, conferir a lista e importar. Nada entra no Cryo sem passar pela conferência.
 * [presetAccountId]/[presetCardId]: aberto a partir de uma conta ou de um cartão (já vem escolhido como destino).
 */
@Composable
fun ImportScreen(presetAccountId: Long? = null, presetCardId: Long? = null, model: ImportModel? = null) {
    val c = LocalContainer.current
    val nav = LocalNav.current
    val vm = model ?: viewModel { ImportModel(c) }
    LaunchedEffect(presetAccountId, presetCardId) {
        vm.preset(presetAccountId, presetCardId)
        // Arquivo de exemplo escolhido no modo desenvolvedor: já abre com ele.
        c.pendingSample.value?.let { s ->
            c.pendingSample.value = null
            vm.openBytes(s.name, s.bytes)
        }
    }
    val l = rememberLedger() ?: return LoadingBox()
    val devMode by c.dev.enabledFlow.collectAsState(initial = c.dev.enabled)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) vm.open(uri) }
    var confirmLeave by remember { mutableStateOf(false) }
    val onBack = {
        when (vm.step) {
            ImportModel.Step.Review -> confirmLeave = true
            is ImportModel.Step.Columns, is ImportModel.Step.Password -> vm.reset()
            else -> nav.back()
        }
    }
    BackHandler(enabled = vm.step is ImportModel.Step.Review || vm.step is ImportModel.Step.Columns || vm.step is ImportModel.Step.Password) {
        onBack()
    }

    when (val step = vm.step) {
        ImportModel.Step.Reading -> ReadingScreen(vm.fileName)
        ImportModel.Step.Pick -> ImportStart(
            error = vm.error,
            devMode = devMode,
            testMode = c.sandbox,
            hasReport = c.lastImport.collectAsState().value != null,
            onPick = { picker.launch(arrayOf("*/*")) },
            onBack = { nav.back() },
            onReport = { nav.go(Routes.IMPORT_REPORT) },
        )
        is ImportModel.Step.Password -> PasswordStep(vm.fileName, retry = step.retry, onSubmit = vm::submitPassword, onBack = { vm.reset() })
        is ImportModel.Step.Columns -> ColumnsStep(step, vm.error, onBack = { vm.reset() }, onDone = vm::useColumns)
        ImportModel.Step.Review -> ReviewStep(vm, l, devMode, onBack = { confirmLeave = true }, onReport = { nav.go(Routes.IMPORT_REPORT) })
        is ImportModel.Step.Done -> DoneStep(step, vm, l, devMode, onReport = { nav.go(Routes.IMPORT_REPORT) })
    }

    if (confirmLeave) {
        ConfirmDialog(
            Res.s(R.string.import_sair_conferencia), Res.s(R.string.import_nada_deste_arquivo_foi), Res.s(R.string.import_sair),
            onConfirm = { confirmLeave = false; vm.reset() },
            onDismiss = { confirmLeave = false },
            dismissLabel = Res.s(R.string.import_continuar_conferindo),
        )
    }
}

/* ===================================== começo ===================================== */

@Composable
private fun ImportStart(
    error: String?,
    devMode: Boolean,
    testMode: Boolean,
    hasReport: Boolean,
    onPick: () -> Unit,
    onBack: () -> Unit,
    onReport: () -> Unit,
) {
    Scaffold(
        topBar = { CryoTopBar(Res.s(R.string.import_importar_extrato), onBack = onBack) },
        bottomBar = { BottomButton(Res.s(R.string.import_escolher_arquivo), enabled = true, onClick = onPick) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.UploadFile, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(32.dp)) }
            Spacer(Modifier.height(16.dp))
            Text(Res.s(R.string.import_traga_movimentacoes_seu_banco), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Text(
                Res.s(R.string.import_baixe_extrato_app_ou),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (testMode) {
                Spacer(Modifier.height(16.dp))
                TestModeImportNote()
            }
            if (error != null) {
                Spacer(Modifier.height(16.dp))
                Banner(
                    Icons.Rounded.ErrorOutline, error, MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer,
                )
                if (devMode && hasReport) {
                    TextButton(onClick = onReport) { Text(Res.s(R.string.import_ver_relatorio_leitura)) }
                }
            }
            FieldLabel(Res.s(R.string.import_formatos_que_cryo_le))
            CryoCard {
                FormatLine("OFX", Res.s(R.string.import_melhor_valores_exatos_cryo), best = true)
                FormatLine(Res.s(R.string.import_csv_ou_planilha_xlsx), Res.s(R.string.import_funciona_com_quase_todos))
                FormatLine(
                    "PDF",
                    if (PdfText.isSupported()) Res.s(R.string.import_lido_pelo_texto_arquivo)
                    else Res.s(R.string.import_este_celular_nao_consegue),
                )
                if (devMode) FormatLine(Res.s(R.string.import_json_teste), Res.s(R.string.import_so_modo_desenvolvedor_formato))
            }
            FieldLabel(Res.s(R.string.import_onde_encontrar_arquivo))
            Text(
                Res.s(R.string.import_app_banco_procure_extrato),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lock, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Text(
                    Res.s(R.string.import_arquivo_lido_aqui_celular),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (devMode && hasReport && error == null) {
                TextButton(onClick = onReport, modifier = Modifier.padding(top = 8.dp)) { Text(Res.s(R.string.import_relatorio_ultima_leitura)) }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ReadingScreen(fileName: String) {
    Scaffold(topBar = { CryoTopBar(Res.s(R.string.import_importar_extrato)) }, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(20.dp))
            Text(Res.s(R.string.import_lendo_arquivo), style = MaterialTheme.typography.titleMedium)
            if (fileName.isNotEmpty()) {
                Text(
                    fileName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Aviso de que o extrato vai para os dados de teste (modo desenvolvedor), não para as contas de verdade. */
@Composable
private fun TestModeImportNote() {
    Banner(
        Icons.Rounded.Science,
        Res.s(R.string.import_modo_teste_extrato_vai),
        MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer,
    )
}

@Composable
private fun FormatLine(name: String, text: String, best: Boolean = false) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = MaterialTheme.typography.titleSmall)
            if (best) {
                Spacer(Modifier.width(8.dp))
                Text(
                    "recomendado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BottomButton(label: String, enabled: Boolean, busy: Boolean = false, onClick: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Button(
            onClick = onClick, enabled = enabled,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(16.dp).height(56.dp),
        ) {
            if (busy) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
            }
            Text(label)
        }
    }
}

/* ===================================== senha ====================================== */

@Composable
private fun PasswordStep(fileName: String, retry: Boolean, onSubmit: (String) -> Unit, onBack: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { CryoTopBar(Res.s(R.string.import_pdf_com_senha), onBack = onBack) },
        bottomBar = { BottomButton(Res.s(R.string.import_abrir_pdf), enabled = password.isNotEmpty()) { onSubmit(password) } },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Key, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(32.dp)) }
            Spacer(Modifier.height(16.dp))
            Text(fileName, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Text(
                Res.s(R.string.import_esse_extrato_esta_protegido),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (retry) {
                Spacer(Modifier.height(16.dp))
                Banner(
                    Icons.Rounded.ErrorOutline, Res.s(R.string.import_essa_senha_nao_abriu),
                    MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                password, { password = it }, label = { Text(Res.s(R.string.import_senha_pdf)) }, singleLine = true, isError = retry && password.isEmpty(),
                visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { show = !show }) {
                        Icon(if (show) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, if (show) Res.s(R.string.import_esconder_senha) else Res.s(R.string.import_mostrar_senha))
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (password.isNotEmpty()) onSubmit(password) }),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lock, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Text(
                    Res.s(R.string.import_senha_so_abre_arquivo),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) { Text(Res.s(R.string.import_escolher_outro_arquivo)) }
        }
    }
}

/* ==================================== colunas ===================================== */

@Composable
private fun ColumnsStep(step: ImportModel.Step.Columns, error: String?, onBack: () -> Unit, onDone: (ColumnMap) -> Unit) {
    val table = step.table
    val headerRow = remember(step) { step.guess?.headerRow ?: guessHeaderRow(table) }
    var map by remember(step) {
        mutableStateOf(step.guess?.copy(headerRow = headerRow) ?: ColumnMap(headerRow = headerRow, date = -1, description = -1))
    }
    var twoColumns by remember(step) { mutableStateOf(step.guess?.amount == null && (step.guess?.credit != null || step.guess?.debit != null)) }
    val headers = remember(step, headerRow) { Columns.headers(table, ColumnMap(headerRow, 0, 0)) }
    val data = remember(step, headerRow) { table.drop(headerRow + 1).filter { r -> r.any { !it.isBlank } } }
    val width = headers.size
    val effective = if (twoColumns) map.copy(amount = null) else map.copy(credit = null, debit = null)
    val today = remember { LocalDate.now() }
    // Prévia: as primeiras linhas como vão ficar com as colunas escolhidas.
    val preview = remember(effective, step) {
        if (!effective.isValid) emptyList()
        else runCatching {
            Columns.build(table.take(headerRow + 1 + 40), effective, step.format, today, emptyList()).lines.take(4)
        }.getOrDefault(emptyList())
    }

    Scaffold(
        topBar = { CryoTopBar(Res.s(R.string.import_escolher_colunas), onBack = onBack) },
        bottomBar = { BottomButton(Res.s(R.string.onboarding_continuar), enabled = effective.isValid) { onDone(effective) } },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
            item {
                Text(
                    Res.s(R.string.import_cryo_nao_teve_certeza),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (error != null) {
                item {
                    Banner(
                        Icons.Rounded.ErrorOutline, error, MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer,
                        Modifier.padding(top = 12.dp),
                    )
                }
            }
            item {
                FieldLabel(Res.s(R.string.import_comeco_arquivo))
                TablePreview(headers, data.take(5), effective)
            }
            item {
                FieldLabel(Res.s(R.string.wallet_data))
                ColumnChips(headers, data, map.date) { map = map.copy(date = it) }
                FieldLabel(Res.s(R.string.tx_descricao))
                ColumnChips(headers, data, map.description) { map = map.copy(description = it) }
                FieldLabel(Res.s(R.string.amount_label))
                Segmented(listOf(false to Res.s(R.string.import_coluna), true to Res.s(R.string.import_entradas_saidas)), twoColumns, { twoColumns = it })
                Spacer(Modifier.height(8.dp))
                if (!twoColumns) {
                    ColumnChips(headers, data, map.amount ?: -1) { map = map.copy(amount = it) }
                } else {
                    Text(Res.s(R.string.import_entradas_creditos), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 6.dp))
                    ColumnChips(headers, data, map.credit ?: -1) { map = map.copy(credit = it) }
                    Text(Res.s(R.string.import_saidas_debitos), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 6.dp))
                    ColumnChips(headers, data, map.debit ?: -1) { map = map.copy(debit = it) }
                }
            }
            item {
                FieldLabel(Res.s(R.string.import_como_vai_ficar))
                CryoCard {
                    if (preview.isEmpty()) {
                        Text(
                            if (effective.isValid) Res.s(R.string.import_nenhuma_das_primeiras_linhas)
                            else Res.s(R.string.import_escolha_data_descricao_valor),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        preview.forEach { PreviewLine(it) }
                    }
                }
            }
        }
    }
}

/** Linha de cabeçalho: a primeira só com textos (sem datas nem valores) logo antes de uma linha com data. */
private fun guessHeaderRow(table: List<List<Cell>>): Int {
    for (r in 0 until minOf(table.size - 1, 30)) {
        val row = table[r].filter { !it.isBlank }
        if (row.size < 2) continue
        val allText = row.all { it.number == null && it.date == null && it.text.any(Char::isLetter) && Values.date(it.text, Values.ANY_YEAR) == null }
        val next = table.drop(r + 1).firstOrNull { n -> n.any { !it.isBlank } } ?: continue
        if (allText && next.any { it.date != null || Values.date(it.text, Values.ANY_YEAR) != null }) return r
    }
    return -1
}

@Composable
private fun TablePreview(headers: List<String>, rows: List<List<Cell>>, map: ColumnMap) {
    fun role(i: Int): String? = when (i) {
        map.date -> Res.s(R.string.wallet_data)
        map.description -> Res.s(R.string.tx_descricao)
        map.amount -> Res.s(R.string.amount_label)
        map.credit -> Res.s(R.string.txs_entradas)
        map.debit -> Res.s(R.string.txs_saidas)
        else -> null
    }
    val shape = MaterialTheme.shapes.medium
    Box(
        Modifier.fillMaxWidth().clip(shape).border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .horizontalScroll(rememberScrollState()),
    ) {
        Row {
            headers.forEachIndexed { i, h ->
                val r = role(i)
                Column(
                    Modifier.width(132.dp)
                        .background(if (r != null) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f) else Color.Transparent),
                ) {
                    Text(
                        r ?: " ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                    Text(
                        h, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp).padding(bottom = 6.dp),
                    )
                    HorizontalDivider()
                    rows.forEach { row ->
                        Text(
                            row.getOrNull(i)?.let(::cellText).orEmpty(), style = MaterialTheme.typography.bodySmall, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun cellText(c: Cell): String = when {
    c.date != null -> Dates.full(c.date)
    c.text.isNotBlank() -> c.text
    c.number != null -> c.number.toString()
    else -> ""
}

@Composable
private fun ColumnChips(headers: List<String>, data: List<List<Cell>>, selected: Int, onSelect: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        headers.forEachIndexed { i, h ->
            val sample = data.firstNotNullOfOrNull { r -> r.getOrNull(i)?.takeIf { !it.isBlank }?.let(::cellText) }
            val label = if (h.startsWith("Coluna ") && sample != null) "$h · ${sample.take(14)}" else h
            FilterChip(
                selected = i == selected, onClick = { onSelect(i) },
                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = if (i == selected) ({ Icon(Icons.Rounded.Check, null, Modifier.size(18.dp)) }) else null,
            )
        }
    }
}

@Composable
private fun PreviewLine(line: StatementLine) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(line.description, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(Dates.full(line.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        MoneyText(
            line.amount, signed = true, style = MaterialTheme.typography.titleSmall,
            color = if (line.amount > 0) incomeColor() else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/* ================================== conferência =================================== */

@Composable
private fun ReviewStep(vm: ImportModel, l: Ledger, devMode: Boolean, onBack: () -> Unit, onReport: () -> Unit) {
    val st = vm.statement ?: return
    val t = vm.target ?: return
    val rows = vm.rows
    var editing by remember { mutableStateOf<Int?>(null) }
    var menu by remember { mutableStateOf(false) }
    val count = vm.selectedCount
    val testMode = LocalContainer.current.sandbox

    Scaffold(
        topBar = {
            CryoTopBar(Res.s(R.string.import_conferir_extrato), onBack = onBack, actions = {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, Res.s(R.string.import_mais_opcoes)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(Res.s(R.string.import_marcar_todas)) }, onClick = { menu = false; vm.selectAll(true) })
                    DropdownMenuItem(text = { Text(Res.s(R.string.import_desmarcar_todas)) }, onClick = { menu = false; vm.selectAll(false) })
                    DropdownMenuItem(
                        text = { Text(Res.s(R.string.import_inverter_entradas_saidas)) }, onClick = { menu = false; vm.toggleInvert() },
                        leadingIcon = { Icon(Icons.Rounded.SwapVert, null) },
                    )
                    if (devMode) {
                        DropdownMenuItem(
                            text = { Text(Res.s(R.string.import_relatorio_leitura)) }, onClick = { menu = false; onReport() },
                            leadingIcon = { Icon(Icons.Rounded.Description, null) },
                        )
                    }
                }
            })
        },
        bottomBar = {
            BottomButton(
                when {
                    vm.busy -> Res.s(R.string.import_importando)
                    count == 0 -> Res.s(R.string.import_marque_que_quer_importar)
                    else -> Res.p(R.plurals.import_button, count, count)
                },
                enabled = count > 0 && !vm.busy && !vm.planning, busy = vm.busy, onClick = vm::import,
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(bottom = 16.dp)) {
            if (testMode) item { Box(Modifier.padding(horizontal = 16.dp)) { TestModeImportNote() } }
            item { ReviewHeader(vm.fileName, st) }
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    FieldLabel(Res.s(R.string.import_para_onde_vao))
                    SourceChips(
                        l.activeAccounts, l.activeCards,
                        selected = t.cardId?.let { Source.Card(it) } ?: t.accountId?.let { Source.Acc(it) },
                        onSelect = { s ->
                            when (s) {
                                is Source.Acc -> vm.chooseTarget(ImportTarget(accountId = s.id))
                                is Source.Card -> vm.chooseTarget(ImportTarget(cardId = s.id))
                            }
                        },
                    )
                    val card = t.cardId?.let { l.card[it] }
                    if (card != null) InvoiceChooser(st, card, t.invoiceYm, l.today, vm::setInvoice)
                }
            }
            if (vm.autoInverted) {
                item {
                    Banner(
                        Icons.Rounded.SwapVert,
                        Res.s(R.string.import_compras_vieram_com_valor),
                        MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer,
                        Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                    )
                }
            }
            val repeated = vm.selectedDuplicates
            if (repeated > 0) {
                item {
                    Banner(
                        Icons.Rounded.ErrorOutline,
                        Res.p(R.plurals.import_selected_duplicates, repeated, repeated),
                        MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer,
                        Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                    )
                }
            }
            val unpaired = vm.selectedUnpairedPayments
            if (unpaired > 0) {
                item {
                    Banner(
                        Icons.Rounded.CreditCard, unpairedText(unpaired, t.isCard),
                        CryoTheme.colors.warningContainer, CryoTheme.colors.onWarningContainer,
                        Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                    )
                }
            }
            val flagged = rows.count { it.flag != RowFlag.NONE && !it.selected }
            if (flagged > 0) {
                item {
                    Banner(
                        Icons.Rounded.Info, flaggedText(rows),
                        CryoTheme.colors.warningContainer, CryoTheme.colors.onWarningContainer,
                        Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                    )
                }
            }
            item { Totals(rows) }
            items(rows, key = { it.index }) { r ->
                ReviewRow(r, l, t, onToggle = { vm.toggle(r.index) }, onClick = { editing = r.index })
            }
        }
    }

    editing?.let { idx ->
        rows.firstOrNull { it.index == idx }?.let { r ->
            RowEditor(
                r, t, l,
                onDismiss = { editing = null },
                onSave = { desc, cat, kind, paired, selected ->
                    vm.saveRow(idx, desc, cat, kind, paired, selected)
                    editing = null
                },
            )
        }
    }
}

private fun flaggedText(rows: List<ImportRow>): String {
    val parts = listOfNotNull(
        rows.count { it.flag == RowFlag.IMPORTED_BEFORE && !it.selected }.takeIf { it > 0 }?.let { Res.p(R.plurals.import_flag_imported, it, it) },
        rows.count { it.flag == RowFlag.LOOKS_DUPLICATE && !it.selected }.takeIf { it > 0 }?.let { Res.p(R.plurals.import_flag_duplicate, it, it) },
        rows.count { (it.flag == RowFlag.INVOICE_PAYMENT || it.flag == RowFlag.CARD_CREDIT) && !it.selected }.takeIf { it > 0 }
            ?.let { Res.p(R.plurals.import_flag_invoice_payment, it, it) },
    )
    return Res.s(R.string.import_flagged_summary, parts.joinToString(Res.s(R.string.list_separator)))
}

/** Pagamentos de fatura marcados sem o outro lado: o que vai acontecer e como resolver (se for o caso). */
private fun unpairedText(n: Int, card: Boolean): String =
    Res.p(if (card) R.plurals.import_unpaired_card else R.plurals.import_unpaired_account, n, n)

@Composable
private fun ReviewHeader(fileName: String, st: Statement) {
    val first = st.lines.minOfOrNull { it.date }
    val last = st.lines.maxOfOrNull { it.date }
    CryoCard(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.Description, MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(fileName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val period = if (first != null && last != null) {
                    if (first == last) Dates.full(first) else "${Dates.full(first)} a ${Dates.full(last)}"
                } else ""
                Text(
                    listOf(st.format.label, Res.p(R.plurals.count_transactions, st.lines.size, st.lines.size), period)
                        .filter { it.isNotEmpty() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                st.accountHint?.let {
                    Text(Res.s(R.string.import_arquivo, it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun InvoiceChooser(st: Statement, card: CreditCard, selected: Int?, today: LocalDate, onSelect: (Int?) -> Unit) {
    val center = remember(st, card) { ImportPlanner.defaultInvoice(st, card) ?: CardMath.invoiceFor(card, today).let { it.year * 100 + it.monthValue } }
    val months = listOf(-1L, 0L, 1L).map { ymOf(center).plusMonths(it) }
    fun label(ym: java.time.YearMonth) = if (ym.year == today.year) Dates.monthName(ym) else "${Dates.monthShort(ym)} ${ym.year}"
    FieldLabel(Res.s(R.string.import_qual_fatura))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        months.forEach { ym ->
            val key = ym.year * 100 + ym.monthValue
            FilterChip(selected = selected == key, onClick = { onSelect(key) }, label = { Text(label(ym)) })
        }
        FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text(Res.s(R.string.import_pela_data_cada_compra)) })
    }
    val chosen = selected?.let { ymOf(it) }
    Text(
        if (chosen != null) {
            Res.s(R.string.import_fatura_que_vence_todas, label(chosen).lowercase(), Dates.full(CardMath.dueDate(card, chosen)))
        } else {
            Res.s(R.string.import_cada_compra_vai_para)
        },
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun Totals(rows: List<ImportRow>) {
    val sel = rows.filter { it.selected }
    val income = sel.filter { it.type == TxType.INCOME && it.pairedId == null }.sumOf { it.amount }
    val expense = sel.filter { it.type == TxType.EXPENSE && it.pairedId == null }.sumOf { it.amount }
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            Res.s(R.string.import_marcadas, sel.size, rows.size), style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
        )
        if (income > 0) MoneyText(income, signed = true, style = MaterialTheme.typography.bodyMedium, color = incomeColor())
        if (income > 0 && expense > 0) Spacer(Modifier.width(12.dp))
        if (expense > 0) MoneyText(-expense, signed = true, style = MaterialTheme.typography.bodyMedium)
    }
    HorizontalDivider(Modifier.padding(top = 4.dp))
}

private fun flagLabel(r: ImportRow, t: ImportTarget, l: Ledger): String? = when {
    r.pairedId != null && r.canBeInvoicePayment ->
        if (t.isCard) Res.s(R.string.import_pagamento_fatura_saindo, l.accountName(r.pairedId)) else Res.s(R.string.import_pagamento_fatura_cartao, l.cardName(r.pairedId))
    r.flag == RowFlag.IMPORTED_BEFORE -> Res.s(R.string.import_ja_importada_antes)
    r.flag == RowFlag.LOOKS_DUPLICATE -> r.match?.let { Res.s(R.string.import_parece_repetida_2, it.description.ifBlank { Res.s(R.string.import_sem_descricao) }, Dates.short(it.date)) } ?: Res.s(R.string.import_parece_repetida)
    r.flag == RowFlag.CARD_CREDIT -> Res.s(R.string.import_pagamento_fatura_toque_para_2)
    r.flag == RowFlag.INVOICE_PAYMENT -> Res.s(R.string.import_pagamento_fatura_toque_para)
    else -> null
}

@Composable
private fun ReviewRow(r: ImportRow, l: Ledger, t: ImportTarget, onToggle: () -> Unit, onClick: () -> Unit) {
    val cat = r.categoryId?.let { l.category[it] }
    val paired = r.pairedId != null && r.canBeInvoicePayment
    val date = if (r.line.date.year == l.today.year) Dates.short(r.line.date) else Dates.full(r.line.date)
    val subtitle = listOfNotNull(date, if (paired) null else cat?.name).joinToString(" · ")
    val flag = flagLabel(r, t, l)
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // A caixa é um toque à parte da linha (que abre a conferência): o leitor de tela precisa saber de qual linha ela é.
        Checkbox(
            checked = r.selected, onCheckedChange = { onToggle() },
            modifier = Modifier.semantics { contentDescription = Res.s(R.string.import_importar, r.line.description) },
        )
        Row(Modifier.weight(1f).alpha(if (r.selected) 1f else 0.6f), verticalAlignment = Alignment.CenterVertically) {
            when {
                paired -> IconBadge(Icons.Rounded.CreditCard, MaterialTheme.colorScheme.primary)
                cat != null -> IconBadge(categoryIcon(cat.icon), Color(cat.color))
                else -> IconBadge(typeIcon(r.type), MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(r.line.description, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                if (flag != null) {
                    Text(
                        flag, style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        color = if (paired) MaterialTheme.colorScheme.primary else CryoTheme.colors.onWarningContainer,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            MoneyText(
                r.signed, signed = true, style = MaterialTheme.typography.titleSmall,
                color = if (r.type == TxType.INCOME) incomeColor() else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/* ================================ editar uma linha ================================= */

@Composable
private fun RowEditor(
    r: ImportRow,
    t: ImportTarget,
    l: Ledger,
    onDismiss: () -> Unit,
    onSave: (description: String, categoryId: Long?, kind: CategoryKind, paired: Long?, selected: Boolean) -> Unit,
) {
    // Se a linha mudar de tipo enquanto a folha está aberta (sinais invertidos), as escolhas recomeçam.
    var description by remember(r.index, r.type) { mutableStateOf(r.line.description) }
    var category by remember(r.index, r.type) { mutableStateOf(r.categoryId) }
    var paired by remember(r.index, r.type) { mutableStateOf(r.pairedId) }
    var selected by remember(r.index, r.type) { mutableStateOf(r.selected) }
    val kind = ImportModel.kindOf(r.type)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(Res.s(R.string.import_conferir_movimentacao), style = MaterialTheme.typography.titleLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${Dates.full(r.line.date)} · ", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MoneyText(
                    r.signed, signed = true, style = MaterialTheme.typography.bodyMedium,
                    color = if (r.type == TxType.INCOME) incomeColor() else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            explanation(r, t, l)?.let {
                Spacer(Modifier.height(12.dp))
                Banner(Icons.Rounded.Info, it, CryoTheme.colors.warningContainer, CryoTheme.colors.onWarningContainer)
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                description, { description = it }, label = { Text(Res.s(R.string.tx_descricao)) }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (r.canBeInvoicePayment) {
                FieldLabel(if (t.isCard) Res.s(R.string.import_pagamento_fatura_qual_conta) else Res.s(R.string.import_pagamento_qual_cartao))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (t.isCard) {
                        l.activeAccounts.forEach { a ->
                            FilterChip(selected = paired == a.id, onClick = { paired = a.id; selected = true }, label = { Text(a.name) })
                        }
                    } else {
                        l.activeCards.forEach { cd ->
                            FilterChip(selected = paired == cd.id, onClick = { paired = cd.id; selected = true }, label = { Text(cd.name) })
                        }
                    }
                    FilterChip(
                        selected = paired == null, onClick = { paired = null },
                        label = { Text(if (t.isCard) Res.s(R.string.import_nao_pagamento_estorno_ou) else Res.s(R.string.import_nao_gasto_comum)) },
                    )
                }
            }
            if (paired == null) {
                FieldLabel(Res.s(R.string.plan_categoria))
                CategoryGrid(l.categories(kind), category, { category = it.id })
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                    .toggleable(value = selected, role = Role.Checkbox, onValueChange = { selected = it })
                    .semantics { stateDescription = if (selected) Res.s(R.string.import_marcada) else Res.s(R.string.import_desmarcada) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = selected, onCheckedChange = null)
                Spacer(Modifier.width(8.dp))
                Text(Res.s(R.string.import_importar_esta_movimentacao), style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { onSave(description, if (paired == null) category else null, kind, paired, selected) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Icon(Icons.Rounded.Check, null); Spacer(Modifier.width(8.dp)); Text(Res.s(R.string.import_pronto)) }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(Res.s(R.string.wallet_cancelar)) }
        }
    }
}

private fun explanation(r: ImportRow, t: ImportTarget, l: Ledger): String? = when (r.flag) {
    RowFlag.IMPORTED_BEFORE -> Res.s(R.string.import_esta_movimentacao_ja_veio)
    RowFlag.LOOKS_DUPLICATE -> r.match?.let { m ->
        Res.s(R.string.import_parece_com_que_ja, m.description.ifBlank { Res.s(R.string.import_sem_descricao) }, Dates.full(m.date), Money.format(m.amount), m.accountId?.let { " em ${l.accountName(it)}" } ?: m.cardId?.let { Res.s(R.string.import_cartao_2, l.cardName(it)) } ?: "")
    } ?: Res.s(R.string.import_parece_com_algo_que)
    RowFlag.CARD_CREDIT ->
        Res.s(R.string.import_parece_pagamento_fatura_cryo)
    RowFlag.INVOICE_PAYMENT ->
        Res.s(R.string.import_parece_pagamento_fatura_cartao)
    RowFlag.NONE -> if (t.isCard && r.type == TxType.INCOME) "Crédito na fatura (estorno, reembolso ou ajuste): diminui o valor da fatura." else null
}

/* ===================================== pronto ===================================== */

@Composable
private fun DoneStep(step: ImportModel.Step.Done, vm: ImportModel, l: Ledger, devMode: Boolean, onReport: () -> Unit) {
    val nav = LocalNav.current
    val t = step.target
    val where = t.cardId?.let { Res.s(R.string.import_cartao, l.cardName(it)) } ?: Res.s(R.string.import_conta, l.accountName(t.accountId))
    val balance = vm.statement?.balance
    Scaffold(
        topBar = { CryoTopBar(Res.s(R.string.import_importar_extrato), onBack = nav::back) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Box(
                Modifier.size(80.dp).clip(CircleShape)
                    .background(if (step.undone) MaterialTheme.colorScheme.surfaceContainerHighest else CryoTheme.colors.incomeContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (step.undone) Icons.AutoMirrored.Rounded.Undo else Icons.Rounded.CheckCircle, null, Modifier.size(44.dp),
                    tint = if (step.undone) MaterialTheme.colorScheme.onSurfaceVariant else CryoTheme.colors.onIncomeContainer,
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                when {
                    step.undone -> Res.s(R.string.import_importacao_desfeita)
                    else -> Res.p(R.plurals.import_done_title, step.count, step.count)
                },
                style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                when {
                    step.undone -> Res.s(R.string.import_nada_deste_arquivo_ficou)
                    else -> Res.p(R.plurals.import_done_where, step.count, where)
                },
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
            )
            if (!step.undone && balance != null && t.accountId != null) {
                Spacer(Modifier.height(20.dp))
                CryoCard {
                    Row(Modifier.fillMaxWidth()) {
                        Text(Res.s(R.string.import_saldo_fim_extrato), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        MoneyText(balance, style = MaterialTheme.typography.titleSmall)
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        Text(Res.s(R.string.import_saldo_cryo_agora), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        MoneyText(l.balance(t.accountId), style = MaterialTheme.typography.titleSmall)
                    }
                    if (balance != l.balance(t.accountId)) {
                        Text(
                            Res.s(R.string.import_estao_diferentes_pode_ser),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            if (!step.undone) {
                Button(
                    onClick = { nav.go(t.cardId?.let { Routes.card(it) } ?: Routes.account(t.accountId ?: 0)) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) { Text(if (t.isCard) Res.s(R.string.import_ver_cartao) else Res.s(R.string.import_ver_conta)) }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = vm::undo, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.Undo, null)
                    Spacer(Modifier.width(8.dp))
                    Text(Res.s(R.string.import_desfazer_importacao))
                }
            }
            TextButton(onClick = vm::reset, modifier = Modifier.padding(top = 8.dp)) { Text(Res.s(R.string.import_importar_outro_arquivo)) }
            if (devMode) TextButton(onClick = onReport) { Text(Res.s(R.string.import_relatorio_leitura)) }
        }
    }
}
