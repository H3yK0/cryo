// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.screens

import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.Res
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.h3yk0.cryo.AppContainer
import io.github.h3yk0.cryo.data.AutoBackupSettings
import io.github.h3yk0.cryo.data.Backup
import io.github.h3yk0.cryo.data.BackupFrequency
import io.github.h3yk0.cryo.data.BackupOutcome
import io.github.h3yk0.cryo.data.BackupPlaces
import io.github.h3yk0.cryo.data.BackupPolicy
import io.github.h3yk0.cryo.data.BackupStatus
import io.github.h3yk0.cryo.data.BackupTarget
import io.github.h3yk0.cryo.data.BackupTargetState
import io.github.h3yk0.cryo.data.FolderMode
import io.github.h3yk0.cryo.data.FoundBackup
import io.github.h3yk0.cryo.data.PlaceCheck
import io.github.h3yk0.cryo.domain.Dates
import io.github.h3yk0.cryo.notify.AutoBackupWork
import io.github.h3yk0.cryo.notify.Reminders
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.components.Banner
import io.github.h3yk0.cryo.ui.components.ChoiceRow
import io.github.h3yk0.cryo.ui.components.ConfirmDialog
import io.github.h3yk0.cryo.ui.components.CryoCard
import io.github.h3yk0.cryo.ui.components.CryoTopBar
import io.github.h3yk0.cryo.ui.components.EmptyState
import io.github.h3yk0.cryo.ui.components.FieldLabel
import io.github.h3yk0.cryo.ui.components.IconBadge
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.SectionTitle
import io.github.h3yk0.cryo.ui.components.Segmented
import io.github.h3yk0.cryo.ui.components.SwitchRow
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import io.github.h3yk0.cryo.ui.tx.LoadingBox
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/** "hoje às 03:12", "ontem às 22:05", "23/09 às 03:12". */
fun backupWhen(at: Long, now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): String {
    val d = Instant.ofEpochMilli(at).atZone(zone)
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val time = "%02d:%02d".format(d.hour, d.minute)
    val day = d.toLocalDate()
    return when {
        day == today -> Res.s(R.string.backup_hoje, time)
        day == today.minusDays(1) -> Res.s(R.string.backup_ontem, time)
        day.year == today.year -> Res.s(R.string.backup_texto, Dates.short(day), time)
        else -> Res.s(R.string.backup_texto, Dates.full(day), time)
    }
}

/** Resumo para a linha "Backup automático" dos Ajustes. */
fun autoBackupSummary(s: AutoBackupSettings, now: Long = System.currentTimeMillis()): String {
    val last = listOfNotNull(s.folder.lastAt, s.cloud.lastAt).maxOrNull()
    return when {
        !s.enabled -> Res.s(R.string.backup_desligado_toque_para_configurar)
        !s.hasTarget -> Res.s(R.string.backup_escolha_onde_guardar)
        s.hasProblem -> Res.s(R.string.backup_atencao_ultimo_backup_nao)
        last != null -> Res.s(R.string.backup_ultimo, s.frequency.label, backupWhen(last, now))
        else -> Res.s(R.string.backup_primeiro_ainda_vai_ser, s.frequency.label)
    }
}

/** Lugar escolhido que precisa de uma decisão da pessoa antes de ser usado. */
private class PendingPlace(
    val t: BackupTarget,
    val uri: Uri,
    val previous: String?,
    val check: PlaceCheck,
    val overwrites: Boolean,
    /** Modo da pasta que passa a valer quando a pessoa confirmar (ao trocar para "substituir sempre o mesmo"). */
    val mode: FolderMode?,
)

/** Roda uma tarefa do backup sem deixar um erro inesperado (ex.: celular sem espaço) fechar o app. */
private fun AppContainer.safely(done: () -> Unit = {}, block: suspend () -> Unit) = launch {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        message(Res.s(R.string.backup_nao_consegui_concluir_agora))
    } finally {
        done()
    }
}

