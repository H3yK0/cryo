// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.screens

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
import io.github.h3yk0.cryo.ui.components.FieldLabel
import io.github.h3yk0.cryo.ui.components.IconBadge
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.SectionTitle
import io.github.h3yk0.cryo.ui.components.Segmented
import io.github.h3yk0.cryo.ui.components.SwitchRow
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import io.github.h3yk0.cryo.ui.tx.LoadingBox
import kotlinx.coroutines.CancellationException
import java.time.Instant
import java.time.ZoneId

/** "hoje às 03:12", "ontem às 22:05", "23/09 às 03:12". */
fun backupWhen(at: Long, now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): String {
    val d = Instant.ofEpochMilli(at).atZone(zone)
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val time = "%02d:%02d".format(d.hour, d.minute)
    val day = d.toLocalDate()
    return when {
        day == today -> "hoje às $time"
        day == today.minusDays(1) -> "ontem às $time"
        day.year == today.year -> "${Dates.short(day)} às $time"
        else -> "${Dates.full(day)} às $time"
    }
}

/** Resumo para a linha "Backup automático" dos Ajustes. */
fun autoBackupSummary(s: AutoBackupSettings, now: Long = System.currentTimeMillis()): String {
    val last = listOfNotNull(s.folder.lastAt, s.cloud.lastAt).maxOrNull()
    return when {
        !s.enabled -> "Desligado · toque para configurar"
        !s.hasTarget -> "Escolha onde guardar"
        s.hasProblem -> "Atenção: o último backup não foi salvo"
        last != null -> "${s.frequency.label} · último: ${backupWhen(last, now)}"
        else -> "${s.frequency.label} · o primeiro ainda vai ser feito"
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
        message("Não consegui concluir agora. Tente de novo.")
    } finally {
        done()
    }
}

@Composable
fun AutoBackupScreen() {
    val c = LocalContainer.current
    val nav = LocalNav.current
    val ctx = LocalContext.current
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
                if (t == BackupTarget.FOLDER) "Não deu para usar essa pasta. Tente outra."
                else "Essa nuvem não deixa o Cryo atualizar o arquivo depois. Tente outra nuvem ou use a pasta do celular.",
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
            c.message("Este celular não tem o seletor de arquivos do Android.")
        }
    }
    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        canNotify = granted
        if (!granted) c.message("Sem permissão, o aviso não aparece. Você pode liberar nas configurações do Android.")
    }

    Scaffold(topBar = { CryoTopBar("Backup automático", onBack = nav::back) }, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { pad ->
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
                            "Uma cópia de tudo, sem você lembrar", style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Text(
                        "O Cryo salva seus dados sozinho, numa pasta do celular, na nuvem ou nos dois. " +
                            "Ele continua sem acesso à internet: quem envia o arquivo para a nuvem é o app da própria nuvem.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
            item {
                SwitchRow(
                    "Fazer backup automático", s.enabled,
                    { v ->
                        c.safely {
                            c.autoBackup.setEnabled(v)
                            runCatching { AutoBackupWork.sync(ctx, v && s.hasTarget) }
                        }
                    },
                    if (s.hasTarget) "Em segundo plano, mesmo com o app fechado" else "Escolha abaixo onde guardar",
                )
            }
            if (s.enabled && s.hasTarget && !canNotify && Build.VERSION.SDK_INT >= 33) {
                item {
                    Banner(
                        Icons.Rounded.NotificationsActive,
                        "Toque aqui e permita as notificações, para o Cryo avisar se um backup não for salvo.",
                        MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer,
                    ) { askNotify.launch(Manifest.permission.POST_NOTIFICATIONS) }
                }
            }
            item {
                FieldLabel("Com que frequência")
                Segmented(
                    BackupFrequency.entries.map { it to it.label }, s.frequency,
                    { f -> c.safely { c.autoBackup.setFrequency(f) } },
                )
                Text(
                    "Se nada mudou desde o último backup, o Cryo não grava outra cópia igual.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            item { SectionTitle("No celular") }
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

            item { SectionTitle("Na nuvem") }
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
                    Text(if (running) "Salvando..." else "Fazer backup agora")
                }
            }
            item {
                Text(
                    "Para voltar um backup, use Ajustes › Restaurar backup e escolha o arquivo. " +
                        "Se o celular economizar bateria de forma rígida, o backup pode atrasar um pouco; " +
                        "abrir o Cryo também faz o backup que estiver atrasado.",
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
                            c.message("Não consegui restaurar esse backup. Nada foi mudado.")
                        }
                    }
                },
                onKeep = ::keep,
                onCancel = ::cancel,
            )
            is PlaceCheck.CantRead -> ConfirmDialog(
                "Usar este lugar?",
                "${check.reason} Se continuar, ele será substituído pelos dados deste celular.",
                "Substituir", onConfirm = ::keep, onDismiss = ::cancel,
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
            Place(Icons.Rounded.Folder, "Nenhuma pasta escolhida", null)
            Text(
                "Guarde no próprio celular ou numa pasta que um app de nuvem sincroniza. " +
                    "Dica: na tela que abrir, crie uma pasta chamada Cryo (por exemplo, dentro de Documents) e toque em \"Usar esta pasta\".",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Button(onClick = onPick, modifier = Modifier.padding(top = 12.dp)) {
                Icon(Icons.Rounded.Folder, null); Spacer(Modifier.width(8.dp)); Text("Escolher pasta")
            }
            return@CryoCard
        }
        Place(Icons.Rounded.Folder, s.folder.name, s.folder)
        FieldLabel("Como guardar")
        ChoiceRow(
            "Substituir sempre o mesmo arquivo", "Fica só a cópia mais recente, no arquivo ${BackupPolicy.SINGLE_NAME}",
            s.folderMode == FolderMode.REPLACE,
        ) { onMode(FolderMode.REPLACE) }
        ChoiceRow(
            "Guardar várias cópias", "Um arquivo novo a cada backup, para poder voltar a uma data anterior",
            s.folderMode == FolderMode.COPIES,
        ) { onMode(FolderMode.COPIES) }
        if (s.folderMode == FolderMode.COPIES) {
            FieldLabel("Quantas cópias guardar")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5 to "5", 10 to "10", 30 to "30", 0 to "Todas").forEach { (n, label) ->
                    FilterChip(selected = s.keep == n, onClick = { c.safely { c.autoBackup.setKeep(n) } }, label = { Text(label) })
                }
            }
            Text(
                if (s.keep == 0) "Nenhuma cópia é apagada. De vez em quando, apague as antigas para liberar espaço."
                else "Quando passar de ${s.keep}, a cópia mais antiga feita por este celular é apagada sozinha. Os outros arquivos da pasta nunca são mexidos.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onPick) { Text("Trocar pasta") }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onRemove) { Text("Parar de usar") }
        }
    }
}

