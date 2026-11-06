// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.h3yk0.cryo.data.AppSettings
import io.github.h3yk0.cryo.data.DefaultData
import io.github.h3yk0.cryo.data.ThemeMode
import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.AccountType
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.ParseContext
import io.github.h3yk0.cryo.domain.PhraseParser
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.ui.CryoRoot
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.Navigator
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.screens.SCENE_MS
import io.github.h3yk0.cryo.ui.screens.TOUR_PAGES
import io.github.h3yk0.cryo.ui.screens.Tour
import io.github.h3yk0.cryo.ui.screens.TourExamples
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

/**
 * O passeio "Como usar o Cryo" (PRD: RF-TOUR). Imagens das páginas em `app/screenshots/47..52_passeio_*.png`.
 * Com `-PtourGif=true`, também os quadros das animações (`app/screenshots/passeio_gif/`), para montar GIFs.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = TestApp::class, qualifiers = "+w393dp-h852dp-xxhdpi")
class TourTest {
    @get:Rule val rule = createComposeRule()

    private val titles = listOf("Registre em segundos", "Saiba como o mês vai terminar", "Cartões e parcelas sem susto", "Planeje e não esqueça nada", "Seus dados são seus")

    private fun waitText(t: String, ms: Long = 8000) =
        rule.waitUntil(ms) { rule.onAllNodesWithText(t, substring = true).fetchSemanticsNodes().isNotEmpty() }

    @Composable
    private fun Wrap(dark: Boolean = false, content: @Composable () -> Unit) {
        val c = remember { AppContainer(ApplicationProvider.getApplicationContext()) }
        CryoTheme(mode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, dynamicColor = false) {
            val nav = rememberNavController()
            val navigator = remember { Navigator(nav) }
            CompositionLocalProvider(
                LocalContainer provides c, LocalSettings provides AppSettings(onboardingDone = true, defaultAccountId = 1),
                LocalNav provides navigator,
            ) {
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) { content() }
            }
        }
    }

    /** RF-TOUR-01, RF-TOUR-05: as 5 páginas, com "Próximo", e a última com o botão pedido. Uma imagem de cada (DS-PAD-07). */
    @Test fun cincoPaginasComImagens() {
        var done = 0
        rule.setContent { Wrap { Tour(lastLabel = "Vamos lá", onFinish = { done++ }, onBackFromFirst = {}, onSkip = {}) } }
        assertEquals(5, TOUR_PAGES)
        titles.forEachIndexed { i, title ->
            waitText(title)
            rule.onNodeWithContentDescription("Página ${i + 1} de 5").assertExists()
            rule.waitForIdle()
            rule.onRoot().captureRoboImage("screenshots/${47 + i}_passeio_${i + 1}.png")
            if (i < 4) rule.onNodeWithText("Próximo").performClick()
        }
        // Na última página não há "Pular": só o botão de terminar.
        assertEquals(0, rule.onAllNodesWithText("Pular").fetchSemanticsNodes().size)
        rule.onNodeWithText("Vamos lá").performClick()
        assertEquals(1, done)
    }

    @Test fun passeioEscuro() {
        rule.setContent { Wrap(dark = true) { Tour(lastLabel = "Entendi", onFinish = {}, onBackFromFirst = {}, initialPage = 2) } }
        waitText(titles[2])
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("screenshots/52_passeio_escuro.png")
    }

    /** RF-TOUR-03, RF-AJU-11: Ajustes › Como usar o Cryo abre o passeio; "Entendi" volta aos Ajustes. */
    @Test fun passeioPelosAjustes() {
        rule.setContent { Wrap { CryoRoot(null) } }
        waitText("Saldo em contas")
        rule.onNodeWithContentDescription("Ajustes").performClick()
        rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Como usar o Cryo"))
        rule.onNodeWithText("Como usar o Cryo").performClick()
        waitText(titles[0])
        repeat(4) { rule.onNodeWithText("Próximo").performClick() }
        waitText(titles[4])
        rule.onNodeWithText("Entendi").performClick()
        rule.waitUntil(8000) { rule.onAllNodesWithText("Entendi").fetchSemanticsNodes().isEmpty() }
        waitText("Como usar o Cryo")
    }

    /** Quadros das animações para GIFs (só com -PtourGif=true). */
    @Test fun quadrosDasAnimacoes() {
        assumeTrue(System.getProperty("cryo.tourGif") == "true")
        val page = mutableIntStateOf(0)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            Wrap { key(page.intValue) { Tour(lastLabel = "Vamos lá", onFinish = {}, onBackFromFirst = {}, initialPage = page.intValue) } }
        }
        val dir = File("screenshots/passeio_gif").apply { mkdirs() }
        for (p in 0 until TOUR_PAGES) {
            page.intValue = p
            rule.mainClock.advanceTimeByFrame()
            var ms = 0
            var frame = 0
            while (ms <= SCENE_MS + 400) {
                rule.onRoot().captureRoboImage(File(dir, "p${p + 1}_%03d.png".format(frame++)).path)
                rule.mainClock.advanceTimeBy(80)
                ms += 80
            }
        }
    }
}

