// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.tx

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.Dates
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.domain.ParseContext
import io.github.h3yk0.cryo.domain.ParsedEntry
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.domain.Spoken
import io.github.h3yk0.cryo.ui.components.CryoButton
import io.github.h3yk0.cryo.ui.components.IconBadge
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.components.animationsOn
import io.github.h3yk0.cryo.ui.components.categoryIcon
import io.github.h3yk0.cryo.ui.components.depthCard
import io.github.h3yk0.cryo.ui.components.raised
import io.github.h3yk0.cryo.ui.components.rememberAppear
import io.github.h3yk0.cryo.ui.components.rememberHaptics
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import io.github.h3yk0.cryo.voice.Listener
import io.github.h3yk0.cryo.voice.MicInput
import io.github.h3yk0.cryo.voice.SystemListener
import io.github.h3yk0.cryo.voice.VoiceEngineKind
import io.github.h3yk0.cryo.voice.VoicePhase
import io.github.h3yk0.cryo.voice.VoiceSettings
import io.github.h3yk0.cryo.notify.Sound
import io.github.h3yk0.cryo.notify.Sounds
import io.github.h3yk0.cryo.voice.VoiceProblem
import io.github.h3yk0.cryo.voice.VoicePrompt
import io.github.h3yk0.cryo.voice.VoiceUi
import io.github.h3yk0.cryo.voice.WhisperEngine
import io.github.h3yk0.cryo.voice.WhisperListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/*
 * RF-VOZ-03/08: a tela de "ouvindo", com o eco aprovado pelo Hanry (protótipo de 09/11/2026). O microfone respira com a
 * voz, cada sílaba solta um anel, cada palavra entra subindo com um brilho, e o que o Cryo entende vira um cartãozinho
 * que salta. Toda animação é finita (anéis só enquanto há voz, DS-MOV-10) e some com "Remover animações".
 */

/**
 * A folha de voz. [start] = qual ouvido tentar primeiro; se o Whisper não abrir, cai sozinha no reconhecedor do celular
 * (só no aparelho) e, sem ele, em [onLegacy] (a tela de voz do sistema, como na 1.5). [listeners] troca os ouvidos nos testes.
 */