@Composable
private fun CloudCard(s: AutoBackupSettings, onPick: () -> Unit, onRemove: () -> Unit) {
    CryoCard {
        if (!s.cloud.isSet) {
            Place(Icons.Rounded.Cloud, "Nenhuma nuvem escolhida", null)
            Text(
                "Funciona com o Google Drive, OneDrive, Nextcloud e outras nuvens instaladas no celular.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            listOf(
                "Toque em \"Escolher na nuvem\".",
                "Na tela que abrir, toque em ☰ e escolha a nuvem.",
                "Escolha a pasta e toque em Salvar.",
            ).forEachIndexed { i, step ->
                Text(
                    "${i + 1}. $step", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp),
                )
            }
            Button(onClick = onPick, modifier = Modifier.padding(top = 12.dp)) {
                Icon(Icons.Rounded.Cloud, null); Spacer(Modifier.width(8.dp)); Text("Escolher na nuvem")
            }
            return@CryoCard
        }
        Place(Icons.Rounded.Cloud, s.cloud.name, s.cloud)
        Text(
            "A cada backup, o Cryo atualiza este mesmo arquivo. O envio pela internet é feito pelo app da nuvem.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onPick) { Text("Trocar") }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onRemove) { Text("Parar de usar") }
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
        t.lastError != null -> Triple("Não salvou ${backupWhen(t.lastErrorAt ?: now, now)}: ${t.lastError}", error, Icons.Rounded.ErrorOutline)
        t.lastAt != null -> Triple(
            "Último backup: ${backupWhen(t.lastAt, now)}" + if (t.checkedAt != null && t.checkedAt > t.lastAt) " · nada mudou desde então" else "",
            CryoTheme.colors.income, Icons.Rounded.CheckCircle,
        )
        else -> Triple("O primeiro backup ainda não foi feito", MaterialTheme.colorScheme.onSurfaceVariant, Icons.Rounded.Schedule)
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
    val keepLabel = if (overwrites) "Substituir pelos dados deste celular" else "Continuar sem restaurar"
    Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) {
            Icon(
                Icons.Rounded.SettingsBackupRestore, null, tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.align(Alignment.CenterHorizontally).size(28.dp),
            )
            Text(
                "Já existe um backup aqui", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )
            Text(
                (if (target == BackupTarget.FOLDER) "Esta pasta" else "Este arquivo") +
                    " já tem um backup do Cryo, com dados diferentes dos deste celular.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
            Compare(
                if (target == BackupTarget.FOLDER) Icons.Rounded.Folder else Icons.Rounded.Cloud,
                "No backup" + (found.exportedAt?.let { " · salvo ${backupWhen(it)}" } ?: ""),
                counts(found.txs, found.accounts), Modifier.padding(top = 16.dp),
            )
            Compare(
                Icons.Rounded.Smartphone, "Neste celular agora", counts(found.currentTxs, found.currentAccounts),
                Modifier.padding(top = 8.dp),
            )
            Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (found.richer) {
                    Button(onClick = onRestore, modifier = Modifier.fillMaxWidth()) { Text("Restaurar este backup") }
                    OutlinedButton(onClick = onKeep, modifier = Modifier.fillMaxWidth()) { Text(keepLabel, textAlign = TextAlign.Center) }
                } else {
                    Button(onClick = onKeep, modifier = Modifier.fillMaxWidth()) { Text(keepLabel, textAlign = TextAlign.Center) }
                    OutlinedButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) { Text("Restaurar este backup") }
                }
            }
            Text(
                "Restaurar troca os dados deste celular pelos do backup. " + when {
                    overwrites -> "Substituir troca o backup pelos dados deste celular."
                    replaceMode -> "Continuar mantém os outros backups da pasta, e o arquivo ${BackupPolicy.SINGLE_NAME} passa a ter os dados deste celular."
                    else -> "Os backups que já estão na pasta continuam lá."
                },
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
            TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.End).padding(top = 8.dp)) { Text("Cancelar") }
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
    "${plural(txs, "movimentação", "movimentações")} · ${plural(accounts, "conta", "contas")}"

