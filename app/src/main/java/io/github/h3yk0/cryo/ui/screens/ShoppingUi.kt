// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.data.SampleData
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.notify.Reminders
import io.github.h3yk0.cryo.notify.Shopping
import io.github.h3yk0.cryo.ui.components.AmountField
import io.github.h3yk0.cryo.ui.components.Art
import io.github.h3yk0.cryo.ui.components.ArtImage
import io.github.h3yk0.cryo.ui.components.CryoButton
import io.github.h3yk0.cryo.ui.components.UsageBar
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.depthCard
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import java.time.LocalDate

/**
 * RF-AVI-10: o modo compras no Início. Enquanto a sessão dura, um cartão com o que já foi gasto desde que começou (só os
 * dados de verdade: a notificação "ao vivo" acompanha o mesmo número).
 */
@Composable
fun ShoppingBanner(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val session by Shopping.session.collectAsState()
    val s = session ?: return
    // Só aparece fora do modo de teste: o container das telas é o de verdade.
    val snap by LocalContainer.current.repo.snapshot.collectAsState()
    val spent = snap?.let { s.spent(it, LocalDate.now()) } ?: 0L
    val left = s.cap - spent
    Column(modifier.fillMaxWidth().depthCard().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ArtImage(Art.SHOPPING, 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(Res.s(R.string.shop_title, Money.format(spent), Money.format(s.cap)), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    if (left >= 0) Res.s(R.string.shop_left, Money.format(left)) else Res.s(R.string.shop_over, Money.format(-left)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (left >= 0) MaterialTheme.colorScheme.onSurfaceVariant else CryoTheme.colors.expense,
                )
            }
            TextButton(onClick = { Shopping.stop(ctx) }) { Text(Res.s(R.string.shop_end)) }
        }
        Spacer(Modifier.height(10.dp))
        UsageBar(spent.toFloat() / s.cap.coerceAtLeast(1))
    }
}

/** "Vou às compras": o teto da ida e o começo da sessão (com a notificação ao vivo). */
@Composable
fun ShoppingStartSheet(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val c = LocalContainer.current
    var cap by remember { mutableLongStateOf(SampleData.m(30000)) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.imePadding().padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            ArtImage(Art.SHOPPING, 64.dp)
            Spacer(Modifier.height(14.dp))
            Text(Res.s(R.string.shop_start_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(Res.s(R.string.shop_start_text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            AmountField(cap, { cap = it }, label = Res.s(R.string.shop_start_cap))
            Spacer(Modifier.height(16.dp))
            CryoButton(
                onClick = {
                    onDismiss()
                    Shopping.start(ctx, cap, c.repo.snapshot.value)
                    c.guideCount(io.github.h3yk0.cryo.domain.GuideCount.SHOPPING)
                    if (Build.VERSION.SDK_INT >= 33 && !Reminders.canNotify(ctx)) ask.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
                enabled = cap > 0, modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Text(Res.s(R.string.shop_start_button)) }
        }
    }
}
