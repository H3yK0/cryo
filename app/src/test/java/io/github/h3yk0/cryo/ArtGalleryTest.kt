// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.h3yk0.cryo.data.ThemeMode
import io.github.h3yk0.cryo.domain.GoalArt
import io.github.h3yk0.cryo.ui.components.Art
import io.github.h3yk0.cryo.ui.components.ArtImage
import io.github.h3yk0.cryo.ui.components.GoalImage
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** RF-ILU-02/03: todas as ilustrações do Cryo numa imagem só, no claro e no escuro (imagens 69 e 70). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = TestApp::class, qualifiers = "+w420dp-h1100dp-xxhdpi")
class ArtGalleryTest {
    @get:Rule val rule = createComposeRule()

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun Gallery(dark: Boolean) {
        CryoTheme(mode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, dynamicColor = false) {
            val fg = MaterialTheme.colorScheme.onSurfaceVariant
            Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(12.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Art.entries.forEach { a ->
                        Column(Modifier.width(74.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            ArtImage(a, 64.dp)
                            Text(a.name.lowercase(), fontSize = 9.sp, color = fg, textAlign = TextAlign.Center)
                        }
                    }
                }
                Text(" ", Modifier.padding(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GoalArt.entries.forEachIndexed { i, g ->
                        Column(Modifier.width(74.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            GoalImage(g, Color(io.github.h3yk0.cryo.data.Palette.pick(i)), 64.dp)
                            Text(g.key, fontSize = 9.sp, color = fg, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }

    @Test fun claro() {
        rule.setContent { Gallery(false) }
        rule.onRoot().captureRoboImage("screenshots/69_ilustracoes.png")
    }

    @Test fun escuro() {
        rule.setContent { Gallery(true) }
        rule.onRoot().captureRoboImage("screenshots/70_ilustracoes_escuro.png")
    }
}
