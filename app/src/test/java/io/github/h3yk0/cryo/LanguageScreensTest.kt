// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.h3yk0.cryo.data.AppSettings
import io.github.h3yk0.cryo.data.ThemeMode
import io.github.h3yk0.cryo.domain.AppLocale
import io.github.h3yk0.cryo.ui.CryoRoot
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.Navigator
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.screens.InsightsScreen
import io.github.h3yk0.cryo.ui.screens.OnboardingScreen
import io.github.h3yk0.cryo.ui.screens.Tour
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * Imagens das telas principais e do passeio em cada língua (RF-LNG-01, RF-TOUR-04), com a língua e a moeda que o celular de cada país teria.
 * Os dados de exemplo são os mesmos para todas (nomes de contas como a pessoa digitou), só a interface muda.
 * Saem em `app/screenshots/linguas/`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = TestApp::class, qualifiers = "+w393dp-h1500dp-xxhdpi")
class LanguageScreensTest {
    private lateinit var c: AppContainer
    private val today = LocalDate.now()

    @Before
    fun setUp() {
        c = AppContainer(ApplicationProvider.getApplicationContext())
        runBlocking { seed(c, today); c.autoBackup.clear() }
        val deadline = System.currentTimeMillis() + 10_000
        while ((c.repo.snapshot.value?.txs?.size ?: 0) < 50 && System.currentTimeMillis() < deadline) Thread.sleep(50)
    }

    @Composable
    private fun Wrap(onboarding: Boolean, content: @Composable () -> Unit) {
        CryoTheme(mode = ThemeMode.LIGHT, dynamicColor = false) {
            val nav = rememberNavController()
            val navigator = remember { Navigator(nav) }
            val s = AppSettings(onboardingDone = !onboarding, defaultAccountId = 1, userName = "Hanry")
            CompositionLocalProvider(LocalContainer provides c, LocalSettings provides s, LocalNav provides navigator) {
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) { content() }
            }
        }
    }

    private fun shots(lang: String, currency: String) {
        assertEquals(currency, AppLocale.currency.currencyCode)
        captureRoboImage("screenshots/linguas/${lang}_1_inicio.png") { Wrap(false) { CryoRoot(null) } }
        captureRoboImage("screenshots/linguas/${lang}_2_analises.png") { Wrap(false) { InsightsScreen() } }
        captureRoboImage("screenshots/linguas/${lang}_3_boas_vindas.png") { Wrap(true) { OnboardingScreen() } }
        // RF-TOUR-04: o passeio na língua e na moeda do país.
        captureRoboImage("screenshots/linguas/${lang}_4_passeio.png") { Wrap(true) { Tour(lastLabel = "", onFinish = {}, onBackFromFirst = {}) } }
    }

    @Config(qualifiers = "+en-rUS") @Test fun ingles() = shots("en", "USD")
    @Config(qualifiers = "+es-rMX") @Test fun espanhol() = shots("es", "MXN")
    @Config(qualifiers = "+fr-rFR") @Test fun frances() = shots("fr", "EUR")
    @Config(qualifiers = "+de-rDE") @Test fun alemao() = shots("de", "EUR")
    @Config(qualifiers = "+it-rIT") @Test fun italiano() = shots("it", "EUR")
    @Config(qualifiers = "+ja-rJP") @Test fun japones() = shots("ja", "JPY")
    @Config(qualifiers = "+ko-rKR") @Test fun coreano() = shots("ko", "KRW")
    @Config(qualifiers = "+zh-rCN") @Test fun chines() = shots("zh", "CNY")
    @Config(qualifiers = "+hi-rIN") @Test fun hindi() = shots("hi", "INR")
}
