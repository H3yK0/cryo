// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.h3yk0.cryo.data.Palette
import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.Category
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.domain.Dates
import io.github.h3yk0.cryo.domain.Money
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/* ============================== Valor (R$) ============================== */

private class MoneyTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val cents = text.text.toLongOrNull() ?: 0L
        val out = Money.format(cents)
        return TransformedText(
            AnnotatedString(out),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = out.length
                override fun transformedToOriginal(offset: Int) = text.length
            },
        )
    }
}

/**
 * Campo de valor no estilo "caixa eletrônico": você digita só números e os centavos se ajustam sozinhos
 * (1 → R$ 0,01; 1250 → R$ 12,50). Teclado numérico grande, sem vírgulas para errar.
 */
@Composable
fun AmountField(
    cents: Long,
    onChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Valor",
    color: Color = MaterialTheme.colorScheme.onSurface,
    autoFocus: Boolean = false,
    onDone: (() -> Unit)? = null,
) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val digits = if (cents == 0L) "" else cents.toString()
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                focus.requestFocus(); keyboard?.show()
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        BasicTextField(
            value = TextFieldValue(digits, TextRange(digits.length)),
            onValueChange = { v ->
                val d = v.text.filter(Char::isDigit).trimStart('0').take(12)
                onChange(d.toLongOrNull() ?: 0L)
            },
            textStyle = MaterialTheme.typography.displaySmall.copy(color = color, textAlign = TextAlign.Center),
            visualTransformation = MoneyTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { keyboard?.hide(); onDone?.invoke() }),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus)
                .semantics { contentDescription = "$label: ${Money.format(cents)}. Digite apenas números." },
        )
    }
    LaunchedEffect(autoFocus) {
        if (autoFocus) { focus.requestFocus(); keyboard?.show() }
    }
}

/* ============================== Categorias ============================== */

/** Grade de categorias (ícone grande + nome), fácil de tocar e de ler. */
@Composable
fun CategoryGrid(categories: List<Category>, selectedId: Long?, onSelect: (Category) -> Unit, columns: Int = 4) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // largura em pixels inteiros (arredondada para baixo) para caberem exatamente [columns] por linha
        val itemW = with(density) { (constraints.maxWidth / columns).toDp() }
        FlowRow(Modifier.fillMaxWidth()) {
            categories.forEach { c ->
                val sel = c.id == selectedId
                Column(
                    Modifier
                        .width(itemW)
                        .clip(MaterialTheme.shapes.medium)
                        .selectable(selected = sel, role = Role.RadioButton, onClick = { onSelect(c) })
                        .padding(vertical = 8.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (sel) {
                            Box(
                                Modifier.size(52.dp).clip(CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            )
                        }
                        IconBadge(categoryIcon(c.icon), Color(c.color), size = 44.dp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        c.name, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                        color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

/* ============================ Contas e cartões ============================ */

sealed interface Source {
    data class Acc(val id: Long) : Source
    data class Card(val id: Long) : Source
}

/** Escolha de onde sai/entra o dinheiro: contas e (opcionalmente) cartões. */
@Composable
fun SourceChips(
    accounts: List<Account>,
    cards: List<CreditCard>,
    selected: Source?,
    onSelect: (Source) -> Unit,
    balances: Map<Long, Long> = emptyMap(),
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        accounts.forEach { a ->
            val sel = selected == Source.Acc(a.id)
            FilterChip(
                selected = sel,
                onClick = { onSelect(Source.Acc(a.id)) },
                label = { Text(a.name) },
                leadingIcon = {
                    Icon(if (sel) Icons.Rounded.Check else accountIcon(a.type), null, Modifier.size(18.dp))
                },
            )
        }
        cards.forEach { c ->
            val sel = selected == Source.Card(c.id)
            FilterChip(
                selected = sel,
                onClick = { onSelect(Source.Card(c.id)) },
                label = { Text("Cartão ${c.name}") },
                leadingIcon = { Icon(if (sel) Icons.Rounded.Check else Icons.Rounded.CreditCard, null, Modifier.size(18.dp)) },
            )
        }
    }
}

@Composable
fun AccountChips(accounts: List<Account>, selectedId: Long?, onSelect: (Long) -> Unit, exclude: Long? = null) {
    SourceChips(
        accounts = accounts.filter { it.id != exclude }, cards = emptyList(),
        selected = selectedId?.let { Source.Acc(it) }, onSelect = { if (it is Source.Acc) onSelect(it.id) },
    )
}

/* ================================= Data ================================= */

private fun LocalDate.toUtcMillis() = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
private fun Long.utcMillisToDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

@Composable
fun DateChooser(date: LocalDate, today: LocalDate, onChange: (LocalDate) -> Unit) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val isToday = date == today
    val isYesterday = date == today.minusDays(1)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = isToday, onClick = { onChange(today) }, label = { Text("Hoje") })
        FilterChip(selected = isYesterday, onClick = { onChange(today.minusDays(1)) }, label = { Text("Ontem") })
        FilterChip(
            selected = !isToday && !isYesterday,
            onClick = { showPicker = true },
            label = { Text(if (!isToday && !isYesterday) Dates.full(date) else "Outra data") },
            leadingIcon = { Icon(Icons.Rounded.CalendarMonth, null, Modifier.size(18.dp)) },
        )
    }
    if (showPicker) {
        DatePickerDialogField(date, onDismiss = { showPicker = false }, onPick = { onChange(it); showPicker = false })
    }
}

@Composable
fun DatePickerDialogField(initial: LocalDate?, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = (initial ?: LocalDate.now()).toUtcMillis())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { state.selectedDateMillis?.let { onPick(it.utcMillisToDate()) } ?: onDismiss() }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    ) { DatePicker(state = state, showModeToggle = true) }
}

