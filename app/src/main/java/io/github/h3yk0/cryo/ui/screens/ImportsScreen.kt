// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.Dates
import io.github.h3yk0.cryo.domain.ImportBatch
import io.github.h3yk0.cryo.domain.ImportBatches
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.components.ConfirmDialog
import io.github.h3yk0.cryo.ui.components.CryoTopBar
import io.github.h3yk0.cryo.ui.components.EmptyState
import io.github.h3yk0.cryo.ui.components.IconBadge
import io.github.h3yk0.cryo.ui.tx.LoadingBox
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.MoneyText
import io.github.h3yk0.cryo.ui.components.depthCard
import io.github.h3yk0.cryo.ui.components.rememberLedger
import java.time.Instant
import java.time.ZoneId

/**
 * RF-IMP-23 (TELA-IMPORTACOES): as importações já feitas, com o que cada uma trouxe e um jeito de apagar a importação
 * inteira. Apagar tira só o que veio do arquivo: o que a pessoa lançou à mão continua. Sempre com desfazer.
 */
@Composable
fun ImportsScreen() {
    val l = rememberLedger() ?: return LoadingBox()
    val c = LocalContainer.current
    val nav = LocalNav.current
    val batches = remember(l) { ImportBatches.of(l.s.txs) }
    var confirm by remember { mutableStateOf<ImportBatch?>(null) }

    Scaffold(topBar = { CryoTopBar(Res.s(R.string.imports_title), onBack = nav::back) }, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (batches.isEmpty()) {
                item { EmptyState(Icons.Rounded.UploadFile, Res.s(R.string.imports_empty), Res.s(R.string.settings_ofx_csv_planilha_ou)) }
            } else {
                item {
                    Text(
                        Res.s(R.string.imports_hint), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
            }
            items(batches.size, key = { batches[it].scope + batches[it].createdAt }) { i ->
                val b = batches[i]
                val where = b.accountId?.let { id -> l.account[id]?.name } ?: b.cardId?.let { id -> l.cardName(id) } ?: ""
                val day = Instant.ofEpochMilli(b.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
                Row(Modifier.fillMaxWidth().depthCard().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Rounded.UploadFile, MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(where, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            Res.p(R.plurals.imports_count, b.count, b.count) + " · " + Res.s(R.string.imports_when, Dates.short(day)),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            Res.s(R.string.imports_period, Dates.short(b.first), Dates.short(b.last)),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        MoneyText(b.net, style = MaterialTheme.typography.bodyMedium)
                    }
                    IconButton(onClick = { confirm = b }) {
                        Icon(Icons.Rounded.Delete, Res.s(R.string.imports_delete_q), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
    confirm?.let { b ->
        ConfirmDialog(
            Res.s(R.string.imports_delete_q),
            Res.p(R.plurals.imports_count, b.count, b.count) + " · " + Res.s(R.string.imports_hint),
            Res.s(R.string.wallet_apagar),
            onConfirm = {
                confirm = null
                val apagados = b.txs
                c.launch {
                    c.repo.undoImport(apagados.map { it.id })
                    c.message(Res.s(R.string.imports_deleted), Res.s(R.string.wallet_desfazer)) { c.repo.importTxs(apagados) }
                }
            },
            onDismiss = { confirm = null },
        )
    }
    Spacer(Modifier.height(0.dp))
}
