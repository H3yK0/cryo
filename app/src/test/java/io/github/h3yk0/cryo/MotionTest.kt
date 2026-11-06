// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import android.provider.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.core.app.ApplicationProvider
import io.github.h3yk0.cryo.ui.components.animateMoney
import io.github.h3yk0.cryo.ui.components.rememberAppear
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Movimento do app (PRD: DS-MOV, ACS-08). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = TestApp::class)
class MotionTest {
    @get:Rule val rule = createComposeRule()

    private fun shown(tag: String): String =
        rule.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.Text].joinToString("") { it.text }

    /** DS-MOV-05: o número conta até o novo valor e termina exato; na primeira vez aparece pronto. */
    @Test fun valorContaAteONovoETerminaExato() {
        val target = mutableLongStateOf(1_000L)
        rule.mainClock.autoAdvance = false
        rule.setContent { Text(animateMoney(target.longValue).toString(), Modifier.testTag("v")) }
        rule.mainClock.advanceTimeByFrame()
        assertEquals("1000", shown("v"))

        rule.runOnUiThread { target.longValue = 123_457L; Snapshot.sendApplyNotifications() }
        repeat(3) { rule.mainClock.advanceTimeByFrame() }
        rule.mainClock.advanceTimeBy(200)
        val middle = shown("v").toLong()
        assertTrue("no meio da contagem: $middle", middle in 1_001L..123_456L)

        // Mudou de novo no meio: continua de onde estava e termina no valor exato.
        rule.runOnUiThread { target.longValue = 99_999L; Snapshot.sendApplyNotifications() }
        rule.mainClock.advanceTimeBy(2_000)
        assertEquals("99999", shown("v"))
    }

    /** ACS-08: com "Remover animações" (escala 0), o que "aparece" já começa pronto. */
    @Test fun removerAnimacoesMostraOFinal() {
        Settings.Global.putFloat(ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        try {
            rule.mainClock.autoAdvance = false
            rule.setContent {
                val p by rememberAppear(durationMillis = 5_000)
                Text(if (p >= 1f) "pronto" else "andando", Modifier.testTag("p"))
            }
            rule.mainClock.advanceTimeBy(100)
            assertEquals("pronto", shown("p"))
        } finally {
            Settings.Global.putFloat(ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }
    }

    /** DS-MOV-06: com a escala normal, a mesma animação ainda está andando depois de 100 ms. */
    @Test fun comAnimacoesAparecerLeva() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val p by rememberAppear(durationMillis = 5_000)
            Text(if (p >= 1f) "pronto" else "andando", Modifier.testTag("p"))
        }
        rule.mainClock.advanceTimeBy(100)
        assertEquals("andando", shown("p"))
        rule.mainClock.advanceTimeBy(6_000)
        assertEquals("pronto", shown("p"))
        assertEquals(0, rule.onAllNodesWithText("andando").fetchSemanticsNodes().size)
    }
}
