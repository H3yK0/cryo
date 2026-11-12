// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import io.github.h3yk0.cryo.data.AppSettings
import io.github.h3yk0.cryo.domain.GoalArt
import java.io.File
import java.text.BreakIterator

/** RF-PRF-01: o que a bolinha do perfil mostra. */
sealed interface AvatarKind {
    /** A primeira letra do nome (ou o ícone de pessoa, sem nome). */
    data object Letter : AvatarKind
    data class Drawing(val art: GoalArt) : AvatarKind

    /** Um emoji do teclado, escolhido pela pessoa: é dado dela, como o nome de uma categoria (o app não tem emoji próprio). */
    data class Emoji(val text: String) : AvatarKind

    /** Uma foto guardada só neste aparelho; [stamp] muda quando a foto é trocada. */
    data class Photo(val stamp: String) : AvatarKind
}

object Avatars {
    private const val SIDE = 256
    private const val MAX_EMOJI = 24

    /** Como o ajuste guarda a escolha: "" (letra), "art:<chave>", "emoji:<grafema>" ou "photo:<carimbo>". */
    fun parse(value: String): AvatarKind = when {
        value.startsWith("art:") -> AvatarKind.Drawing(GoalArt.of(value.removePrefix("art:")))
        value.startsWith("emoji:") -> firstGrapheme(value.removePrefix("emoji:")).let { if (it.isEmpty()) AvatarKind.Letter else AvatarKind.Emoji(it) }
        value.startsWith("photo:") -> AvatarKind.Photo(value.removePrefix("photo:"))
        else -> AvatarKind.Letter
    }

    /**
     * Só o primeiro "caractere que a pessoa vê": um emoji de família, de bandeira ou com tom de pele é feito de vários
     * códigos e precisa ficar inteiro. O que passar disso é cortado.
     */
    fun firstGrapheme(text: String): String {
        val t = text.trim()
        if (t.isEmpty()) return ""
        val it = BreakIterator.getCharacterInstance()
        it.setText(t)
        val end = it.next()
        return if (end == BreakIterator.DONE) t.take(MAX_EMOJI) else t.substring(0, end).take(MAX_EMOJI)
    }

    fun photoFile(ctx: Context, sandbox: Boolean): File = File(ctx.filesDir, if (sandbox) "avatar-teste.jpg" else "avatar.jpg")

    /**
     * Guarda a foto escolhida como um quadrado pequeno na área privada do app. O arquivo novo não leva nada do original
     * além da imagem (sai sem o local e a data em que a foto foi tirada). Roda fora da linha principal.
     */
    fun savePhoto(ctx: Context, uri: Uri, sandbox: Boolean): Boolean = runCatching {
        val resolver = ctx.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val shortest = minOf(bounds.outWidth, bounds.outHeight)
        if (shortest <= 0) return false
        var sample = 1
        while (shortest / (sample * 2) >= SIDE) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) } ?: return false
        // A câmera guarda a foto "deitada" e anota o giro à parte: sem aplicar o giro, o rosto sairia de lado.
        val turn = runCatching {
            resolver.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
        }.getOrNull()
        val degrees = when (turn) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        val side = minOf(decoded.width, decoded.height)
        val matrix = Matrix().apply { postRotate(degrees); postScale(SIDE.toFloat() / side, SIDE.toFloat() / side) }
        val small = Bitmap.createBitmap(decoded, (decoded.width - side) / 2, (decoded.height - side) / 2, side, side, matrix, true)
        val file = photoFile(ctx, sandbox)
        val tmp = File(file.path + ".tmp")
        tmp.outputStream().use { small.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        tmp.renameTo(file)
    }.getOrDefault(false)
}

/**
 * A bolinha do perfil (RF-PRF-01). [onHeader]: sobre o cabeçalho colorido do Início, onde o fundo é um véu claro.
 * Sem a foto no aparelho (celular novo, backup restaurado), volta para a letra.
 */
@Composable
fun ProfileAvatar(s: AppSettings, size: Dp, modifier: Modifier = Modifier, onHeader: Boolean = false) {
    val kind = remember(s.avatar) { Avatars.parse(s.avatar) }
    val ctx = LocalContext.current
    val sandbox = LocalContainer.current.sandbox
    val custom = s.avatarColor != 0
    val back = when {
        custom -> Color(s.avatarColor)
        onHeader -> LocalContentColor.current.copy(alpha = 0.16f)
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val front = when {
        custom -> Color.White
        onHeader -> LocalContentColor.current
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    val photo = (kind as? AvatarKind.Photo)?.let { p ->
        remember(p.stamp, sandbox) { runCatching { BitmapFactory.decodeFile(Avatars.photoFile(ctx, sandbox).path)?.asImageBitmap() }.getOrNull() }
    }
    Box(modifier.size(size).clip(CircleShape).background(back), contentAlignment = Alignment.Center) {
        when {
            photo != null -> Image(photo, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            kind is AvatarKind.Drawing -> GoalImage(kind.art, if (custom) Color(s.avatarColor) else MaterialTheme.colorScheme.primary, size)
            kind is AvatarKind.Emoji -> Text(kind.text, fontSize = (size.value * 0.5f).sp)
            else -> {
                val initial = s.userName.trim().firstOrNull()?.uppercaseChar()
                if (initial != null) Text(initial.toString(), color = front, fontSize = (size.value * 0.42f).sp)
                else Icon(Icons.Rounded.Person, null, modifier = Modifier.size(size * 0.55f), tint = front)
            }
        }
    }
}