private fun summary(r: List<BackupOutcome>): String {
    r.firstOrNull { it.status == BackupStatus.FAILED || it.status == BackupStatus.HELD }?.let { return it.message ?: "O backup falhou" }
    if (r.any { it.status == BackupStatus.NOTHING_TO_SAVE }) return "Ainda não há nada para salvar"
    val written = r.filter { it.status == BackupStatus.WRITTEN }.map { it.target }
    return when {
        written.containsAll(BackupTarget.entries) -> "Backup feito na pasta e na nuvem"
        BackupTarget.FOLDER in written -> "Backup feito na pasta"
        BackupTarget.CLOUD in written -> "Backup feito na nuvem"
        else -> "Nada para salvar agora"
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
    val where = if (t == BackupTarget.FOLDER) "na pasta" else "na nuvem"
    c.message(
        when (r?.status) {
            BackupStatus.WRITTEN -> when {
                restored -> "Backup restaurado. Daqui para frente, o Cryo salva $where."
                mode == FolderMode.REPLACE -> "Pronto! Agora o backup fica sempre no arquivo ${BackupPolicy.SINGLE_NAME}."
                else -> "Pronto! O primeiro backup já está $where."
            }
            BackupStatus.NOTHING_TO_SAVE -> "Lugar escolhido. O primeiro backup sai quando você tiver algo cadastrado."
            BackupStatus.FAILED, BackupStatus.HELD -> r.message ?: "Não consegui salvar nesse lugar"
            else -> if (restored) "Backup restaurado" else "Lugar escolhido"
        },
    )
}

private fun remove(c: AppContainer, ctx: Context, t: BackupTarget, uri: String?) {
    c.safely {
        BackupPlaces.releaseAccess(ctx, uri)
        c.autoBackup.setTarget(t, null, "")
        val left = c.autoBackup.current()
        runCatching { AutoBackupWork.sync(ctx, left.enabled && left.hasTarget) }
        c.message(if (t == BackupTarget.FOLDER) "A pasta não será mais usada. Os arquivos que já estão lá continuam." else "A nuvem não será mais usada. O arquivo que já está lá continua.")
    }
}