@Composable
fun AutoBackupScreen() {
    val c = LocalContainer.current
    val nav = LocalNav.current
    val ctx = LocalContext.current
    if (c.sandbox) return TestModeBackupNotice(onBack = nav::back)
    val state by c.autoBackup.flow.collectAsState(initial = null)
    val s = state ?: return LoadingBox()
    var running by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<PendingPlace?>(null) }
    var canNotify by remember { mutableStateOf(Reminders.canNotify(ctx)) }

    /**
     * Depois de escolher a pasta ou o arquivo (ou ao passar a pasta para "substituir sempre o mesmo"): guarda a
     * permissão, confere o que já existe lá e faz o primeiro backup. [mode]: o modo da pasta que vai valer.
     */
    fun picked(t: BackupTarget, uri: Uri, mode: FolderMode = s.folderMode) {
        val previous = s.target(t).uri
        if (runCatching { BackupPlaces.keepAccess(ctx, uri) }.isFailure) {
            c.message(
                if (t == BackupTarget.FOLDER) Res.s(R.string.backup_nao_deu_para_usar)
                else Res.s(R.string.backup_essa_nuvem_nao_deixa),
            )
            return
        }
        val overwrites = t == BackupTarget.CLOUD || mode == FolderMode.REPLACE
        val newMode = mode.takeIf { t == BackupTarget.FOLDER && it != s.folderMode }
        val lastAt = s.target(t).lastAt
        running = true
        c.safely({ running = false }) {
            // Confere sempre, até o mesmo lugar de antes: ele pode ter um backup bom que não deve ser substituído.
            val check = c.backupRunner.inspect(t, uri, mode)
            // O arquivo que está lá é o último que este celular gravou, e desde então só entrou coisa nova.
            val ownLast = uri.toString() == previous && check is PlaceCheck.HasBackup &&
                check.found.exportedAt != null && check.found.exportedAt == lastAt && !check.found.richer
            val nothingToAsk = check == PlaceCheck.Clear || ownLast ||
                (check is PlaceCheck.HasBackup && !check.atRisk && !check.found.richer)
            when {
                check is PlaceCheck.Unreachable -> {
                    if (uri.toString() != previous) BackupPlaces.releaseAccess(ctx, uri.toString())
                    c.message(check.reason)
                }
                nothingToAsk -> usePlace(c, ctx, t, uri, previous, mode = newMode)
                else -> pending = PendingPlace(t, uri, previous, check, overwrites, newMode)
            }
        }
    }

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) picked(BackupTarget.FOLDER, uri)
    }
    val pickCloud = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) picked(BackupTarget.CLOUD, uri)
    }
    fun open(launch: () -> Unit) {
        try {
            launch()
        } catch (_: ActivityNotFoundException) {
            c.message(Res.s(R.string.backup_este_celular_nao_tem))
        }
    }
    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        canNotify = granted
        if (!granted) c.message(Res.s(R.string.backup_sem_permissao_aviso_nao))
    }

    Scaffold(topBar = { CryoTopBar(Res.s(R.string.settings_backup_automatico), onBack = nav::back) }, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                CryoCard(color = MaterialTheme.colorScheme.primaryContainer) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Backup, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            Res.s(R.string.backup_copia_tudo_sem_voce), style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Text(
                        Res.s(R.string.backup_cryo_salva_seus_dados),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
            item {
                SwitchRow(
                    Res.s(R.string.backup_fazer_backup_automatico), s.enabled,
                    { v ->
                        c.safely {
                            c.autoBackup.setEnabled(v)
                            runCatching { AutoBackupWork.sync(ctx, v && s.hasTarget) }
                        }
                    },
                    if (s.hasTarget) Res.s(R.string.backup_segundo_plano_mesmo_com) else Res.s(R.string.backup_escolha_abaixo_onde_guardar),
                )
            }
            if (s.enabled && s.hasTarget && !canNotify && Build.VERSION.SDK_INT >= 33) {
                item {
                    Banner(
                        Icons.Rounded.NotificationsActive,
                        Res.s(R.string.backup_toque_aqui_permita_notificacoes),
                        MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer,
                    ) { askNotify.launch(Manifest.permission.POST_NOTIFICATIONS) }
                }
            }
            item {
                FieldLabel(Res.s(R.string.backup_com_que_frequencia))
                Segmented(
                    BackupFrequency.entries.map { it to it.label }, s.frequency,
                    { f -> c.safely { c.autoBackup.setFrequency(f) } },
                )
                Text(
                    Res.s(R.string.backup_se_nada_mudou_desde),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            item { SectionTitle(Res.s(R.string.backup_celular)) }
            item {
                FolderCard(
                    s,
                    onPick = { open { pickFolder.launch(null) } },
                    onRemove = { remove(c, ctx, BackupTarget.FOLDER, s.folder.uri) },
                    onMode = { m ->
                        val folder = s.folder.uri
                        when {
                            m == s.folderMode || running -> Unit
                            // Passar a substituir um cryo-backup.json que não é deste celular: confere antes, como ao escolher a pasta.
                            m == FolderMode.REPLACE && folder != null && BackupPolicy.SINGLE_NAME !in s.folder.owned ->
                                picked(BackupTarget.FOLDER, Uri.parse(folder), FolderMode.REPLACE)
                            else -> c.safely { c.autoBackup.setFolderMode(m) }
                        }
                    },
                )
            }

            item { SectionTitle(Res.s(R.string.backup_nuvem_2)) }
            item { CloudCard(s, onPick = { open { pickCloud.launch(BackupPolicy.SINGLE_NAME) } }, onRemove = { remove(c, ctx, BackupTarget.CLOUD, s.cloud.uri) }) }

            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        running = true
                        c.safely({ running = false }) { c.message(summary(c.backupRunner.run(force = true))) }
                    },
                    enabled = s.hasTarget && !running,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Icon(Icons.Rounded.Backup, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (running) Res.s(R.string.backup_salvando) else Res.s(R.string.backup_fazer_backup_agora))
                }
            }
            item {
                Text(
                    Res.s(R.string.backup_para_voltar_backup_use),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }

    pending?.let { p ->
        fun cancel() {
            pending = null
            if (p.uri.toString() != p.previous) BackupPlaces.releaseAccess(ctx, p.uri.toString())
        }
        // A pessoa decidiu usar o lugar com os dados deste celular: o que já estava lá deixa de ser "deste celular"
        // (nada dele é apagado depois) e o primeiro backup pode substituir o arquivo que ela viu.
        fun keep() {
            pending = null
            running = true
            c.safely({ running = false }) { usePlace(c, ctx, p.t, p.uri, p.previous, fresh = true, mode = p.mode) }
        }
        when (val check = p.check) {
            is PlaceCheck.HasBackup -> ExistingBackupDialog(
                check.found, p.t, overwrites = p.overwrites && check.atRisk,
                replaceMode = p.t == BackupTarget.FOLDER && p.overwrites,
                onRestore = {
                    pending = null
                    running = true
                    c.safely({ running = false }) {
                        val restored = check.found.restored
                        val ok = runCatching { c.repo.replaceAll(restored.snapshot, fromOldVersion = restored.version < Backup.VERSION) }.isSuccess
                        if (ok) {
                            usePlace(c, ctx, p.t, p.uri, p.previous, restored = true, fresh = true, mode = p.mode)
                        } else {
                            if (p.uri.toString() != p.previous) BackupPlaces.releaseAccess(ctx, p.uri.toString())
                            c.message(Res.s(R.string.backup_nao_consegui_restaurar_esse))
                        }
                    }
                },
                onKeep = ::keep,
                onCancel = ::cancel,
            )
            is PlaceCheck.CantRead -> ConfirmDialog(
                Res.s(R.string.backup_usar_este_lugar),
                Res.s(R.string.backup_se_continuar_ele_sera, check.reason),
                Res.s(R.string.backup_substituir), onConfirm = ::keep, onDismiss = ::cancel,
            )
            PlaceCheck.Clear, is PlaceCheck.Unreachable -> Unit
        }
    }
}

@Composable
private fun FolderCard(s: AutoBackupSettings, onPick: () -> Unit, onRemove: () -> Unit, onMode: (FolderMode) -> Unit) {
    val c = LocalContainer.current
    CryoCard {
        if (!s.folder.isSet) {
            Place(Icons.Rounded.Folder, Res.s(R.string.backup_nenhuma_pasta_escolhida), null)
            Text(
                Res.s(R.string.backup_guarde_proprio_celular_ou),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Button(onClick = onPick, modifier = Modifier.padding(top = 12.dp)) {
                Icon(Icons.Rounded.Folder, null); Spacer(Modifier.width(8.dp)); Text(Res.s(R.string.backup_escolher_pasta))
            }
            return@CryoCard
        }
        Place(Icons.Rounded.Folder, s.folder.name, s.folder)
        FieldLabel(Res.s(R.string.backup_como_guardar))
        ChoiceRow(
            Res.s(R.string.backup_substituir_sempre_mesmo_arquivo), Res.s(R.string.backup_fica_so_copia_mais, BackupPolicy.SINGLE_NAME),
            s.folderMode == FolderMode.REPLACE,
        ) { onMode(FolderMode.REPLACE) }
        ChoiceRow(
            Res.s(R.string.backup_guardar_varias_copias), Res.s(R.string.backup_arquivo_novo_cada_backup),
            s.folderMode == FolderMode.COPIES,
        ) { onMode(FolderMode.COPIES) }
        if (s.folderMode == FolderMode.COPIES) {
            FieldLabel(Res.s(R.string.backup_quantas_copias_guardar))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5 to "5", 10 to "10", 30 to "30", 0 to Res.s(R.string.backup_todas)).forEach { (n, label) ->
                    FilterChip(selected = s.keep == n, onClick = { c.safely { c.autoBackup.setKeep(n) } }, label = { Text(label) })
                }
            }
            Text(
                if (s.keep == 0) Res.s(R.string.backup_nenhuma_copia_apagada_vez)
                else Res.s(R.string.backup_quando_passar_copia_mais, s.keep),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onPick) { Text(Res.s(R.string.backup_trocar_pasta)) }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onRemove) { Text(Res.s(R.string.backup_parar_usar)) }
        }
    }
}

