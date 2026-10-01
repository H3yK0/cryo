// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.tx

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.ui.components.CryoTopBar
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.components.Source
import io.github.h3yk0.cryo.ui.components.rememberLedger

@Composable
fun LoadingBox() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

/** Tela completa de lançamento (novo ou edição). */
@Composable
fun TxEditorScreen(
    txId: Long?,
    initialType: TxType,
    presetAccountId: Long?,
    presetCardId: Long?,
    onDone: () -> Unit,
) {
    val l = rememberLedger() ?: return LoadingBox()
    val c = LocalContainer.current
    val settings = LocalSettings.current
    val draft = remember(txId) {
        val o = txId?.let { id -> l.s.txs.firstOrNull { it.id == id } }
        if (o != null) {
            TxDraft.fromTx(o)
        } else {
            val acc = presetAccountId ?: settings.defaultAccountId?.takeIf { l.account[it]?.archived == false }
                ?: l.activeAccounts.firstOrNull()?.id
            TxDraft(
                type = initialType, date = l.today,
                source = presetCardId?.let { Source.Card(it) } ?: acc?.let { Source.Acc(it) },
            )
        }
    }
    var askDelete by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CryoTopBar(
                title = if (draft.isEditing) "Editar lançamento" else "Novo lançamento",
                onBack = onDone,
                actions = {
                    if (draft.isEditing) IconButton(onClick = { askDelete = true }) { Icon(Icons.Rounded.Delete, "Apagar") }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(16.dp)) {
                    val problem = draft.problem()
                    if (problem != null) {
                        Text(problem, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                    }
                    Button(
                        onClick = {
                            c.launch {
                                val r = saveDraft(draft, c.repo, l)
                                c.message(r.alert ?: r.summary, "Desfazer") { r.undo() }
                            }
                            onDone()
                        },
                        enabled = problem == null,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) {
                        Icon(Icons.Rounded.Check, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (draft.isEditing) "Salvar alterações" else "Salvar")
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            TxForm(draft, l, compact = false, autoFocusAmount = !draft.isEditing)
            Spacer(Modifier.height(24.dp))
        }
    }

    if (askDelete) {
        val o = draft.original!!
        val group = o.installmentGroup?.let { g -> l.s.txs.filter { it.installmentGroup == g } }.orEmpty()
        AlertDialog(
            onDismissRequest = { askDelete = false },
            title = { Text("Apagar lançamento?") },
            text = {
                Text(
                    when {
                        group.size > 1 -> "Esta é a parcela ${o.installmentNumber} de ${o.installmentTotal}. Você pode apagar só ela ou a compra inteira."
                        o.billId != null -> "Ele está ligado a uma conta fixa, que voltará a aparecer como não paga neste mês."
                        else -> "Você poderá desfazer logo em seguida."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    askDelete = false
                    c.launch {
                        c.repo.deleteTx(o, wholeGroup = group.size > 1)
                        c.message(if (group.size > 1) "Compra parcelada apagada" else "Lançamento apagado", "Desfazer") {
                            c.repo.restore(if (group.size > 1) group else listOf(o))
                        }
                    }
                    onDone()
                }) { Text(if (group.size > 1) "Apagar todas" else "Apagar") }
            },
            dismissButton = {
                if (group.size > 1) {
                    TextButton(onClick = {
                        askDelete = false
                        c.launch {
                            c.repo.deleteTx(o)
                            c.message("Parcela apagada", "Desfazer") { c.repo.restore(listOf(o)) }
                        }
                        onDone()
                    }) { Text("Só esta") }
                } else {
                    TextButton(onClick = { askDelete = false }) { Text("Cancelar") }
                }
            },
        )
    }
}
