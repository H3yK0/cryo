// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Shield
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import io.github.h3yk0.cryo.R
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.h3yk0.cryo.data.Palette
import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.AccountType
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.notify.Reminders
import io.github.h3yk0.cryo.ui.components.AmountField
import io.github.h3yk0.cryo.ui.components.FieldLabel
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.Stepper
import io.github.h3yk0.cryo.ui.components.SwitchRow

/** Primeira abertura: nome, conta principal, cartão (opcional) e lembretes. */
@Composable
fun OnboardingScreen() {
    val c = LocalContainer.current
    val ctx = LocalContext.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var accName by rememberSaveable { mutableStateOf("Conta principal") }
    var balance by rememberSaveable { mutableLongStateOf(0L) }
    var negative by rememberSaveable { mutableStateOf(false) }
    var cash by rememberSaveable { mutableStateOf(true) }
    var hasCard by rememberSaveable { mutableStateOf(false) }
    var cardName by rememberSaveable { mutableStateOf("") }
    var limit by rememberSaveable { mutableLongStateOf(0L) }
    var closing by rememberSaveable { mutableIntStateOf(3) }
    var due by rememberSaveable { mutableIntStateOf(10) }
    val notif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun finish() {
        c.launch {
            c.settings.setName(name)
            val accId = c.repo.saveAccount(
                Account(name = accName.ifBlank { "Conta principal" }.trim(), type = AccountType.CHECKING, initialBalance = if (negative) -balance else balance, color = Palette.pick(5)),
            )
            if (cash) c.repo.saveAccount(Account(name = "Carteira", type = AccountType.CASH, initialBalance = 0, color = Palette.pick(9), sortOrder = 1))
            if (hasCard && cardName.isNotBlank() && limit > 0) {
                c.repo.saveCard(CreditCard(name = cardName.trim(), limitAmount = limit, closingDay = closing, dueDay = due, color = Palette.pick(2), payAccountId = accId))
            }
            c.settings.setDefaultAccount(accId)
            c.settings.setOnboardingDone(true)
        }
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding(),
    ) {
        LinearProgressIndicator(progress = { (step + 1) / 4f }, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp))
        AnimatedContent(step, modifier = Modifier.weight(1f), label = "onboarding") { s ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp)) {
                when (s) {
                    0 -> {
                        Spacer(Modifier.height(24.dp))
                        Box(
                            Modifier.size(96.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer).align(Alignment.CenterHorizontally),
                            contentAlignment = Alignment.Center,
                        ) { Icon(painterResource(R.drawable.ic_cryo_mark), null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                        Spacer(Modifier.height(20.dp))
                        Text("Bem-vindo ao Cryo", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().semantics { heading() })
                        Text(
                            "Seu dinheiro, claro como gelo.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(24.dp))
                        Feature(Icons.Rounded.Mic, "Registre falando", "Diga “almoço 25 no débito” e pronto.")
                        Feature(Icons.AutoMirrored.Rounded.ShowChart, "Saiba como o mês vai terminar", "Previsão, calendário de gastos e o caminho do seu dinheiro.")
                        Feature(Icons.Rounded.Shield, "Seus dados são seus", "Tudo fica só neste celular, sem cadastro e sem internet.")
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            name, { name = it }, label = { Text("Como quer ser chamado? (opcional)") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words), modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    1 -> {
                        Text("Sua conta principal", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                        Text(
                            "Onde fica a maior parte do seu dinheiro? Pode ser o banco ou a carteira digital.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            accName, { accName = it }, label = { Text("Nome da conta") }, singleLine = true,
                            placeholder = { Text("Ex.: Nubank, Caixa, Inter") },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words), modifier = Modifier.fillMaxWidth(),
                        )
                        FieldLabel("Quanto tem nela hoje?")
                        AmountField(balance, { balance = it }, label = "Saldo atual")
                        SwitchRow("O saldo está negativo", negative, { negative = it })
                        SwitchRow("Também uso dinheiro vivo", cash, { cash = it }, "Cria uma “Carteira” para gastos em espécie")
                    }
                    2 -> {
                        Text("Cartão de crédito", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                        Text(
                            "Com o cartão cadastrado, o Cryo separa as compras por fatura e cuida das parcelas para você.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        SwitchRow("Uso cartão de crédito", hasCard, { hasCard = it })
                        if (hasCard) {
                            OutlinedTextField(
                                cardName, { cardName = it }, label = { Text("Nome do cartão") }, singleLine = true,
                                placeholder = { Text("Ex.: Nubank") },
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words), modifier = Modifier.fillMaxWidth(),
                            )
                            FieldLabel("Limite")
                            AmountField(limit, { limit = it }, label = "Limite total")
                            Spacer(Modifier.height(8.dp))
                            Stepper(closing, { closing = it }, 1..31, "Dia que fecha")
                            Stepper(due, { due = it }, 1..31, "Dia que vence")
                            Text(
                                "Dá para ver essas datas no app do seu banco. Você pode mudar depois.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    else -> {
                        Spacer(Modifier.height(24.dp))
                        Text("Tudo pronto!", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                        Spacer(Modifier.height(12.dp))
                        Feature(Icons.Rounded.Mic, "Experimente o registro rápido", "Na tela inicial, toque no microfone e fale um gasto do seu dia.")
                        Feature(Icons.Rounded.NotificationsActive, "Lembretes de contas", "Cadastre suas contas fixas em Planejar e o Cryo avisa antes de vencer.")
                        if (Build.VERSION.SDK_INT >= 33 && !Reminders.canNotify(ctx)) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { notif.launch(Manifest.permission.POST_NOTIFICATIONS) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Permitir notificações")
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (step > 0) TextButton(onClick = { step-- }, modifier = Modifier.height(56.dp)) { Text("Voltar") }
            Button(
                onClick = { if (step < 3) step++ else finish() },
                enabled = step != 2 || !hasCard || (cardName.isNotBlank() && limit > 0),
                modifier = Modifier.weight(1f).height(56.dp),
            ) {
                Text(
                    when (step) {
                        0 -> "Começar"
                        2 -> if (hasCard) "Continuar" else "Pular"
                        3 -> "Ir para o Cryo"
                        else -> "Continuar"
                    },
                )
            }
        }
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
        Text("Cryo bloqueado", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Use sua digital, rosto ou senha do celular.", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onUnlock, modifier = Modifier.height(52.dp)) { Text("Desbloquear") }
    }
}