@Composable
private fun CloudCard(s: AutoBackupSettings, onPick: () -> Unit, onRemove: () -> Unit) {
    CryoCard {
        if (!s.cloud.isSet) {
            Place(Icons.Rounded.Cloud, Res.s(R.string.backup_nenhuma_nuvem_escolhida), null)
            Text(
                Res.s(R.string.backup_funciona_com_google_drive),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            listOf(
                Res.s(R.string.backup_toque_escolher_nuvem),
                Res.s(R.string.backup_tela_que_abrir_toque),
                Res.s(R.string.backup_escolha_pasta_toque_salvar),
            ).forEachIndexed { i, step ->
                Text(
                    "${i + 1}. $step", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp),
                )
            }
            Button(onClick = onPick, modifier = Modifier.padding(top = 12.dp)) {
                Icon(Icons.Rounded.Cloud, null); Spacer(Modifier.width(8.dp)); Text(Res.s(R.string.backup_escolher_nuvem))
            }
            return@CryoCard
        }
        Place(Icons.Rounded.Cloud, s.cloud.name, s.cloud)
        Text(
            Res.s(R.string.backup_cada_backup_cryo_atualiza),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onPick) { Text(Res.s(R.string.onboarding_trocar)) }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onRemove) { Text(Res.s(R.string.backup_parar_usar)) }
        }
    }
}

