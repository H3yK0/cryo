// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.data.Backup
import io.github.h3yk0.cryo.data.BackupCrypto
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.ui.components.CryoButton
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/*
 * RF-SEG-03/04: as telas da proteção dos backups. A senha só existe na memória enquanto a folha está aberta: o que fica
 * guardado é a chave derivada, trancada pelo cofre do Android.
 */

/** Mínimo de caracteres da senha dos backups. */
const val MIN_PASSWORD = 8

/** Ligar a proteção: a senha duas vezes e o aviso de que, sem ela, não há como abrir os backups. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtectBackupSheet(busy: Boolean, onDismiss: () -> Unit, onConfirm: (CharArray) -> Unit) {
    var a by remember { mutableStateOf("") }
    var b by remember { mutableStateOf("") }
    val error = when {
        a.isNotEmpty() && a.length < MIN_PASSWORD -> Res.s(R.string.sec_password_short, MIN_PASSWORD)
        b.isNotEmpty() && a != b -> Res.s(R.string.sec_password_mismatch)
        else -> null
    }
    ModalBottomSheet(onDismissRequest = { if (!busy) onDismiss() }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding().imePadding()) {
            Text(Res.s(R.string.sec_protect_sheet_title), style = MaterialTheme.typography.titleLarge)
            Text(
                Res.s(R.string.sec_protect_warning), style = MaterialTheme.typography.bodyMedium,
                color = CryoTheme.colors.onWarningContainer, modifier = Modifier.padding(vertical = 8.dp),
            )
            PasswordField(a, { a = it }, Res.s(R.string.sec_password))
            Spacer(Modifier.height(8.dp))
            PasswordField(b, { b = it }, Res.s(R.string.sec_password_again))
            error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = CryoTheme.colors.expense, modifier = Modifier.padding(top = 6.dp)) }
            Spacer(Modifier.height(16.dp))
            CryoButton(
                onClick = { onConfirm(a.toCharArray()) },
                enabled = !busy && error == null && a.length >= MIN_PASSWORD && a == b,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(if (busy) Res.s(R.string.sec_protecting) else Res.s(R.string.sec_protect_confirm)) }
            TextButton(onClick = onDismiss, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(Res.s(R.string.wallet_cancelar)) }
        }
    }
}

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value, { onChange(it.take(128)) }, label = { Text(label) }, singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Abrir um backup protegido: pede a senha (e diz quando está errada). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupPasswordSheet(error: String?, busy: Boolean, onDismiss: () -> Unit, onSubmit: (CharArray) -> Unit) {
    var pw by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = { if (!busy) onDismiss() }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding().imePadding()) {
            Text(Res.s(R.string.sec_open_title), style = MaterialTheme.typography.titleLarge)
            Text(Res.s(R.string.sec_open_help), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
            PasswordField(pw, { pw = it }, Res.s(R.string.sec_password))
            error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = CryoTheme.colors.expense, modifier = Modifier.padding(top = 6.dp)) }
            Spacer(Modifier.height(16.dp))
            CryoButton(onClick = { onSubmit(pw.toCharArray()) }, enabled = !busy && pw.isNotEmpty(), modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(if (busy) Res.s(R.string.sec_opening) else Res.s(R.string.sec_open_button))
            }
            TextButton(onClick = onDismiss, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(Res.s(R.string.wallet_cancelar)) }
        }
    }
}

/**
 * RF-SEG-04: abre o texto de um backup. Aberto, segue direto; protegido, tenta a chave deste aparelho e, se não servir,
 * pede a senha. [onOpened] recebe o backup já lido (quem chama confere o modo de teste e confirma a restauração).
 */
@Composable
fun rememberBackupOpener(onOpened: (Backup.Restored) -> Unit): (String) -> Unit {
    val c = LocalContainer.current
    var locked by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    locked?.let { text ->
        BackupPasswordSheet(error, busy, onDismiss = { locked = null; error = null }) { pw ->
            busy = true
            error = null
            c.launch {
                val r = runCatching { withContext(Dispatchers.Default) { Backup.import(BackupCrypto.decrypt(text, pw)) } }
                pw.fill(' ')
                withContext(Dispatchers.Main) {
                    busy = false
                    r.onSuccess { locked = null; onOpened(it) }
                        .onFailure { error = it.message ?: Res.s(R.string.sec_wrong_password) }
                }
            }
        }
    }
    return { text ->
        // Ler um backup grande leva alguns segundos: fora da linha principal, para a tela não travar.
        c.launch {
            val r = runCatching {
                withContext(Dispatchers.Default) {
                    if (BackupCrypto.isEncrypted(text)) {
                        val key = c.backupProtection.key()
                        try {
                            key?.let { BackupCrypto.decryptWithKey(text, it) }?.let { Backup.import(it) } // null = pede a senha
                        } finally {
                            key?.bytes?.fill(0)
                        }
                    } else Backup.import(text)
                }
            }
            withContext(Dispatchers.Main) {
                r.onSuccess { if (it != null) onOpened(it) else { error = null; locked = text } }
                    .onFailure { c.message(it.message ?: Res.s(R.string.backup_invalid_file)) }
            }
        }
    }
}

/** O texto de um backup escolhido pela pessoa, ou uma mensagem que ela entende (não abriu, grande demais). */
suspend fun readBackupFile(ctx: android.content.Context, uri: android.net.Uri): String = withContext(Dispatchers.IO) {
    runCatching { io.github.h3yk0.cryo.data.SafFiles.read(ctx.contentResolver, uri) }.getOrNull()
        ?: throw IllegalArgumentException(Res.s(R.string.settings_nao_consegui_ler_arquivo))
}
