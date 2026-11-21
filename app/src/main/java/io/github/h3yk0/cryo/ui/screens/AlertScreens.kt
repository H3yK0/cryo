// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.h3yk0.cryo.CryoApp
import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.data.SampleData
import io.github.h3yk0.cryo.domain.AppLocale
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.domain.Num
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.notify.Alert
import io.github.h3yk0.cryo.notify.AlertAction
import io.github.h3yk0.cryo.notify.AlertGroup
import io.github.h3yk0.cryo.notify.AlertKind
import io.github.h3yk0.cryo.notify.AlertState
import io.github.h3yk0.cryo.notify.AlertTone
import io.github.h3yk0.cryo.notify.Alerts
import io.github.h3yk0.cryo.notify.Reminders
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.components.Art
import io.github.h3yk0.cryo.ui.components.ArtImage
import io.github.h3yk0.cryo.ui.components.CryoButton
import io.github.h3yk0.cryo.ui.components.CryoTopBar
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.Segmented
import io.github.h3yk0.cryo.ui.components.SwitchRow
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import io.github.h3yk0.cryo.ui.components.CryoSheet

/** "20:30" (ou "8:30 PM") na língua do app. */
fun reminderTime(minutes: Int): String =
    LocalTime.of(minutes / 60, minutes % 60).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(AppLocale.locale))

/** Os horários sugeridos para o lembrete do dia (RF-AVI-11). */
private val REMINDER_TIMES = listOf(12 * 60, 18 * 60, 20 * 60 + 30, 22 * 60)

/** Pede a permissão de notificações (Android 13+) quando a pessoa liga algo que avisa. */
@Composable
private fun rememberNotificationAsk(): () -> Unit {
    val ctx = LocalContext.current
    val c = LocalContainer.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) c.message(Res.s(R.string.settings_sem_permissao_lembretes_nao))
    }
    return { if (Build.VERSION.SDK_INT >= 33 && !Reminders.canNotify(ctx)) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
}

/**
 * RF-AVI-11: "Quando eu te lembro de anotar?". Aparece uma vez (depois da apresentação ou na primeira abertura da 1.7) e
 * também em Ajustes › Avisos para trocar o horário. O lembrete só sai se nada foi anotado no dia.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReminderTimeSheet(current: Int?, onDismiss: () -> Unit, onChoose: (Int?) -> Unit) {
    var custom by remember { mutableStateOf(false) }
    CryoSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            ArtImage(Art.WEEK, 64.dp)
            Spacer(Modifier.height(14.dp))
            Text(Res.s(R.string.reminder_ask_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(Res.s(R.string.reminder_ask_text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                REMINDER_TIMES.forEach { m ->
                    FilterChip(
                        selected = current == m, onClick = { onChoose(m) },
                        label = { Text(reminderTime(m), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)) },
                    )
                }
                FilterChip(
                    selected = current != null && current !in REMINDER_TIMES, onClick = { custom = true },
                    label = {
                        Text(
                            if (current != null && current !in REMINDER_TIMES) reminderTime(current) else Res.s(R.string.reminder_ask_other),
                            style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                        )
                    },
                )
            }
            Spacer(Modifier.height(20.dp))
            CryoButton(onClick = { onChoose(current ?: AlertState.DEFAULT_REMINDER) }, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                Text(Res.s(R.string.reminder_ask_ok, reminderTime(current ?: AlertState.DEFAULT_REMINDER)))
            }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = { onChoose(null) }, modifier = Modifier.fillMaxWidth()) { Text(Res.s(R.string.reminder_ask_none)) }
        }
    }
    if (custom) {
        val start = current ?: AlertState.DEFAULT_REMINDER
        val picker = rememberTimePickerState(start / 60, start % 60, is24Hour = !DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(AppLocale.locale).format(LocalTime.NOON).contains(Regex("[AaPp]")))
        AlertDialog(
            onDismissRequest = { custom = false },
            text = { TimePicker(picker) },
            confirmButton = { TextButton(onClick = { custom = false; onChoose(picker.hour * 60 + picker.minute) }) { Text(Res.s(R.string.wallet_salvar)) } },
            dismissButton = { TextButton(onClick = { custom = false }) { Text(Res.s(R.string.wallet_cancelar)) } },
        )
    }
}

/** Guarda o horário escolhido e reagenda a noite. */
fun saveReminder(ctx: android.content.Context, minutes: Int?) {
    val state = AlertState(ctx)
    state.reminderMinutes = minutes
    state.reminderAsked = true
    runCatching { Alerts.scheduleEvening(ctx, state) }
}