/** Ícone + nome do lugar + situação do último backup. */
@Composable
private fun Place(icon: ImageVector, title: String, t: BackupTargetState?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon, MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (t != null) TargetStatus(t)
        }
    }
}

@Composable
private fun TargetStatus(t: BackupTargetState) {
    val now = System.currentTimeMillis()
    val error = MaterialTheme.colorScheme.error
    val (text, color, icon) = when {
        t.lastError != null -> Triple(Res.s(R.string.backup_nao_salvou, backupWhen(t.lastErrorAt ?: now, now), t.lastError), error, Icons.Rounded.ErrorOutline)
        t.lastAt != null -> Triple(
            Res.s(R.string.backup_ultimo_backup, backupWhen(t.lastAt, now)) + if (t.checkedAt != null && t.checkedAt > t.lastAt) Res.s(R.string.backup_nada_mudou_desde_entao) else "",
            CryoTheme.colors.income, Icons.Rounded.CheckCircle,
        )
        else -> Triple(Res.s(R.string.backup_primeiro_backup_ainda_nao), MaterialTheme.colorScheme.onSurfaceVariant, Icons.Rounded.Schedule)
    }
    Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = color, modifier = Modifier.padding(top = 1.dp).size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text, style = MaterialTheme.typography.bodySmall,
            color = if (t.lastError != null) error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * O lugar escolhido já tem um backup do Cryo com dados diferentes. A pessoa decide: trazer esse backup para o celular,
 * seguir com os dados do celular (na nuvem e no "substituir sempre o mesmo", isso troca o backup que está lá) ou desistir.
 * O botão em destaque é o que guarda mais movimentações, para um toque apressado não perder dados.
 */
@Composable
fun ExistingBackupDialog(
    found: FoundBackup,
    target: BackupTarget,
    overwrites: Boolean,
    replaceMode: Boolean,
    onRestore: () -> Unit,
    onKeep: () -> Unit,
    onCancel: () -> Unit,
) {
    BasicAlertDialog(onDismissRequest = onCancel) {
        ExistingBackupContent(found, target, overwrites, replaceMode, onRestore, onKeep, onCancel)
    }
}

@Composable
fun ExistingBackupContent(
    found: FoundBackup,
    target: BackupTarget,
    /** Seguir com os dados deste celular substitui o backup encontrado. */
    overwrites: Boolean,
    /** Pasta no modo "substituir sempre o mesmo": o cryo-backup.json passa a ter os dados deste celular. */
    replaceMode: Boolean,
    onRestore: () -> Unit,
    onKeep: () -> Unit,
    onCancel: () -> Unit,
) {
    val keepLabel = if (overwrites) Res.s(R.string.backup_substituir_pelos_dados_deste) else Res.s(R.string.backup_continuar_sem_restaurar)
    Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) {
            Icon(
                Icons.Rounded.SettingsBackupRestore, null, tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.align(Alignment.CenterHorizontally).size(28.dp),
            )
            Text(
                Res.s(R.string.backup_ja_existe_backup_aqui), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )
            Text(
                (if (target == BackupTarget.FOLDER) Res.s(R.string.backup_esta_pasta) else Res.s(R.string.backup_este_arquivo)) +
                    Res.s(R.string.backup_ja_tem_backup_cryo),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
            Compare(
                if (target == BackupTarget.FOLDER) Icons.Rounded.Folder else Icons.Rounded.Cloud,
                Res.s(R.string.backup_backup) + (found.exportedAt?.let { Res.s(R.string.backup_salvo, backupWhen(it)) } ?: ""),
                counts(found.txs, found.accounts), Modifier.padding(top = 16.dp),
            )
            Compare(
                Icons.Rounded.Smartphone, Res.s(R.string.backup_neste_celular_agora), counts(found.currentTxs, found.currentAccounts),
                Modifier.padding(top = 8.dp),
            )
            Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (found.richer) {
                    Button(onClick = onRestore, modifier = Modifier.fillMaxWidth()) { Text(Res.s(R.string.backup_restaurar_este_backup)) }
                    OutlinedButton(onClick = onKeep, modifier = Modifier.fillMaxWidth()) { Text(keepLabel, textAlign = TextAlign.Center) }
                } else {
                    Button(onClick = onKeep, modifier = Modifier.fillMaxWidth()) { Text(keepLabel, textAlign = TextAlign.Center) }
                    OutlinedButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) { Text(Res.s(R.string.backup_restaurar_este_backup)) }
                }
            }
            Text(
                Res.s(R.string.backup_restaurar_troca_dados_deste) + when {
                    overwrites -> Res.s(R.string.backup_substituir_troca_backup_pelos)
                    replaceMode -> Res.s(R.string.backup_continuar_mantem_outros_backups, BackupPolicy.SINGLE_NAME)
                    else -> Res.s(R.string.backup_backups_que_ja_estao)
                },
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
            TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.End).padding(top = 8.dp)) { Text(Res.s(R.string.wallet_cancelar)) }
        }
    }
}

