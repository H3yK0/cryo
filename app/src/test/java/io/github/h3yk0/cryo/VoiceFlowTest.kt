// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureScreenRoboImage
import io.github.h3yk0.cryo.data.AppSettings
import io.github.h3yk0.cryo.data.ThemeMode
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.Navigator
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.components.rememberLedger
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import io.github.h3yk0.cryo.ui.tx.MicPermissionSheet
import io.github.h3yk0.cryo.ui.tx.VoiceSheet
import io.github.h3yk0.cryo.voice.Listener
import io.github.h3yk0.cryo.voice.VoiceEngineKind
import io.github.h3yk0.cryo.voice.VoicePhase
import io.github.h3yk0.cryo.voice.VoiceProblem
import io.github.h3yk0.cryo.voice.VoiceUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.time.LocalDate

/**
 * RF-VOZ-03/06/08: a tela de eco com um ouvido falso (o estado é dado pelo teste). Imagens 58–62 e os caminhos: salvar
 * um lançamento, salvar vários de uma fala, "não ouvi nada" → falar de novo, cancelar.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = TestApp::class, qualifiers = "+w393dp-h852dp-xxhdpi")
class VoiceFlowTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private lateinit var c: AppContainer
    private val today = LocalDate.now()
    private val settings = AppSettings(onboardingDone = true, defaultAccountId = 1, dynamicColor = false)

    private class FakeListener(initial: VoiceUi) : Listener {
        override val kind = VoiceEngineKind.WHISPER
        val flow = MutableStateFlow(initial)
        override val ui = flow
        var started = false
        var finished = false
        var cancelled = false
        override fun start() { started = true }
        override fun finish() { finished = true }
        override fun cancel() { cancelled = true }
    }

    private val made = ArrayList<FakeListener>()
    private var dismissed = false

    @Before fun setUp() {
        c = AppContainer(ApplicationProvider.getApplicationContext(), sandbox = true)
        runBlocking { seed(c, today); c.autoBackup.clear() }
        waitFor { (c.repo.snapshot.value?.txs?.size ?: 0) > 50 }
    }

    private fun waitFor(ms: Long = 10_000, condition: () -> Boolean) = rule.waitUntil(ms) {
        ShadowLooper.idleMainLooper()
        condition()
    }

    private fun show(initial: VoiceUi) {
        rule.setContent {
            CryoTheme(mode = ThemeMode.LIGHT, dynamicColor = false) {
                val nav = rememberNavController()
                val navigator = remember { Navigator(nav) }
                CompositionLocalProvider(LocalContainer provides c, LocalSettings provides settings, LocalNav provides navigator) {
                    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                        val l = rememberLedger() ?: return@Surface
                        VoiceSheet(l, VoiceEngineKind.WHISPER, onDismiss = { dismissed = true }, onLegacy = {}, listeners = { _, _ ->
                            FakeListener(if (made.isEmpty()) initial else VoiceUi(VoicePhase.LISTENING)).also { made += it }
                        })
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    private fun txs() = runBlocking { c.repo.loadOnce().txs }

    @Test fun ouvindoComEcoEPalavrasAoVivo() {
        rule.mainClock.autoAdvance = false
        show(VoiceUi(VoicePhase.LISTENING, level = 0.2f))
        rule.mainClock.advanceTimeBy(800)
        val l = made.single()
        assertTrue(l.started)
        // A pessoa fala: sílabas soltam anéis, as palavras aparecem.
        listOf(0.7f to "Mercado", 0.9f to "Mercado 120", 0.6f to "Mercado 120 reais", 0.8f to "Mercado 120 reais no Roxo").forEachIndexed { i, (lv, t) ->
            rule.runOnUiThread {
                l.flow.value = VoiceUi(VoicePhase.LISTENING, level = lv, syllables = i + 1, strength = lv, text = t)
                Snapshot.sendApplyNotifications()
            }
            rule.mainClock.advanceTimeBy(160)
        }
        captureScreenRoboImage("screenshots/58_voz_ouvindo.png")
        rule.mainClock.autoAdvance = true
        rule.onNodeWithText("Pronto").performClick()
        assertTrue(l.finished)
        rule.onNodeWithText("Cancelar").performClick()
        assertTrue(dismissed)
    }

    @Test fun entendeuESalvaUmLancamento() {
        val before = txs().size
        show(VoiceUi(VoicePhase.DONE, text = "Mercado 120 reais no Roxo ontem"))
        captureScreenRoboImage("screenshots/59_voz_entendi.png")
        rule.onNodeWithText("Salvar · ${Money.format(12000)}").performClick()
        waitFor { txs().size == before + 1 }
        val t = txs().maxBy { it.id }
        assertEquals(12000L, t.amount)
        assertEquals(today.minusDays(1), t.date)
        assertTrue("no cartão", t.cardId != null)
        assertTrue(dismissed)
    }

    @Test fun variosLancamentosNumaFala() {
        val before = txs().size
        show(VoiceUi(VoicePhase.DONE, text = "Almoço 30 e Uber 15 no débito"))
        captureScreenRoboImage("screenshots/60_voz_varios.png")
        rule.onNodeWithText("Salvar 2 lançamentos · ${Money.format(4500)}").performClick()
        waitFor { txs().size == before + 2 }
        val new = txs().sortedBy { it.id }.takeLast(2)
        assertEquals(listOf(3000L, 1500L), new.map { it.amount })
        assertEquals(listOf(TxType.EXPENSE, TxType.EXPENSE), new.map { it.type })
    }

    @Test fun naoOuviNadaEFalaDeNovo() {
        show(VoiceUi(VoicePhase.DONE, problem = VoiceProblem.NOTHING))
        captureScreenRoboImage("screenshots/61_voz_nao_ouvi.png")
        rule.onNodeWithText("Falar de novo").performClick()
        rule.waitForIdle()
        assertEquals(2, made.size)
        assertTrue(made[0].cancelled)
        assertTrue(made[1].started)
    }

    @Test fun explicaOMicrofoneAntesDePedir() {
        var allowed = false
        rule.setContent {
            CryoTheme(mode = ThemeMode.LIGHT, dynamicColor = false) { MicPermissionSheet(onDismiss = {}, onAllow = { allowed = true }) }
        }
        rule.waitForIdle()
        captureScreenRoboImage("screenshots/62_voz_microfone.png")
        rule.onNodeWithText("Permitir microfone").performClick()
        assertTrue(allowed)
    }
}