@Composable
fun VoiceSheet(
    l: Ledger,
    start: VoiceEngineKind,
    onDismiss: () -> Unit,
    onLegacy: () -> Unit,
    listeners: ((VoiceEngineKind, ParseContext) -> Listener)? = null,
) {
    val c = LocalContainer.current
    val settings = LocalSettings.current
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val pctx = remember(l, settings) { l.parseContext(settings) }
    var kind by remember { mutableStateOf(start) }
    var round by remember { mutableIntStateOf(0) }
    // Escopo próprio, que não é cancelado com a tela: a escuta sempre termina sozinha e solta o microfone e o modelo.
    val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
    val listener = remember(kind, round) {
        listeners?.invoke(kind, pctx) ?: when (kind) {
            VoiceEngineKind.WHISPER -> WhisperListener(scope, MicInput(), {
                WhisperEngine.open(context.assets, pctx.language, VoicePrompt.build(pctx), flash = c.dev.enabled && VoiceSettings(context).fastAttention)
            })
            VoiceEngineKind.PHONE -> SystemListener(context)
        }
    }
    DisposableEffect(listener) {
        listener.start()
        onDispose { listener.cancel() }
    }
    val ui by listener.ui.collectAsState()

    // Quando não deu: o Whisper que não abriu passa a vez ao reconhecedor do celular; sem ele, a tela de voz do sistema.
    LaunchedEffect(ui.phase, ui.problem) {
        if (ui.phase != VoicePhase.FAILED) return@LaunchedEffect
        when {
            ui.problem == VoiceProblem.ENGINE && kind == VoiceEngineKind.WHISPER && SystemListener.available(context) -> kind = VoiceEngineKind.PHONE
            ui.problem == VoiceProblem.ENGINE -> onLegacy()
            else -> { c.message(Res.s(R.string.voice_mic_busy)); onDismiss() }
        }
    }

    // RF-SOM: um "pode falar" curto quando o microfone abre e um "pronto" quando o texto chega.
    LaunchedEffect(ui.phase) {
        when (ui.phase) {
            VoicePhase.LISTENING -> Sounds.play(context, Sound.VOICE_START)
            VoicePhase.DONE -> if (ui.text.isNotBlank()) Sounds.play(context, Sound.VOICE_END)
            else -> Unit
        }
    }
    val devMode = remember { c.dev.enabled }

    val entries = remember(ui.text) { if (ui.text.isBlank()) emptyList() else Spoken.entries(ui.text, pctx) }
    val done = ui.phase == VoicePhase.DONE
    val drafts = remember(done, ui.text) { mutableStateListOf<TxDraft>().apply { if (done) addAll(entries.map { TxDraft.fromParsed(it) }) } }
    LaunchedEffect(done) { if (done && drafts.isNotEmpty()) haptics.confirm() }
    var adjusting by remember { mutableStateOf<TxDraft?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 20.dp).navigationBarsPadding()) {
            StatusRow(ui, kind, hasResult = drafts.isNotEmpty(), devMs = if (devMode) ui.finalMs else null)
            Words(ui)
            Echo(ui, done && drafts.isNotEmpty(), onTap = { if (ui.phase == VoicePhase.LISTENING) listener.finish() })
            if (done && drafts.size > 1) {
                drafts.forEachIndexed { i, d -> EntryRow(entries.getOrNull(i), d, pctx, onClick = { adjusting = d }) }
            } else {
                Chips(entries.firstOrNull(), pctx)
            }
            Spacer(Modifier.height(16.dp))
            when {
                done && drafts.isEmpty() -> {
                    Text(Res.s(R.string.voice_nothing), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    CryoButton(onClick = { round++ }, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text(Res.s(R.string.voice_again)) }
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(Res.s(R.string.wallet_cancelar)) }
                }
                done -> {
                    val ok = drafts.all { it.problem() == null }
                    if (!ok) {
                        Text(Res.s(R.string.voice_check), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                    }
                    val total = Money.format(drafts.sumOf { it.amount })
                    CryoButton(
                        onClick = {
                            val list = drafts.toList()
                            c.launch {
                                val results = list.map { saveDraft(it, c.repo, l) }
                                if (results.size == 1) {
                                    c.afterSave(results[0], income = list[0].type == io.github.h3yk0.cryo.data.db.TxType.INCOME)
                                } else {
                                    results.firstNotNullOfOrNull { it.moment }?.let(c.moments::show)
                                        ?: Sounds.play(c.app, Sound.SAVE)
                                    val msg = results.firstNotNullOfOrNull { it.alert } ?: Res.p(R.plurals.voice_saved_many, results.size, results.size)
                                    c.message(msg, Res.s(R.string.wallet_desfazer)) { results.asReversed().forEach { it.undo() } }
                                }
                            }
                            onDismiss()
                        },
                        enabled = ok,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) {
                        Text(if (drafts.size == 1) Res.s(R.string.voice_save, total) else Res.p(R.plurals.voice_save_many, drafts.size, drafts.size, total))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { adjusting = drafts.firstOrNull { it.problem() != null } ?: drafts.first() }, modifier = Modifier.weight(1f).height(48.dp)) {
                            Text(Res.s(R.string.voice_adjust))
                        }
                        TextButton(onClick = { round++ }, modifier = Modifier.weight(1f).height(48.dp)) { Text(Res.s(R.string.voice_again)) }
                    }
                }
                else -> {
                    CryoButton(
                        onClick = { listener.finish() }, enabled = ui.phase == VoicePhase.LISTENING,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) { Text(Res.s(R.string.voice_finish)) }
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(Res.s(R.string.wallet_cancelar)) }
                }
            }
        }
    }

    adjusting?.let { d ->
        QuickConfirmSheet(d, l, onDismiss = { adjusting = null }, onSaved = {
            drafts.remove(d)
            adjusting = null
            if (drafts.isEmpty()) onDismiss()
        })
    }
}

