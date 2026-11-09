// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import android.graphics.Bitmap
import io.github.h3yk0.cryo.ui.components.Art
import io.github.h3yk0.cryo.ui.components.drawArtTile
import io.github.h3yk0.cryo.ui.components.renderArt
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * RF-ILU-04: as figurinhas animadas das notificações saem do mesmo código das ilustrações. Com -Pstickers=true este teste
 * grava os quadros (app/screenshots/stickers/<nome>/f_NN.png); `tools/stickers/make_stickers.sh` junta cada um num WebP
 * animado em res/raw. Sem a opção, só confere que toda ilustração tem a sua figurinha no app.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = TestApp::class)
class StickerFramesTest {
    @Test fun quadros() {
        assumeTrue(System.getProperty("cryo.stickers") == "true")
        for (art in Art.entries) {
            val dir = File("screenshots/stickers/${art.name.lowercase()}").apply { deleteRecursively(); mkdirs() }
            for (i in 0 until FRAMES) {
                val bmp = renderArt(SIZE) { drawArtTile(art, i / FRAMES.toFloat()) }
                File(dir, "f_%02d.png".format(i)).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
    }

    @Test fun todaIlustracaoTemFigurinha() {
        val raw = File("src/main/res/raw")
        val missing = Art.entries.filter { !File(raw, "sticker_${it.name.lowercase()}.webp").exists() }
        assertTrue("figurinhas faltando: $missing (rode -Pstickers=true e tools/stickers/make_stickers.sh)", missing.isEmpty())
    }

    companion object {
        const val SIZE = 240
        const val FRAMES = 24
    }
}