@Composable
private fun Compare(icon: ImageVector, title: String, detail: String, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shape = MaterialTheme.shapes.medium, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelLarge)
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun counts(txs: Int, accounts: Int) =
    "${plural(R.plurals.count_transactions, txs)} · ${plural(R.plurals.count_accounts, accounts)}"

private fun summary(r: List<BackupOutcome>): String {
    r.firstOrNull { it.status == BackupStatus.FAILED || it.status == BackupStatus.HELD }?.let { return it.message ?: Res.s(R.string.backup_backup_falhou) }
    if (r.any { it.status == BackupStatus.NOTHING_TO_SAVE }) return Res.s(R.string.backup_ainda_nao_ha_nada)
    val written = r.filter { it.status == BackupStatus.WRITTEN }.map { it.target }
    return when {
        written.containsAll(BackupTarget.entries) -> Res.s(R.string.backup_backup_feito_pasta_nuvem)
        BackupTarget.FOLDER in written -> Res.s(R.string.backup_backup_feito_pasta)
        BackupTarget.CLOUD in written -> Res.s(R.string.backup_backup_feito_nuvem)
        else -> Res.s(R.string.backup_nada_para_salvar_agora)
    }
}

/**
 * Passa a usar o lugar: guarda, liga o backup automático e já faz o primeiro backup (que pode substituir o arquivo
 * que estava lá, porque a pessoa acabou de conferir). [fresh]: começa o histórico do zero, mesmo sendo o mesmo lugar
 * (o que já estava lá deixa de contar como deste celular e nunca é apagado). [mode]: novo modo da pasta.
 */