/** RF-AVI-08: os exemplos ("Ver exemplo"): a notificação de verdade, com dados de mentira, na língua e na moeda da pessoa. */
object AlertExamples {
    fun of(g: AlertGroup, tone: AlertTone): Alert {
        val friendly = tone == AlertTone.FRIEND
        fun t(a: Int, d: Int, vararg args: Any?) = Res.s(if (friendly) a else d, *args)
        val m = { v: Long -> Money.format(SampleData.m(v)) }
        val cat = Res.s(R.string.alert_ex_category)
        return when (g) {
            AlertGroup.BILLS -> Alert(
                AlertKind.BILL_DUE, "sample:bills",
                "${Res.s(R.string.alert_ex_bill)} ${Res.s(R.string.reminder_vence_amanha)} · ${m(12990)}",
                t(R.string.alert_bill_a1, R.string.alert_bill_d), listOf(AlertAction.PAID, AlertAction.SNOOZE), open = "bills",
            )
            AlertGroup.BUDGET -> Alert(
                AlertKind.BUDGET_80, "sample:budget", Res.s(R.string.alert_80_title, cat, Num.percent(82)),
                t(R.string.alert_80_a1, R.string.alert_80_d, 18, m(54000), m(66000)), listOf(AlertAction.OPEN), open = "budgets",
            )
            AlertGroup.CARDS -> Alert(
                AlertKind.INVOICE_CLOSES, "sample:cards", Res.s(R.string.alert_invoice_title, Res.s(R.string.alert_ex_card)),
                t(R.string.alert_invoice_a1, R.string.alert_invoice_d, io.github.h3yk0.cryo.domain.Dates.monthName(java.time.YearMonth.now().plusMonths(2))), open = "wallet",
            )
            AlertGroup.RECURRING -> Alert(
                AlertKind.PRICE_UP, "sample:recurring", Res.s(R.string.alert_price_title, Res.s(R.string.alert_ex_service)),
                t(R.string.alert_price_a1, R.string.alert_price_d, m(500)), listOf(AlertAction.OPEN), open = "insights",
            )
            AlertGroup.WATCH -> Alert(
                AlertKind.DUPLICATE, "sample:watch", Res.s(R.string.alert_dup_title, Res.s(R.string.alert_ex_service)),
                t(R.string.alert_dup_a1, R.string.alert_dup_d, m(3990)), listOf(AlertAction.OPEN), open = "insights",
            )
            AlertGroup.GOALS -> Alert(
                AlertKind.GOAL_NEAR, "sample:goals", Res.s(R.string.alert_goal_title, Res.s(R.string.alert_ex_goal)),
                t(R.string.alert_goal_a1, R.string.alert_goal_d, m(15000)), open = "goals",
            )
            AlertGroup.SUMMARIES -> {
                val day = java.time.DayOfWeek.SATURDAY.getDisplayName(java.time.format.TextStyle.FULL, AppLocale.locale)
                Alert(
                    AlertKind.WEEK, "sample:summaries", Res.s(R.string.alert_week_title, m(48230)),
                    if (friendly) Res.s(R.string.alert_week_less, Num.percent(12), day) else Res.s(R.string.alert_week_d, day),
                    open = "insights",
                    week = listOf(4200L, 6100L, 3500L, 8800L, 5200L, 15800L, 4600L).map(SampleData::m), weekNormal = SampleData.m(7800),
                )
            }
            AlertGroup.DAILY -> Alert(
                AlertKind.DAILY, "sample:daily", Res.s(R.string.alert_daily_title), t(R.string.alert_daily_a1, R.string.alert_daily_d),
                listOf(AlertAction.NO_SPEND, AlertAction.LOG), open = "voice",
            )
        }
    }
}

private val GROUPS = listOf(
    Triple(AlertGroup.BILLS, R.string.alerts_group_bills, R.string.alerts_group_bills_desc),
    Triple(AlertGroup.BUDGET, R.string.alerts_group_budget, R.string.alerts_group_budget_desc),
    Triple(AlertGroup.CARDS, R.string.alerts_group_cards, R.string.alerts_group_cards_desc),
    Triple(AlertGroup.RECURRING, R.string.alerts_group_recurring, R.string.alerts_group_recurring_desc),
    Triple(AlertGroup.WATCH, R.string.alerts_group_watch, R.string.alerts_group_watch_desc),
    Triple(AlertGroup.GOALS, R.string.alerts_group_goals, R.string.alerts_group_goals_desc),
    Triple(AlertGroup.SUMMARIES, R.string.alerts_group_summaries, R.string.alerts_group_summaries_desc),
)

/**
 * RF-AVI-08 (TELA-AVISOS): o tom, o lembrete do dia, cada tipo de aviso com "Ver exemplo" e os sons. Vale sempre para os
 * dados de verdade (os avisos nunca usam o modo de teste).
 */