@Composable
private fun StatusRow(ui: VoiceUi, kind: VoiceEngineKind, hasResult: Boolean, devMs: Long? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        val done = ui.phase == VoicePhase.DONE && hasResult
        if (done) Icon(Icons.Rounded.CheckCircle, null, tint = CryoTheme.colors.income, modifier = Modifier.size(18.dp))
        else Box(Modifier.size(10.dp).clip(CircleShape).background(if (ui.phase == VoicePhase.LISTENING) CryoTheme.colors.expense else MaterialTheme.colorScheme.outline))
        Spacer(Modifier.width(8.dp))
        Text(
            when (ui.phase) {
                VoicePhase.STARTING -> Res.s(R.string.voice_preparing)
                VoicePhase.LISTENING -> Res.s(R.string.voice_listening)
                VoicePhase.THINKING -> Res.s(R.string.voice_thinking)
                VoicePhase.DONE, VoicePhase.FAILED -> if (done) Res.s(R.string.voice_done) else ""
            },
            style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        // RF-VOZ-13: no modo desenvolvedor, quanto tempo levou do fim da fala até o texto.
        if (devMs != null) Text(
            Res.s(R.string.voice_dev_ms, devMs.toInt()), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            Res.s(if (kind == VoiceEngineKind.WHISPER) R.string.voice_engine_whisper else R.string.voice_engine_phone),
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** O texto falado, palavra por palavra: cada palavra nova entra subindo, com um brilho que se apaga. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Words(ui: VoiceUi) {
    val words = ui.text.split(' ').filter { it.isNotBlank() }
    FlowRow(
        Modifier.fillMaxWidth().heightIn(min = 96.dp).padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (words.isEmpty()) {
            if (ui.phase == VoicePhase.LISTENING || ui.phase == VoicePhase.STARTING) {
                Text(Res.s(R.string.voice_hint), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            words.forEachIndexed { i, w -> key(i, w) { Word(w) } }
        }
    }
}

@Composable
private fun Word(w: String) {
    val k by rememberAppear(450)
    val primary = MaterialTheme.colorScheme.primary
    Text(
        w, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold,
        color = lerp(primary, MaterialTheme.colorScheme.onSurface, k),
        modifier = Modifier.graphicsLayer { translationY = (1 - k) * 26f; alpha = (k * 3).coerceAtMost(1f) },
    )
}

private class Ring(val strength: Float) { val t = Animatable(0f) }

/** O eco: anéis que saem do microfone a cada sílaba, e o microfone que cresce com o volume da voz. */
@Composable
private fun Echo(ui: VoiceUi, success: Boolean, onTap: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val anim = animationsOn()
    val rings = remember { mutableStateListOf<Ring>() }
    val ringScope = rememberCoroutineScope()
    LaunchedEffect(ui.syllables) {
        if (!anim || ui.syllables == 0 || ui.phase != VoicePhase.LISTENING) return@LaunchedEffect
        val r = Ring(ui.strength)
        rings += r
        ringScope.launch {
            r.t.animateTo(1f, tween(1100, easing = LinearOutSlowInEasing))
            rings.remove(r)
        }
    }
    val lv = if (ui.phase == VoicePhase.LISTENING) ui.level else 0f
    // Depois de "Entendi" o eco encolhe e dá lugar ao que foi entendido.
    val h by animateDpAsState(if (ui.phase == VoicePhase.DONE) 170.dp else 260.dp, tween(if (anim) 400 else 0), label = "echo")
    Box(Modifier.fillMaxWidth().height(h), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val r0 = 48.dp.toPx()
            val rMax = size.minDimension / 2
            val glow = r0 * (1.6f + 1.4f * lv)
            drawCircle(Brush.radialGradient(listOf(primary.copy(alpha = 0.10f + 0.22f * lv), Color.Transparent), center, glow), glow, center)
            for (ring in rings) {
                val x = ring.t.value
                val e = 1 - (1 - x) * (1 - x) * (1 - x)
                val a = (1 - x) * (1 - x) * (0.20f + 0.55f * ring.strength)
                drawCircle(primary.copy(alpha = a), r0 + (rMax - r0) * e, center, style = Stroke((1.5f + 5f * ring.strength * (1 - x)).dp.toPx()))
            }
        }
        val desc = Res.s(R.string.voice_mic_desc)
        Box(
            Modifier.size(96.dp).scale(1f + 0.14f * lv)
                .raised(CircleShape, if (success) CryoTheme.colors.income else primary, shadowColor = primary.copy(alpha = 0.45f), glow = (14 + 18 * lv).dp)
                .clip(CircleShape).clickable(onClick = onTap).semantics { contentDescription = desc },
            contentAlignment = Alignment.Center,
        ) { Icon(if (success) Icons.Rounded.CheckCircle else Icons.Rounded.Mic, null, tint = Color.White, modifier = Modifier.size(40.dp)) }
    }
}

private data class ChipData(val icon: ImageVector, val label: String)

private fun chips(e: ParsedEntry?, ctx: ParseContext): List<ChipData> = buildList {
    if (e == null) return@buildList
    e.amount?.let { add(ChipData(Icons.Rounded.Payments, Money.format(it))) }
    e.categoryId?.let { id -> ctx.categories.firstOrNull { it.id == id }?.let { add(ChipData(categoryIcon(it.icon), it.name)) } }
    val card = e.cardId?.let { id -> ctx.cards.firstOrNull { it.id == id } }
    if (card != null) add(ChipData(Icons.Rounded.CreditCard, card.name))
    else e.accountId?.let { id -> ctx.accounts.firstOrNull { it.id == id } }?.let { add(ChipData(Icons.Rounded.AccountBalanceWallet, it.name)) }
    if (e.date != ctx.today) add(ChipData(Icons.Rounded.CalendarToday, Dates.dayHeader(e.date, ctx.today)))
    if (e.installments > 1) add(ChipData(Icons.Rounded.Repeat, "${e.installments}x"))
}

/** O que o Cryo entendeu, em cartõezinhos que saltam quando aparecem. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Chips(e: ParsedEntry?, ctx: ParseContext) {
    FlowRow(
        Modifier.fillMaxWidth().heightIn(min = 44.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips(e, ctx).forEach { ch -> key(ch.label) { Chip(ch.icon, ch.label) } }
    }
}

@Composable
private fun Chip(icon: ImageVector, label: String) {
    val anim = animationsOn()
    val k = remember { Animatable(if (anim) 0f else 1f) }
    LaunchedEffect(Unit) { if (k.value < 1f) k.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 420f)) }
    Row(
        Modifier.scale(0.6f + 0.4f * k.value).alpha(k.value.coerceIn(0f, 1f)).depthCard(RoundedCornerShape(50))
            .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, MaterialTheme.colorScheme.primary, 30.dp)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.titleSmall)
    }
}

/** Com vários lançamentos numa fala: um cartão por lançamento (tocar abre para ajustar). */
@Composable
private fun EntryRow(e: ParsedEntry?, d: TxDraft, ctx: ParseContext, onClick: () -> Unit) {
    val appear by rememberAppear(350)
    Column(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).alpha(appear).depthCard().clickable(onClick = onClick).padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(d.description.ifBlank { "—" }, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(Money.format(d.amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        val rest = chips(e, ctx).drop(1).joinToString(" · ") { it.label }
        if (rest.isNotEmpty()) Text(rest, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        d.problem()?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
    }
}
