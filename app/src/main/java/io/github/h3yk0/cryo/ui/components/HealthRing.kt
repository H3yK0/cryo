// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * RF-INI-18 (1.6, pedido do Hanry, LA1-1): a saúde financeira ao lado do saldo, no cabeçalho do Início. Um anel com a nota
 * (0 a 100) na cor da faixa e a palavra da faixa embaixo; tocar abre a Saúde financeira. Não é dinheiro: aparece também
 * com "esconder valores". O anel cresce até a nota ao aparecer (DS-MOV), e aparece cheio com "Remover animações".
 */
@Composable
fun HealthRing(score: Int, color: Color, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 60.dp) {
    val on = LocalContentColor.current
    val grow = rememberAppear()
    val cd = io.github.h3yk0.cryo.domain.Res.s(io.github.h3yk0.cryo.R.string.home_health_cd, score, label)
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .clearAndSetSemantics { contentDescription = cd; role = Role.Button }
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(size)) {
                val stroke = this.size.minDimension * 0.11f
                val inset = stroke / 2
                val arcSize = androidx.compose.ui.geometry.Size(this.size.width - stroke, this.size.height - stroke)
                val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
                drawArc(on.copy(alpha = 0.22f), -90f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                val sweep = 360f * (score.coerceIn(0, 100) / 100f) * grow.value
                drawArc(color, -90f, sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Text(score.toString(), style = MaterialTheme.typography.titleMedium, color = on)
        }
        Text(
            label, style = MaterialTheme.typography.labelSmall, color = on.copy(alpha = 0.9f), maxLines = 1,
            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = size + 24.dp),
        )
    }
}