/**
 * ERR-048: o que a tela de avisos mostra, tirado uma vez por mudança. Antes a tela lia os ajustes direto do arquivo
 * dentro de cada `item { }` da lista e só **declarava** o contador de mudanças sem usá-lo: o Compose não registrava
 * leitura nenhuma, então trocar o tom ou um interruptor só aparecia depois de sair e voltar.
 */
private class AlertUi(
    val tone: AlertTone,
    val sounds: Boolean,
    val reminderMinutes: Int?,
    val enabled: Map<AlertGroup, Boolean>,
)

@Composable
private fun rememberAlertUi(state: AlertState): AlertUi {
    val version by state.version.collectAsState()
    return remember(version) {
        AlertUi(state.tone, state.sounds, state.reminderMinutes, AlertGroup.entries.associateWith { state.enabled(it) })
    }
}

@Composable
fun AlertSettingsScreen() {
    val ctx = LocalContext.current
    val nav = LocalNav.current
    val c = LocalContainer.current
    val state = remember { AlertState(ctx) }
    val ui = rememberAlertUi(state)
    // Os avisos são sempre dos dados de verdade (no modo de teste, o interruptor das contas também é o de verdade).
    val real = remember { (ctx.applicationContext as? CryoApp)?.real ?: c }
    val askNotifications = rememberNotificationAsk()
    var choosingTime by remember { mutableStateOf(false) }

    Scaffold(topBar = { CryoTopBar(Res.s(R.string.alerts_settings_title), onBack = nav::back) }, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ArtImage(Art.BILL, 48.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(Res.s(R.string.alerts_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (!Reminders.canNotify(ctx)) item {
                Spacer(Modifier.height(12.dp))
                SettingsGroup {
                    Text(Res.s(R.string.alerts_no_permission), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(8.dp))
                    OutlinedButton(onClick = askNotifications, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) { Text(Res.s(R.string.alerts_allow)) }
                    Spacer(Modifier.height(4.dp))
                }
            }

            item { Header(Res.s(R.string.alerts_tone)) }
            item {
                SettingsGroup {
                    Segmented(
                        listOf(AlertTone.FRIEND to Res.s(R.string.alerts_tone_friend), AlertTone.DISCREET to Res.s(R.string.alerts_tone_discreet)),
                        ui.tone, { state.tone = it }, Modifier.padding(6.dp),
                    )
                    Text(
                        Res.s(if (ui.tone == AlertTone.FRIEND) R.string.alerts_tone_friend_desc else R.string.alerts_tone_discreet_desc),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }

            item { Header(Res.s(R.string.alerts_daily)) }
            item {
                SettingsGroup {
                    val minutes = ui.reminderMinutes
                    SettingLink(
                        Icons.Rounded.Schedule, Res.s(R.string.alerts_daily_time),
                        if (minutes == null) Res.s(R.string.alerts_off) else Res.s(R.string.alerts_daily_time_sub, reminderTime(minutes)),
                    ) { choosingTime = true }
                    ExampleButton { Alerts.post(ctx, AlertExamples.of(AlertGroup.DAILY, ui.tone), state, record = false); askNotifications() }
                }
            }

            item { Header(Res.s(R.string.alerts_what)) }
            item {
                SettingsGroup {
                    GROUPS.forEach { (g, title, desc) ->
                        val on = ui.enabled[g] == true
                        SwitchRow(Res.s(title), on, { v ->
                            state.setEnabled(g, v)
                            // Contas e dívidas = o mesmo interruptor da 1.0 (os lembretes de sempre).
                            if (g == AlertGroup.BILLS) c.launch { real.settings.setReminders(v) }
                            if (g == AlertGroup.SUMMARIES) runCatching { Alerts.scheduleEvening(ctx, state) }
                            if (v) askNotifications()
                        }, Res.s(desc), Modifier.padding(horizontal = 6.dp))
                        if (on) ExampleButton { if (!Alerts.post(ctx, AlertExamples.of(g, ui.tone), state, record = false)) askNotifications() }
                    }
                }
            }

            item { Header(Res.s(R.string.alerts_sounds)) }
            item {
                SettingsGroup {
                    SwitchRow(Res.s(R.string.alerts_sounds_on), ui.sounds, { state.sounds = it }, Res.s(R.string.alerts_sounds_desc), Modifier.padding(horizontal = 6.dp))
                }
            }
            item {
                Text(
                    Res.s(R.string.alerts_slots_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp, start = 4.dp, end = 4.dp),
                )
            }
        }
    }
    if (choosingTime) ReminderTimeSheet(ui.reminderMinutes, onDismiss = { choosingTime = false }) { m ->
        choosingTime = false
        saveReminder(ctx, m)
        if (m != null) askNotifications()
    }
}

@Composable
private fun ExampleButton(onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)) { Text(Res.s(R.string.alerts_example)) }
}
