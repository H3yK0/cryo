// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.data.Backup
import io.github.h3yk0.cryo.data.Palette
import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.AccountType
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.notify.Reminders
import io.github.h3yk0.cryo.ui.components.AmountField
import io.github.h3yk0.cryo.ui.components.ConfirmDialog
import io.github.h3yk0.cryo.ui.components.FieldLabel
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.Motion
import io.github.h3yk0.cryo.ui.components.Motion.steps
import io.github.h3yk0.cryo.ui.components.rememberAppear
import io.github.h3yk0.cryo.ui.components.Stepper
import io.github.h3yk0.cryo.ui.components.SwitchRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Passos da apresentação (RF-BOA-08). */
private object Step {
    const val WELCOME = 0
    const val TOUR = 1
    const val ACCOUNT = 2
    const val CARD = 3
    const val DONE = 4
    const val COUNT = 5
}

/**
 * Primeira abertura (RF-BOA-08): boas-vindas (nome, língua, moeda, restaurar), o passeio "Como usar o Cryo" (RF-TOUR-02),
 * conta principal, cartão (opcional) e lembretes.
 */
@Composable
fun OnboardingScreen() {
    val c = LocalContainer.current
    val ctx = LocalContext.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var accName by rememberSaveable { mutableStateOf(Res.s(R.string.wallet_conta_principal)) }
    var balance by rememberSaveable { mutableLongStateOf(0L) }
    var negative by rememberSaveable { mutableStateOf(false) }
    var cash by rememberSaveable { mutableStateOf(true) }
    var hasCard by rememberSaveable { mutableStateOf(false) }
    var cardName by rememberSaveable { mutableStateOf("") }
    var limit by rememberSaveable { mutableLongStateOf(0L) }
    var closing by rememberSaveable { mutableIntStateOf(3) }
    var due by rememberSaveable { mutableIntStateOf(10) }
    val notif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    var showLanguage by rememberSaveable { mutableStateOf(false) }
    var showCurrency by rememberSaveable { mutableStateOf(false) }
    var restoring by remember { mutableStateOf<Backup.Restored?>(null) }
    // RF-BOA-06: quem já usa o Cryo (celular novo) restaura um backup e pula a apresentação.
    val pickBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) c.launch {
            try {
                val text = withContext(Dispatchers.IO) { ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }
                    ?: throw IllegalArgumentException(Res.s(R.string.settings_nao_consegui_ler_arquivo))
                val restored = Backup.import(text)
                if (restored.testMode && !c.sandbox) {
                    throw IllegalArgumentException(Res.s(R.string.settings_este_backup_modo_teste))
                }
                restoring = restored
            } catch (e: Exception) {
                c.message(e.message ?: Res.s(R.string.settings_arquivo_invalido))
            }
        }
    }

    fun finish() {
        c.launch {
            c.settings.setName(name)
            val accId = c.repo.saveAccount(
                Account(name = accName.ifBlank { Res.s(R.string.wallet_conta_principal) }.trim(), type = AccountType.CHECKING, initialBalance = if (negative) -balance else balance, color = Palette.pick(5)),
            )
            if (cash) c.repo.saveAccount(Account(name = Res.s(R.string.wallet_carteira), type = AccountType.CASH, initialBalance = 0, color = Palette.pick(9), sortOrder = 1))
            if (hasCard && cardName.isNotBlank() && limit > 0) {
                c.repo.saveCard(CreditCard(name = cardName.trim(), limitAmount = limit, closingDay = closing, dueDay = due, color = Palette.pick(2), payAccountId = accId))
            }
            c.settings.setDefaultAccount(accId)
            c.settings.setOnboardingDone(true)
        }
    }

    // O Voltar do celular volta um passo (no passeio, uma página: RF-TOUR-05).
    BackHandler(enabled = step > Step.WELCOME && step != Step.TOUR) { step-- }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding(),
    ) {
        // DS-MOV-09: a barra de progresso anda com suavidade.
        val progress by animateFloatAsState((step + 1f) / Step.COUNT, tween(Motion.MEDIUM, easing = Motion.Emphasized), label = "progresso")
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp))
        AnimatedContent(step, modifier = Modifier.weight(1f), transitionSpec = { steps() }, label = "onboarding") { s ->
            if (s == Step.TOUR) {
                // RF-TOUR-02: o passeio, já na língua e na moeda escolhidas. "Pular" vai direto ao cadastro.
                Tour(
                    lastLabel = Res.s(R.string.tour_start), onFinish = { step = Step.ACCOUNT },
                    onBackFromFirst = { step = Step.WELCOME }, onSkip = { step = Step.ACCOUNT },
                )
                return@AnimatedContent
            }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp)) {
                when (s) {
                    Step.WELCOME -> {
                        Spacer(Modifier.height(32.dp))
                        // RF-BOA-01: a marca chega girando de leve e crescendo; o texto vem logo depois.
                        val mark by rememberAppear(durationMillis = 900)
                        val words by rememberAppear(durationMillis = 700, delayMillis = 250)
                        Box(
                            Modifier.size(104.dp).align(Alignment.CenterHorizontally)
                                .graphicsLayer { alpha = mark; val k = 0.6f + 0.4f * mark; scaleX = k; scaleY = k; rotationZ = -40f * (1f - mark) }
                                .clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) { Icon(painterResource(R.drawable.ic_cryo_mark), null, Modifier.size(60.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                        Spacer(Modifier.height(24.dp))
                        Column(Modifier.graphicsLayer { alpha = words; translationY = (1f - words) * 16.dp.toPx() }) {
                            Text(Res.s(R.string.onboarding_bem_vindo_ao_cryo), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().semantics { heading() })
                            Text(
                                Res.s(R.string.onboarding_seu_dinheiro_claro_como), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Spacer(Modifier.height(32.dp))
                        OutlinedTextField(
                            name, { name = it }, label = { Text(Res.s(R.string.onboarding_como_quer_ser_chamado)) }, singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words), modifier = Modifier.fillMaxWidth(),
                        )
                        // RF-BOA-07: a língua e a moeda que o celular escolheu, para trocar ali mesmo.
                        Spacer(Modifier.height(12.dp))
                        LocaleLine(Icons.Rounded.Language, Res.s(R.string.locale_language_title), languageSummary(ctx)) { showLanguage = true }
                        LocaleLine(Icons.Rounded.Payments, Res.s(R.string.locale_currency_title), currencySummary(ctx)) { showCurrency = true }
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = { pickBackup.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Rounded.SettingsBackupRestore, null); Spacer(Modifier.width(8.dp)); Text(Res.s(R.string.onboarding_ja_uso_cryo_restaurar))
                        }
                    }
                    Step.ACCOUNT -> {
                        Text(Res.s(R.string.onboarding_sua_conta_principal), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                        Text(
                            Res.s(R.string.onboarding_onde_fica_maior_parte),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            accName, { accName = it }, label = { Text(Res.s(R.string.wallet_nome_conta)) }, singleLine = true,
                            placeholder = { Text(Res.s(R.string.onboarding_ex_nubank_caixa_inter)) },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words), modifier = Modifier.fillMaxWidth(),
                        )
                        FieldLabel(Res.s(R.string.onboarding_quanto_tem_nela_hoje))
                        AmountField(balance, { balance = it }, label = Res.s(R.string.wallet_saldo_atual))
                        SwitchRow(Res.s(R.string.onboarding_saldo_esta_negativo), negative, { negative = it })
                        SwitchRow(Res.s(R.string.onboarding_tambem_uso_dinheiro_vivo), cash, { cash = it }, Res.s(R.string.onboarding_cria_carteira_para_gastos))
                    }
                    Step.CARD -> {
                        Text(Res.s(R.string.onboarding_cartao_credito), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                        Text(
                            Res.s(R.string.onboarding_com_cartao_cadastrado_cryo),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        SwitchRow(Res.s(R.string.onboarding_uso_cartao_credito), hasCard, { hasCard = it })
                        // DS-MOV-09: os campos do cartão se abrem com a altura animada.
                        AnimatedVisibility(
                            hasCard,
                            enter = expandVertically(tween(Motion.MEDIUM, easing = Motion.Emphasized)) + fadeIn(tween(Motion.MEDIUM)),
                            exit = shrinkVertically(tween(Motion.MEDIUM, easing = Motion.Emphasized)) + fadeOut(tween(Motion.SHORT)),
                        ) { Column {
                            OutlinedTextField(
                                cardName, { cardName = it }, label = { Text(Res.s(R.string.wallet_nome_cartao)) }, singleLine = true,
                                placeholder = { Text(Res.s(R.string.onboarding_ex_nubank)) },
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words), modifier = Modifier.fillMaxWidth(),
                            )
                            FieldLabel(Res.s(R.string.wallet_limite))
                            AmountField(limit, { limit = it }, label = Res.s(R.string.wallet_limite_total))
                            Spacer(Modifier.height(8.dp))
                            Stepper(closing, { closing = it }, 1..31, Res.s(R.string.wallet_dia_que_fecha))
                            Stepper(due, { due = it }, 1..31, Res.s(R.string.wallet_dia_que_vence))
                            Text(
                                Res.s(R.string.onboarding_para_ver_essas_datas),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } }
                    }
                    else -> {
                        Spacer(Modifier.height(24.dp))
                        Text(Res.s(R.string.onboarding_tudo_pronto), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                        Spacer(Modifier.height(12.dp))
                        Feature(Icons.Rounded.Mic, Res.s(R.string.onboarding_experimente_registro_rapido), Res.s(R.string.onboarding_tela_inicial_toque_microfone))
                        Feature(Icons.Rounded.NotificationsActive, Res.s(R.string.onboarding_lembretes_contas), Res.s(R.string.onboarding_cadastre_suas_contas_fixas))
                        if (Build.VERSION.SDK_INT >= 33 && !Reminders.canNotify(ctx)) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { notif.launch(Manifest.permission.POST_NOTIFICATIONS) }, modifier = Modifier.fillMaxWidth()) {
                                Text(Res.s(R.string.onboarding_permitir_notificacoes))
                            }
                        }
                    }
                }
            }
        }
        // O passeio tem os próprios botões.
        if (step != Step.TOUR) Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (step > Step.WELCOME) TextButton(onClick = { step-- }, modifier = Modifier.height(56.dp)) { Text(Res.s(R.string.onboarding_voltar)) }
            Button(
                onClick = { if (step < Step.DONE) step++ else finish() },
                enabled = step != Step.CARD || !hasCard || (cardName.isNotBlank() && limit > 0),
                modifier = Modifier.weight(1f).height(56.dp),
            ) {
                Text(
                    when (step) {
                        Step.WELCOME -> Res.s(R.string.onboarding_comecar)
                        Step.CARD -> if (hasCard) Res.s(R.string.onboarding_continuar) else Res.s(R.string.onboarding_pular)
                        Step.DONE -> Res.s(R.string.onboarding_ir_para_cryo)
                        else -> Res.s(R.string.onboarding_continuar)
                    },
                )
            }
        }
    }
    if (showLanguage) LanguageSheet(onDismiss = { showLanguage = false })
    if (showCurrency) CurrencySheet(onDismiss = { showCurrency = false })
    restoring?.let { restored ->
        val snap = restored.snapshot
        ConfirmDialog(
            Res.s(R.string.onboarding_restaurar_este_backup),
            Res.s(R.string.onboarding_dados_arquivo_entram_cryo, Res.p(R.plurals.count_transactions, snap.txs.size, snap.txs.size), Res.p(R.plurals.count_accounts, snap.accounts.size, snap.accounts.size)),
            Res.s(R.string.wallet_restaurar),
            onConfirm = {
                restoring = null
                c.launch {
                    c.repo.replaceAll(snap, fromOldVersion = restored.version < Backup.VERSION)
                    snap.accounts.firstOrNull { !it.archived }?.let { c.settings.setDefaultAccount(it.id) }
                    c.settings.setOnboardingDone(true)
                    c.message(Res.s(R.string.settings_backup_restaurado))
                }
            },
            onDismiss = { restoring = null },
        )
    }
}

@Composable
private fun LocaleLine(icon: ImageVector, title: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onClick).padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
        Text(Res.s(R.string.onboarding_trocar), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun Feature(icon: ImageVector, title: String, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer) }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Tela de bloqueio: pede a digital/senha do aparelho. */
@Composable
fun LockScreen(onUnlock: () -> Unit) {
    LaunchedEffect(Unit) { onUnlock() }
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(88.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) { Icon(painterResource(R.drawable.ic_cryo_mark), null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer) }
        Spacer(Modifier.height(20.dp))
        Text(Res.s(R.string.onboarding_cryo_bloqueado), style = MaterialTheme.typography.headlineSmall)
        Text(
            Res.s(R.string.onboarding_use_sua_digital_rosto), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onUnlock, modifier = Modifier.height(52.dp)) { Text(Res.s(R.string.onboarding_desbloquear)) }
    }
}
