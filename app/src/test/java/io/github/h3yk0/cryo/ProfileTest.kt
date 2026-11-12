// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureScreenRoboImage
import io.github.h3yk0.cryo.data.AppSettings
import io.github.h3yk0.cryo.data.ThemeMode
import io.github.h3yk0.cryo.domain.GoalArt
import io.github.h3yk0.cryo.ui.CryoRoot
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.Navigator
import io.github.h3yk0.cryo.ui.components.AvatarKind
import io.github.h3yk0.cryo.ui.components.Avatars
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalFixedToday
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.screens.ProfileScreen
import io.github.h3yk0.cryo.ui.theme.CryoTheme
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
import java.time.LocalDate

/** RF-PRF-01..04: a bolinha do perfil, o painel que ela abre e as imagens 86–88. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = TestApp::class, qualifiers = "+w393dp-h852dp-xxhdpi")
class ProfileTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var c: AppContainer
    private val day = LocalDate.now()

    @Before fun setUp() {
        io.github.h3yk0.cryo.domain.engine.EngineRunner.inline = true
        c = AppContainer(ApplicationProvider.getApplicationContext())
        runBlocking { seed(c, day) }
        rule.waitUntil(10_000) { (c.repo.snapshot.value?.txs?.size ?: 0) > 50 }
    }

    private fun show(settings: AppSettings, dark: Boolean = false, content: @Composable () -> Unit) {
        rule.setContent {
            CryoTheme(mode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, dynamicColor = false) {
                val nav = rememberNavController()
                val navigator = remember { Navigator(nav) }
                CompositionLocalProvider(LocalContainer provides c, LocalSettings provides settings, LocalNav provides navigator, LocalFixedToday provides day) {
                    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) { content() }
                }
            }
        }
        rule.waitForIdle()
    }

    private fun waitText(t: String) = rule.waitUntil(10_000) { rule.onAllNodesWithText(t, substring = true).fetchSemanticsNodes().isNotEmpty() }

    /** O que os ajustes guardam vira a bolinha certa; texto estranho ou vazio volta para a letra. */
    @Test fun oQueABolinhaMostra() {
        assertEquals(AvatarKind.Letter, Avatars.parse(""))
        assertEquals(AvatarKind.Letter, Avatars.parse("qualquer coisa"))
        assertEquals(AvatarKind.Drawing(GoalArt.PLANE), Avatars.parse("art:plane"))
        assertEquals(AvatarKind.Photo("123"), Avatars.parse("photo:123"))
        assertEquals(AvatarKind.Letter, Avatars.parse("emoji:"))
        // Um emoji só, inteiro: bandeira e família são vários códigos e não podem ser cortados no meio; o resto sai.
        assertEquals("\uD83E\uDD8A", Avatars.firstGrapheme("\uD83E\uDD8A\uD83D\uDC36abc"))
        assertEquals("\uD83C\uDDE7\uD83C\uDDF7", Avatars.firstGrapheme("\uD83C\uDDE7\uD83C\uDDF7\uD83C\uDDFA\uD83C\uDDF8"))
        assertEquals("", Avatars.firstGrapheme("   "))
        assertTrue(Avatars.firstGrapheme("x".repeat(200)).length == 1)
    }

    @Test fun painelDoPerfil() {
        show(AppSettings(onboardingDone = true, defaultAccountId = 1, dynamicColor = false, userName = "Marina", avatar = "art:plane", avatarColor = 0xFF00897B.toInt())) { ProfileScreen() }
        waitText("Olá, Marina")
        waitText("Todos os ajustes")
        captureScreenRoboImage("screenshots/86_perfil.png")
        // Editar perfil: nome, bolinha (letra, desenhos, emoji, foto) e cor.
        rule.onNodeWithText("Editar perfil").performClick()
        waitText("Como quer ser chamado")
        waitText("Desenhos")
        captureScreenRoboImage("screenshots/87_editar_perfil.png")
    }

    @Test fun painelDoPerfilEscuro() {
        show(AppSettings(onboardingDone = true, defaultAccountId = 1, dynamicColor = false, userName = "Marina", hideValues = true), dark = true) { ProfileScreen() }
        waitText("Olá, Marina")
        captureScreenRoboImage("screenshots/88_perfil_escuro.png")
    }

    /** RF-PRF-02: no Início, a bolinha é um botão com descrição e abre o perfil; a engrenagem saiu. */
    @Test fun aBolinhaDoInicioAbreOPerfil() {
        show(AppSettings(onboardingDone = true, defaultAccountId = 1, dynamicColor = false, userName = "Marina", avatar = "emoji:\uD83E\uDD8A")) { CryoRoot(null) }
        waitText("Saldo em contas")
        rule.onNodeWithContentDescription("Perfil e ajustes").assertHasClickAction().performClick()
        waitText("Todos os ajustes")
        assertTrue(rule.onAllNodesWithText("Bloqueio do app").fetchSemanticsNodes().isNotEmpty())
    }
}