/**
 * RF-TOUR-04: as frases do passeio, na língua e na moeda de cada país, são entendidas pelo registro rápido daquela língua:
 * o almoço vira um gasto de alimentação na conta principal, e a TV vira 10 parcelas no cartão.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = TestApp::class)
class TourPhrasesTest {
    private fun check() {
        val cats = DefaultData.categories().mapIndexed { i, c -> c.copy(id = i + 1L) }
        val ctx = ParseContext(
            today = LocalDate.of(2026, 11, 5),
            accounts = listOf(
                Account(id = 1, name = Res.s(R.string.wallet_conta_principal), type = AccountType.CHECKING, color = 0),
                Account(id = 2, name = Res.s(R.string.wallet_carteira), type = AccountType.CASH, color = 0),
            ),
            cards = listOf(CreditCard(id = 1, name = "Visa", limitAmount = 10_000_000, closingDay = 3, dueDay = 10, color = 0)),
            categories = cats, defaultAccountId = 1,
        )
        val lunch = PhraseParser(ctx).parse(TourExamples.lunchPhrase())
        val what = "\"${TourExamples.lunchPhrase()}\""
        assertEquals(what, TxType.EXPENSE, lunch.type)
        assertEquals(what, TourExamples.lunch(), lunch.amount)
        assertEquals(what, 1L, lunch.accountId)
        assertNull(what, lunch.cardId)
        assertEquals(what, Res.s(R.string.cat_restaurant_name), cats.firstOrNull { it.id == lunch.categoryId }?.name)

        val tv = PhraseParser(ctx).parse(TourExamples.tvPhrase())
        val what2 = "\"${TourExamples.tvPhrase()}\""
        assertEquals(what2, TxType.EXPENSE, tv.type)
        assertEquals(what2, TourExamples.tv(), tv.amount)
        assertEquals(what2, 10, tv.installments)
        assertEquals(what2, 1L, tv.cardId)
    }

    @Test fun portugues() = check()
    @Config(qualifiers = "+en-rUS") @Test fun ingles() = check()
    @Config(qualifiers = "+en-rGB") @Test fun inglesLibra() = check()
    @Config(qualifiers = "+es-rMX") @Test fun espanhol() = check()
    @Config(qualifiers = "+es-rES") @Test fun espanholEuro() = check()
    @Config(qualifiers = "+fr-rFR") @Test fun frances() = check()
    @Config(qualifiers = "+de-rDE") @Test fun alemao() = check()
    @Config(qualifiers = "+it-rIT") @Test fun italiano() = check()
    @Config(qualifiers = "+ja-rJP") @Test fun japones() = check()
    @Config(qualifiers = "+ko-rKR") @Test fun coreano() = check()
    @Config(qualifiers = "+zh-rCN") @Test fun chines() = check()
    @Config(qualifiers = "+hi-rIN") @Test fun hindi() = check()

    /** Os valores dos exemplos ficam redondos e na escala de cada moeda. */
    @Test fun valoresRedondos() {
        assertEquals(130_000L, TourExamples.nice(130_000L))
        assertEquals(98_000L, TourExamples.nice(97_500L))
        assertEquals(6_500L, TourExamples.nice(6_500L))
        assertEquals(4_800_000L, TourExamples.nice(4_800_000L))
        assertEquals(120_000L, TourExamples.lunch("JPY"))
        assertEquals(3_250L, TourExamples.lunch("BRL"))
        // Como a pessoa digitaria: sem separador de milhar e sem ",00".
        assertEquals("32,50", io.github.h3yk0.cryo.domain.Money.typed(3_250))
        assertEquals("1300", io.github.h3yk0.cryo.domain.Money.typed(130_000))
    }
}
