// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureScreenRoboImage
import io.github.h3yk0.cryo.data.AppSettings
import io.github.h3yk0.cryo.data.GuideStore
import io.github.h3yk0.cryo.data.ThemeMode
import io.github.h3yk0.cryo.domain.GuideCount
import io.github.h3yk0.cryo.domain.GuidePlace
import io.github.h3yk0.cryo.domain.GuidePlanner
import io.github.h3yk0.cryo.domain.Lesson
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.ui.CryoRoot
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalFixedToday
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.components.guideFacts
import io.github.h3yk0.cryo.ui.components.titleRes
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/** RF-GUI-01, 05, 06, 07: o caderninho do guia, o cartão da dica, o destaque e a tela "Aprender o Cryo" (imagens 89–91). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = TestApp::class, qualifiers = "+w393dp-h852dp-xxhdpi")
class GuideFlowTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var c: AppContainer
    private val app: Application get() = ApplicationProvider.getApplicationContext()
    private val day = LocalDate.now()
    private val settings = AppSettings(onboardingDone = true, defaultAccountId = 1, dynamicColor = false, userName = "Marina")

    @Before fun setUp() {
        io.github.h3yk0.cryo.domain.engine.EngineRunner.inline = true
        c = AppContainer(app)
        // O DataStore não zera entre testes: o backup automático de outro teste mudaria qual dica vem primeiro.
        runBlocking { c.autoBackup.clear(); seed(c, day) }
        rule.waitUntil(10_000) { (c.repo.snapshot.value?.txs?.size ?: 0) > 50 }
    }

    /** Liga o guia como num celular em uso: a chave ligada e o app aberto pela primeira vez há um bom tempo. */
    private fun guideOn() {
        app.getSharedPreferences(GuideStore.FILE, Context.MODE_PRIVATE).edit().putLong("first", System.currentTimeMillis() - 3_600_000L).commit()
        c.guide.enabled = true
    }

    private fun showApp() {
        rule.setContent {
            CryoTheme(mode = ThemeMode.LIGHT, dynamicColor = false) {
                CompositionLocalProvider(LocalContainer provides c, LocalSettings provides settings, LocalFixedToday provides day) {
                    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) { CryoRoot(null) }
                }
            }
        }
        rule.waitForIdle()
    }

    private fun has(t: String) = rule.onAllNodesWithText(t, substring = true).fetchSemanticsNodes().isNotEmpty()
    private fun waitText(t: String) = rule.waitUntil(10_000) { has(t) }
    private fun waitGone(t: String) = rule.waitUntil(10_000) { !has(t) }

    /** RF-GUI-07: nos testes (e com a chave desligada) nenhuma dica aparece e nada é anotado como mostrado. */
    @Test fun desligadoOGuiaNaoFala() {
        assertFalse("o TestApp desliga o guia", c.guide.enabled)
        showApp()
        waitText("Saldo em contas")
        assertFalse(has("Dica do Cryo"))
        assertTrue(c.guide.log().tipDays.isEmpty())
    }

    /** RF-GUI-03/05: a dica sobe acima da barra, conta para o dia na hora, e "Agora não" a fecha sem outra em seguida. */
    @Test fun aDicaApareceEAgoraNaoFecha() {
        guideOn()
        val expected = runBlocking { GuidePlanner.next(guideFacts(c, day), c.guide.log(), day, GuidePlace.HOME) }!!
        // Com os dados de exemplo (muitos lançamentos e nenhuma cópia), quem fala primeiro é o backup.
        assertEquals(Lesson.BACKUP, expected)
        showApp()
        waitText("Dica do Cryo")
        assertTrue(has(Res.s(expected.titleRes())))
        assertEquals(listOf(day.toEpochDay()), c.guide.log().tipDays)
        assertEquals(1, c.guide.log().lessons[expected]?.shown)
        captureScreenRoboImage("screenshots/89_dica_do_guia.png")
        rule.onNodeWithText("Agora não").performClick()
        waitGone("Dica do Cryo")
        assertEquals(1, c.guide.log().laterStreak)
        // Trocar de aba no mesmo dia não traz outra dica.
        rule.onNodeWithText("Extrato").performClick()
        rule.waitForIdle()
        assertFalse(has("Dica do Cryo"))
        assertEquals(1, c.guide.log().tipDays.size)
    }

    /** RF-GUI-05: Voltar fecha a dica (vale como "Agora não") e o app continua onde estava. */
    @Test fun voltarFechaADica() {
        guideOn()
        showApp()
        waitText("Dica do Cryo")
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        waitGone("Dica do Cryo")
        assertEquals(1, c.guide.log().laterStreak)
        assertTrue(has("Saldo em contas"))
    }

    /** RF-GUI-05: "Me mostra" dá a lição como vista e contorna o ponto na tela; um toque fecha o destaque. */
    @Test fun meMostraDestacaOPonto() {
        guideOn()
        // O backup já foi visto: agora a vez é da primeira lição do primeiro degrau, que mora no Início.
        c.guide.markSeen(Lesson.BACKUP)
        showApp()
        waitText("Dica do Cryo")
        val title = Res.s(Lesson.PHRASE.titleRes())
        assertTrue(has(title))
        rule.onNodeWithText("Me mostra").performClick()
        waitGone("Dica do Cryo")
        assertTrue(c.guide.log().lessons[Lesson.PHRASE]?.seen == true)
        rule.waitUntil(10_000) { rule.onAllNodesWithContentDescription(title).fetchSemanticsNodes().isNotEmpty() }
        captureScreenRoboImage("screenshots/90_destaque_do_guia.png")
        rule.onNodeWithContentDescription(title).assertHasClickAction().performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithContentDescription(title).fetchSemanticsNodes().isEmpty() }
        assertTrue(has("Saldo em contas"))
    }

    /** RF-GUI-06: pelo perfil, a lista inteira com o que já se usa marcado; tocar numa lição a mostra de novo. */
    @Test fun aprenderOCryoPeloPerfil() {
        showApp()
        waitText("Saldo em contas")
        rule.onNodeWithContentDescription("Perfil e ajustes").performClick()
        waitText("Todos os ajustes")
        rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Aprender o Cryo"))
        rule.onNodeWithText("Aprender o Cryo").performClick()
        waitText("Você já conhece")
        waitText("Dicas do Cryo")
        captureScreenRoboImage("screenshots/91_aprender_o_cryo.png")
        val before = runBlocking { GuidePlanner.progress(guideFacts(c, day), c.guide.log()) }
        assertTrue("os dados de exemplo já usam várias coisas", before in 3 until GuidePlanner.total)
        // A lição da voz: volta para o Início e contorna o botão do meio da barra.
        val voice = Res.s(Lesson.VOICE.titleRes())
        rule.onNodeWithText(voice).performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithContentDescription(voice).fetchSemanticsNodes().isNotEmpty() }
        assertTrue(c.guide.log().lessons[Lesson.VOICE]?.seen == true)
        assertEquals(before + 1, runBlocking { GuidePlanner.progress(guideFacts(c, day), c.guide.log()) })
        rule.onNodeWithContentDescription(voice).performClick()
        waitText("Saldo em contas")
        // O Início ficou limpo: ir a outra aba e voltar não reabre a lista.
        rule.onNodeWithText("Extrato").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Início").performClick()
        waitText("Saldo em contas")
        assertFalse(has("Você já conhece"))
    }

    /** RF-GUI-01: o caderninho só tem chaves de lição e de tela, números, dias e sim/não. Nenhum valor, nome ou descrição. */
    @Test fun oCaderninhoSoGuardaContagensEDias() {
        val prefs = app.getSharedPreferences(GuideStore.FILE, Context.MODE_PRIVATE)
        val store = GuideStore(prefs)
        store.enabled = true
        assertEquals(0L, store.minutesSinceFirstOpen(1_000_000L))
        assertEquals(3L, store.minutesSinceFirstOpen(1_000_000L + 3 * 60_000L))
        store.markShown(Lesson.BACKUP, day)
        store.markLater(day)
        store.markSeen(Lesson.VOICE)
        store.markMoment(day)
        repeat(3) { store.count(GuideCount.INSIGHTS) }
        store.count(GuideCount.PHRASE)
        // O que foi escrito é o que se lê de volta.
        val g = store.log()
        assertEquals(1, g.lessons[Lesson.BACKUP]?.shown)
        assertEquals(day.toEpochDay(), g.lessons[Lesson.BACKUP]?.day)
        assertTrue(g.lessons[Lesson.VOICE]?.seen == true)
        assertEquals(listOf(day.toEpochDay()), g.tipDays)
        assertEquals(day.toEpochDay(), g.momentDay)
        assertEquals(3, g.counts[GuideCount.INSIGHTS])
        assertEquals(1, g.counts[GuideCount.PHRASE])
        // "Me mostra" zera a conta dos "Agora não" seguidos.
        assertEquals(0, g.laterStreak)
        val key = Regex("^(enabled|first|tips|streak|paused|moment|[nds]_[A-Z_]+|c_[a-z]+)$")
        for ((k, v) in prefs.all) {
            assertTrue("chave estranha: $k", key.matches(k))
            when (v) {
                is Boolean, is Int, is Long -> Unit
                is String -> assertTrue("texto em $k", k == "tips" && Regex("^[0-9,]*$").matches(v))
                else -> throw AssertionError("tipo estranho em $k")
            }
        }
    }
}
