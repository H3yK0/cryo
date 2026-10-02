// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.tx

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

fun Ledger.parseContext(settings: AppSettings) = ParseContext(
    today = today, accounts = s.accounts, cards = s.cards, categories = s.categories,
    investments = s.investments, goals = s.goals, learned = learnedCategories,
    defaultAccountId = settings.defaultAccountId, debts = s.debts,
)

/**
 * Barra de "registro rápido": a pessoa digita ou fala como falaria com alguém
 * ("almoço 25 no débito") e o app monta o lançamento para ela só conferir.
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

    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val spoken = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                text = spoken
                submit(spoken)
            }
        }
    }

    fun listen() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale, por exemplo: mercado 120 no débito")
        }
        try {
            voice.launch(intent)
        } catch (_: ActivityNotFoundException) {
            c.message("Este aparelho não tem reconhecimento de voz disponível")
        }
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
                placeholder = { Text("Ex.: almoço 25 no débito") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { submit(text) }),
                modifier = Modifier.weight(1f),
            )
            if (text.isBlank()) {
                FilledIconButton(onClick = { listen() }) { Icon(Icons.Rounded.Mic, "Registrar falando") }
            } else {
                FilledIconButton(onClick = { submit(text) }) { Icon(Icons.AutoMirrored.Rounded.Send, "Registrar") }
            }
        }
    }

    draft?.let { d ->
        QuickConfirmSheet(d, l, onDismiss = { draft = null }, onSaved = { draft = null; text = "" })
    }
}

/** Folha de conferência: mostra o que o app entendeu, com tudo editável, e salva com um toque. */
@Composable
fun QuickConfirmSheet(d: TxDraft, l: Ledger, onDismiss: () -> Unit, onSaved: () -> Unit, title: String = "Confira o lançamento") {
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
                "Foi isso que eu entendi. Ajuste o que precisar e salve.",
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
            Button(
                onClick = {
                    c.launch {
                        val r = saveDraft(d, c.repo, l)
                        c.message(r.alert ?: r.summary, "Desfazer") { r.undo() }
                    }
                    onSaved()
                },
                enabled = problem == null,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Icon(Icons.Rounded.Check, null)
                Spacer(Modifier.width(8.dp))
                Text("Salvar")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Cancelar") }
        }
    }
}

@Composable
fun QuickEntryHint(modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            "Escreva ou fale do seu jeito: “recebi 3000 de salário”, “tv 1200 em 10x no cartão”.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun SpeakButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) { Icon(Icons.Rounded.Mic, "Falar") }
}
