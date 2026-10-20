// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.importer

import android.content.Context
import android.graphics.pdf.LoadParams
import android.graphics.pdf.PdfRenderer
import android.graphics.pdf.PdfRendererPreV
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
import java.io.File

/**
 * Texto de um PDF com o leitor de PDF do próprio Android (sem bibliotecas de fora: o app continua pequeno).
 * Ele lê texto no Android 15 ou mais novo, e também no 12 a 14 quando o sistema recebeu a atualização desse leitor.
 */
object PdfText {
    /** O PDF tem senha e ela não foi informada (ou está errada). */
    class PasswordNeeded : Exception()

    private const val TOO_OLD =
        "Para ler PDF, o Cryo precisa de um Android mais novo. Peça ao banco o extrato em OFX, CSV ou planilha (XLSX)."

    fun isSupported(): Boolean = Build.VERSION.SDK_INT >= 35 ||
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 13

    /** Texto de cada página. */
    fun extract(context: Context, bytes: ByteArray, password: String?): List<String> {
        if (!isSupported()) throw StatementException(TOO_OLD)
        // O leitor precisa de um arquivo de verdade (uma nuvem pode entregar só um "cano" de dados).
        val tmp = File.createTempFile("extrato", ".pdf", context.cacheDir)
        try {
            tmp.writeBytes(bytes)
            ParcelFileDescriptor.open(tmp, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                return when {
                    Build.VERSION.SDK_INT >= 35 -> modern(pfd, password)
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 13 -> preV(pfd, password)
                    else -> throw StatementException(TOO_OLD)
                }
            }
        } catch (e: SecurityException) {
            throw PasswordNeeded()
        } catch (e: PasswordNeeded) {
            throw e
        } catch (e: StatementException) {
            throw e
        } catch (e: Exception) {
            throw StatementException("Não consegui abrir esse PDF. Ele pode estar estragado.")
        } finally {
            tmp.delete()
        }
    }

    @RequiresApi(35)
    private fun modern(pfd: ParcelFileDescriptor, password: String?): List<String> {
        val renderer = password?.takeIf { it.isNotEmpty() }
            ?.let { PdfRenderer(pfd, LoadParams.Builder().setPassword(it).build()) } ?: PdfRenderer(pfd)
        return renderer.use { r ->
            (0 until r.pageCount).map { i -> r.openPage(i).use { page -> page.textContents.joinToString("\n") { it.text } } }
        }
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
    private fun preV(pfd: ParcelFileDescriptor, password: String?): List<String> {
        val renderer = password?.takeIf { it.isNotEmpty() }
            ?.let { PdfRendererPreV(pfd, LoadParams.Builder().setPassword(it).build()) } ?: PdfRendererPreV(pfd)
        return renderer.use { r ->
            (0 until r.pageCount).map { i -> r.openPage(i).use { page -> page.textContents.joinToString("\n") { it.text } } }
        }
    }
}
