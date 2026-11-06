// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import io.github.h3yk0.cryo.data.AppSettings
import io.github.h3yk0.cryo.data.ThemeMode
import io.github.h3yk0.cryo.ui.CryoRoot
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.Navigator
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalFixedToday
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.screens.CardDetailScreen
import io.github.h3yk0.cryo.ui.screens.DebtsScreen
import io.github.h3yk0.cryo.ui.screens.InsightsScreen
import io.github.h3yk0.cryo.ui.screens.PlanScreen
import io.github.h3yk0.cryo.ui.screens.TransactionsScreen
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

/**
 * Gera o ícone e o banner da página do app no F-Droid (pasta fastlane/).
 * Só roda quando pedido:  ./gradlew testDebugUnitTest --tests '*StoreAssetsTest' -PstoreAssets=true
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = TestApp::class, qualifiers = "+w360dp-h780dp-xxhdpi")
class StoreAssetsTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val images = File("../fastlane/metadata/android/en-US/images")

    @Before
    fun setUp() {
        assumeTrue(System.getProperty("cryo.storeAssets") == "true")
    }

    // As imagens das telas, em cada língua, saem do StoreScreenshotsTest (RF-LNG-10).

    /** Ícone 512×512 a partir do mesmo desenho do ícone do app (cantos arredondados, fundo transparente). */
    @Test fun icone() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val full = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(full)
        // O ícone adaptativo tem 108 unidades; a parte visível é o centro de 72. 108/72 × 512 = 768 px.
        listOf(R.drawable.ic_launcher_background, R.drawable.ic_launcher_foreground).forEach { id ->
            ContextCompat.getDrawable(ctx, id)!!.apply { setBounds(-128, -128, 640, 640) }.draw(canvas)
        }
        val out = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val oc = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        oc.drawRoundRect(RectF(0f, 0f, 512f, 512f), 116f, 116f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        oc.drawBitmap(full, 0f, 0f, paint)
        images.mkdirs()
        FileOutputStream(File(images, "icon.png")).use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Config(qualifiers = "+w1024dp-h500dp-mdpi")
    @Test fun bannerEn() = banner(images, "Your money, crystal clear.", "Personal finance · voice entry · works offline")

    @Config(qualifiers = "+w1024dp-h500dp-mdpi")
    @Test fun bannerPt() = banner(
        File("../fastlane/metadata/android/pt-BR/images"),
        "Seu dinheiro, claro como gelo.", "Finanças pessoais · registro por voz · sem internet",
    )

    private fun banner(dir: File, tagline: String, sub: String) {
        dir.mkdirs()
        rule.setContent {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(Color(0xFF0A4F66), Color(0xFF29B6D6)), Offset.Zero, Offset.Infinite)),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(Modifier.padding(horizontal = 72.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_cryo_mark), null, tint = Color.White, modifier = Modifier.size(168.dp))
                    Spacer(Modifier.width(48.dp))
                    Column {
                        Text("Cryo", color = Color.White, fontSize = 104.sp, fontWeight = FontWeight.Bold, lineHeight = 108.sp)
                        Text(tagline, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(18.dp))
                        Text(sub, color = Color.White.copy(alpha = 0.85f), fontSize = 24.sp)
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage(File(dir, "featureGraphic.png").path)
    }
}
