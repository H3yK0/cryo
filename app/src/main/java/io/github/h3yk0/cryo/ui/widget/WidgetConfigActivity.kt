// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.data.LocaleSettings
import io.github.h3yk0.cryo.data.ThemeMode
import io.github.h3yk0.cryo.data.WidgetPrefs
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.domain.WidgetConfig
import io.github.h3yk0.cryo.domain.WidgetLine
import io.github.h3yk0.cryo.domain.WidgetMain
import io.github.h3yk0.cryo.ui.components.CryoButton
import io.github.h3yk0.cryo.ui.components.GroupTitle
import io.github.h3yk0.cryo.ui.components.SwitchRow
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import kotlinx.coroutines.launch

/**
 * RF-WID-02 (1.7, LA9): a tela de editar um widget, aberta pelo Android ao colocar o widget e em "Editar o widget". Ela só
 * guarda escolhas de tela (o número grande e as linhas), nunca mostra dados, então não passa pelo bloqueio.
 */
class WidgetConfigActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleSettings.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val widgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Se a pessoa sair sem salvar, o Android não põe o widget (ou mantém o de antes).
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val prefs = WidgetPrefs(this)
        val initial = prefs.config(widgetId)
        setContent {
            CryoTheme(mode = ThemeMode.SYSTEM, dynamicColor = true) {
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                    WidgetConfigScreen(initial) { config ->
                        prefs.save(widgetId, config)
                        lifecycleScope.launch {
                            runCatching {
                                val glanceId = GlanceAppWidgetManager(this@WidgetConfigActivity).getGlanceIdBy(widgetId)
                                val widget = CryoWidget()
                                widget.update(this@WidgetConfigActivity, glanceId)
                            }
                            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                            finish()
                        }
                    }
                }
            }
        }
    }
}

/** RF-WID-02: o número principal, as linhas de baixo e o aviso de que o widget mostra os valores (RF-WID-04). */
@Composable
internal fun WidgetConfigScreen(initial: WidgetConfig, onSave: (WidgetConfig) -> Unit) {
    var main by remember { mutableStateOf(initial.main) }
    var lines by remember { mutableStateOf(initial.lines) }
    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(Res.s(R.string.widget_config_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(Res.s(R.string.widget_config_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        GroupTitle(Res.s(R.string.widget_main_title))
        Column(Modifier.selectableGroup()) {
            WidgetMain.entries.forEach { m ->
                Row(
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                        .selectable(selected = m == main, role = Role.RadioButton, onClick = { main = m })
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = m == main, onClick = null)
                    Spacer(Modifier.width(12.dp))
                    Text(mainLabel(m), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        GroupTitle(Res.s(R.string.widget_lines_title))
        WidgetLine.entries.forEach { line ->
            SwitchRow(lineLabel(line), line in lines, { on -> lines = if (on) lines + line else lines - line })
        }
        Spacer(Modifier.height(8.dp))
        Text(Res.s(R.string.widget_privacy_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        CryoButton(onClick = { onSave(WidgetConfig(main, lines)) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(Res.s(R.string.widget_save))
        }
    }
}

internal fun mainLabel(m: WidgetMain): String = Res.s(
    when (m) {
        WidgetMain.MONTH_END -> R.string.month_end
        WidgetMain.BALANCE -> R.string.home_balance
        WidgetMain.GENERAL -> R.string.widget_main_general
        WidgetMain.NET_WORTH -> R.string.home_net_worth
    },
)

internal fun lineLabel(l: WidgetLine): String = Res.s(
    when (l) {
        WidgetLine.BILLS -> R.string.widget_line_bills
        WidgetLine.BUDGETS -> R.string.home_budgets_short
        WidgetLine.GOALS -> R.string.home_goals_short
        WidgetLine.ENTRY -> R.string.widget_line_entry
    },
)
