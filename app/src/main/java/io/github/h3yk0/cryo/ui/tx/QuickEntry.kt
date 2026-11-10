// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.tx

import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.Res
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import io.github.h3yk0.cryo.data.AppSettings
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.ParseContext
import io.github.h3yk0.cryo.domain.PhraseParser
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.components.CryoButton

fun Ledger.parseContext(settings: AppSettings) = ParseContext(
    today = today, accounts = s.accounts, cards = s.cards, categories = s.categories,
    investments = s.investments, goals = s.goals, learned = learnedCategories,
    defaultAccountId = settings.defaultAccountId, debts = s.debts,
    categoryGuess = { kind, desc ->
        categoryModel.guess(if (kind == io.github.h3yk0.cryo.data.db.CategoryKind.INCOME) io.github.h3yk0.cryo.data.db.TxType.INCOME else io.github.h3yk0.cryo.data.db.TxType.EXPENSE, desc)?.categoryId
    },
)

/**
 * Barra de "registro rápido": a pessoa escreve como falaria com alguém ("almoço 25 no débito") e o app monta o lançamento
 * para ela só conferir. Para falar, o botão do meio da barra de baixo (RF-ALC-01).
 */
@Composable
fun QuickEntryBar(l: Ledger, modifier: Modifier = Modifier) {
    val c = LocalContainer.current
    val settings = LocalSettings.current
    val focus = LocalFocusManager.current
    var text by rememberSaveable { mutableStateOf("") }
    var draft by remember { mutableStateOf<TxDraft?>(null) }

    fun submit(t: String) {
        if (t.isBlank()) return
        draft = TxDraft.fromParsed(PhraseParser(l.parseContext(settings)).parse(t))
        focus.clearFocus()
    }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, end = 6.dp)) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(Res.s(R.string.quick_ex_almoco_25_debito)) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { submit(text) }),
                modifier = Modifier.weight(1f),
            )
            // RF-ALC-02: aqui só se escreve; a voz fica no botão do meio da barra de baixo, ao alcance do polegar.
            FilledIconButton(onClick = { submit(text) }, enabled = text.isNotBlank()) { Icon(Icons.AutoMirrored.Rounded.Send, Res.s(R.string.debt_registrar)) }
        }
    }

    draft?.let { d ->
        QuickConfirmSheet(d, l, onDismiss = { draft = null }, onSaved = { draft = null; text = "" })
    }
}

/** RF-VOZ-02: antes de o Android perguntar, o Cryo explica para que é o microfone e que o áudio não sai do celular. */
@Composable
fun MicPermissionSheet(onDismiss: () -> Unit, onAllow: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Icon(Icons.Rounded.Mic, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(12.dp))
            Text(Res.s(R.string.voice_mic_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(Res.s(R.string.voice_mic_text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            CryoButton(onClick = onAllow, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text(Res.s(R.string.voice_mic_allow)) }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(Res.s(R.string.wallet_cancelar)) }
        }
    }
}

/** Folha de conferência: mostra o que o app entendeu, com tudo editável, e salva com um toque. */
@Composable
fun QuickConfirmSheet(d: TxDraft, l: Ledger, onDismiss: () -> Unit, onSaved: () -> Unit, title: String = Res.s(R.string.quick_confira_lancamento)) {
    val c = LocalContainer.current
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(
                Res.s(R.string.quick_foi_isso_que_eu),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            TxForm(d, l, compact = true)
            Spacer(Modifier.height(20.dp))
            val problem = d.problem()
            if (problem != null) {
                Text(problem, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
            }
            CryoButton(
                onClick = {
                    c.launch {
                        val r = saveDraft(d, c.repo, l)
                        c.afterSave(r, income = d.type == io.github.h3yk0.cryo.data.db.TxType.INCOME)
                    }
                    onSaved()
                },
                enabled = problem == null,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Icon(Icons.Rounded.Check, null)
                Spacer(Modifier.width(8.dp))
                Text(Res.s(R.string.wallet_salvar))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(Res.s(R.string.wallet_cancelar)) }
        }
    }
}

@Composable
fun QuickEntryHint(modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            Res.s(R.string.quick_escreva_ou_fale_seu),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