/* ============================ Seletores simples ============================ */

@Composable
fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        options.forEachIndexed { i, (value, label) ->
            SegmentedButton(
                selected = value == selected,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index = i, count = options.size),
                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
        }
    }
}

@Composable
fun ColorChooser(selected: Int, onSelect: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Palette.colors.forEachIndexed { i, c ->
            val sel = c == selected
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(c))
                    .then(if (sel) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                    .selectable(selected = sel, role = Role.RadioButton, onClick = { onSelect(c) })
                    .semantics { contentDescription = "Cor ${i + 1}" },
                contentAlignment = Alignment.Center,
            ) { if (sel) Icon(Icons.Rounded.Check, null, tint = Color.White) }
        }
    }
}

@Composable
fun IconChooser(options: List<Pair<String, String>>, selected: String, color: Color, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (key, desc) ->
            val sel = key == selected
            Box(
                Modifier
                    .clip(CircleShape)
                    .then(if (sel) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                    .selectable(selected = sel, role = Role.RadioButton, onClick = { onSelect(key) })
                    .padding(3.dp),
            ) { IconBadge(categoryIcon(key), color, 42.dp, contentDescription = desc) }
        }
    }
}

@Composable
fun EmojiChooser(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { e ->
            val sel = e == selected
            Box(
                Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .selectable(selected = sel, role = Role.RadioButton, onClick = { onSelect(e) }),
                contentAlignment = Alignment.Center,
            ) { Text(e, style = MaterialTheme.typography.titleLarge) }
        }
    }
}

/** Número com botões − e + (dia do mês, parcelas...). */
@Composable
fun Stepper(
    value: Int,
    onChange: (Int) -> Unit,
    range: IntRange,
    label: String,
    modifier: Modifier = Modifier,
    format: (Int) -> String = { it.toString() },
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        FilledTonalIconButton(onClick = { if (value > range.first) onChange(value - 1) }, enabled = value > range.first) {
            Icon(Icons.Rounded.Remove, "Diminuir $label")
        }
        Text(
            format(value), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center,
            modifier = Modifier.width(72.dp).semantics { stateDescription = format(value) },
        )
        FilledTonalIconButton(onClick = { if (value < range.last) onChange(value + 1) }, enabled = value < range.last) {
            Icon(Icons.Rounded.Add, "Aumentar $label")
        }
    }
}

@Composable
fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit, subtitle: String? = null, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** Rótulo de campo de formulário. */
@Composable
fun FieldLabel(text: String) {
    Text(
        text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

fun defaultColor(seed: Int) = Palette.pick(seed)
