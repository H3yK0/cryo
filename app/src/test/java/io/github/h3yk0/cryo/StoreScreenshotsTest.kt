// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.h3yk0.cryo.data.AppSettings
import io.github.h3yk0.cryo.data.ThemeMode
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.PhraseParser
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.ui.CryoRoot
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.Navigator
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalFixedToday
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.screens.CardDetailScreen
import io.github.h3yk0.cryo.ui.screens.InsightsScreen
import io.github.h3yk0.cryo.ui.screens.PlanScreen
import io.github.h3yk0.cryo.ui.screens.TourExamples
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import io.github.h3yk0.cryo.ui.tx.TxDraft
import io.github.h3yk0.cryo.ui.tx.TxForm
import io.github.h3yk0.cryo.ui.tx.parseContext
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

/**
 * RF-LNG-10: as imagens das telas na página da loja (F-Droid) e no README de cada língua (RF-LNG-09), com os dados de
 * exemplo na língua e na moeda de cada país. Saem em `fastlane/metadata/android/<língua>/images/phoneScreenshots/`.
 * Só roda quando pedido:  ./gradlew testDebugUnitTest --tests '*StoreScreenshotsTest' -PstoreAssets=true
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = TestApp::class, qualifiers = "+w360dp-h780dp-xhdpi")
class StoreScreenshotsTest {
    private lateinit var c: AppContainer
    private val day = LocalDate.of(2026, 9, 25)
    private val settings = AppSettings(onboardingDone = true, defaultAccountId = 1, dynamicColor = false)

    @Before
    fun setUp() {
        assumeTrue(System.getProperty("cryo.storeAssets") == "true")
        c = AppContainer(ApplicationProvider.getApplicationContext())
        runBlocking { seed(c, day); c.autoBackup.clear() }
        val deadline = System.currentTimeMillis() + 10_000
        while ((c.repo.snapshot.value?.txs?.size ?: 0) < 50 && System.currentTimeMillis() < deadline) Thread.sleep(50)
    }

    @Composable
    private fun Wrap(dark: Boolean, content: @Composable () -> Unit) {
        CryoTheme(mode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, dynamicColor = false) {
            val nav = rememberNavController()
            val navigator = remember { Navigator(nav) }
            CompositionLocalProvider(
                LocalContainer provides c, LocalSettings provides settings, LocalNav provides navigator, LocalFixedToday provides day,
            ) {
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) { content() }
            }
        }
    }

    /**
     * O registro rápido: a folha de conferência (DS-PAD-03) por cima do Início, com a frase da TV parcelada na língua e na
     * moeda da página. A folha de verdade é uma janela própria (que a imagem da tela não pega): aqui ela é montada com as
     * mesmas peças (título, texto e o formulário TxForm).
     */
    @Composable
    private fun QuickSheet() {
        val l = Ledger(c.repo.snapshot.value!!, day)
        val d = remember { TxDraft.fromParsed(PhraseParser(l.parseContext(settings)).parse(TourExamples.tvPhrase())) }
        Box(Modifier.fillMaxSize()) {
            CryoRoot(null)
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.86f),
            ) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                    Box(
                        Modifier.align(Alignment.CenterHorizontally).padding(vertical = 22.dp).size(width = 32.dp, height = 4.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape),
                    )
                    Text(Res.s(R.string.quick_confira_lancamento), style = MaterialTheme.typography.titleLarge)
                    Text(
                        Res.s(R.string.quick_foi_isso_que_eu), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    TxForm(d, l, compact = true)
                }
            }
        }
    }

    private fun shots(store: String) {
        val dir = File("../fastlane/metadata/android/$store/images/phoneScreenshots").apply { deleteRecursively(); mkdirs() }
        fun shot(n: Int, dark: Boolean = false, content: @Composable () -> Unit) =
            captureRoboImage(File(dir, "$n.png").path) { Wrap(dark) { content() } }
        shot(1) { CryoRoot(null) }
        shot(2) { QuickSheet() }
        shot(3) { InsightsScreen() }
        shot(4) { PlanScreen(0) }
        shot(5) { CardDetailScreen(1) }
        shot(6, dark = true) { CryoRoot(null) }
    }

    @Test fun portugues() = shots("pt-BR")
    @Config(qualifiers = "+en-rUS") @Test fun ingles() = shots("en-US")
    @Config(qualifiers = "+es-rES") @Test fun espanhol() = shots("es-ES")
    @Config(qualifiers = "+fr-rFR") @Test fun frances() = shots("fr-FR")
    @Config(qualifiers = "+de-rDE") @Test fun alemao() = shots("de-DE")
    @Config(qualifiers = "+it-rIT") @Test fun italiano() = shots("it-IT")
    @Config(qualifiers = "+ja-rJP") @Test fun japones() = shots("ja-JP")
    @Config(qualifiers = "+ko-rKR") @Test fun coreano() = shots("ko-KR")
    @Config(qualifiers = "+zh-rCN") @Test fun chines() = shots("zh-CN")
    @Config(qualifiers = "+hi-rIN") @Test fun hindi() = shots("hi-IN")
}