private suspend fun usePlace(
    c: AppContainer,
    ctx: Context,
    t: BackupTarget,
    uri: Uri,
    previous: String?,
    restored: Boolean = false,
    fresh: Boolean = false,
    mode: FolderMode? = null,
) {
    // O mesmo lugar de antes (ex.: para devolver a permissão) mantém o histórico e os arquivos que já são do Cryo.
    if (fresh || previous != uri.toString()) {
        if (previous != null && previous != uri.toString()) BackupPlaces.releaseAccess(ctx, previous)
        val name = if (t == BackupTarget.FOLDER) BackupPlaces.describeFolder(ctx, uri) else BackupPlaces.describeFile(ctx, uri)
        c.autoBackup.setTarget(t, uri.toString(), name)
    }
    if (mode != null) c.autoBackup.setFolderMode(mode)
    // O que está lá agora é o que a pessoa acabou de conferir: só isso pode ser substituído no primeiro backup.
    c.backupRunner.adopt(t, uri, mode ?: c.autoBackup.current().folderMode)
    c.autoBackup.setEnabled(true)
    runCatching { AutoBackupWork.sync(ctx, true) }
    val r = c.backupRunner.run(force = true, only = t).firstOrNull()
    val where = if (t == BackupTarget.FOLDER) Res.s(R.string.backup_pasta) else Res.s(R.string.backup_nuvem)
    c.message(
        when (r?.status) {
            BackupStatus.WRITTEN -> when {
                restored -> Res.s(R.string.backup_backup_restaurado_daqui_para, where)
                mode == FolderMode.REPLACE -> Res.s(R.string.backup_pronto_agora_backup_fica, BackupPolicy.SINGLE_NAME)
                else -> Res.s(R.string.backup_pronto_primeiro_backup_ja, where)
            }
            BackupStatus.NOTHING_TO_SAVE -> Res.s(R.string.backup_lugar_escolhido_primeiro_backup)
            BackupStatus.FAILED, BackupStatus.HELD -> r.message ?: Res.s(R.string.backup_nao_consegui_salvar_nesse)
            else -> if (restored) Res.s(R.string.settings_backup_restaurado) else Res.s(R.string.backup_lugar_escolhido)
        },
    )
}

private fun remove(c: AppContainer, ctx: Context, t: BackupTarget, uri: String?) {
    c.safely {
        BackupPlaces.releaseAccess(ctx, uri)
        c.autoBackup.setTarget(t, null, "")
        val left = c.autoBackup.current()
        runCatching { AutoBackupWork.sync(ctx, left.enabled && left.hasTarget) }
        c.message(if (t == BackupTarget.FOLDER) Res.s(R.string.backup_pasta_nao_sera_mais) else Res.s(R.string.backup_nuvem_nao_sera_mais))
    }
}

/** No modo de teste (modo desenvolvedor), os ajustes do backup ficam fora de alcance: ele cuida dos dados de verdade. */
@Composable
private fun TestModeBackupNotice(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var leaving by remember { mutableStateOf(false) }
    Scaffold(topBar = { CryoTopBar(Res.s(R.string.settings_backup_automatico), onBack = onBack) }, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            EmptyState(
                Icons.Rounded.Science, Res.s(R.string.backup_voce_esta_modo_teste),
                Res.s(R.string.backup_backup_automatico_continua_salvando),
            ) {
                Button(onClick = { leaving = true; switchTestMode(ctx, false) { leaving = false } }, enabled = !leaving) {
                    Text(Res.s(R.string.backup_sair_modo_teste))
                }
            }
        }
    }
}
