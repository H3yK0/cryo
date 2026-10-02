// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import android.Manifest
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import org.robolectric.Robolectric
import java.io.File
import java.io.FileNotFoundException

/**
 * Um "app de arquivos" de mentira, guardando tudo numa pasta temporária. Funciona como a pasta do celular
 * ou um app de nuvem no seletor do Android, para testar o backup automático de ponta a ponta.
 */
class TestDocsProvider : DocumentsProvider() {
    companion object {
        const val AUTHORITY = "io.github.h3yk0.cryo.testdocs"
        const val ROOT = "root"
        lateinit var dir: File

        /** Simula disco cheio ou nuvem fora do ar: o arquivo abre, mas gravar nele falha. */
        @Volatile var failWrites = false

        fun install(folder: File) {
            dir = folder.also { it.mkdirs() }
            failWrites = false
            val info = ProviderInfo().apply {
                authority = AUTHORITY
                exported = true
                grantUriPermissions = true
                readPermission = Manifest.permission.MANAGE_DOCUMENTS
                writePermission = Manifest.permission.MANAGE_DOCUMENTS
            }
            Robolectric.buildContentProvider(TestDocsProvider::class.java).create(info)
        }
    }

    private val docCols = arrayOf(
        Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE,
        Document.COLUMN_SIZE, Document.COLUMN_LAST_MODIFIED, Document.COLUMN_FLAGS,
    )

    private fun file(id: String): File = if (id == ROOT) dir else File(dir, id.removePrefix("$ROOT/"))

    private fun row(c: MatrixCursor, id: String) {
        val f = file(id)
        if (!f.exists()) throw FileNotFoundException(id)
        val flags = if (f.isDirectory) Document.FLAG_DIR_SUPPORTS_CREATE else Document.FLAG_SUPPORTS_WRITE or Document.FLAG_SUPPORTS_DELETE
        c.addRow(arrayOf(id, if (id == ROOT) "Backups" else f.name, if (f.isDirectory) Document.MIME_TYPE_DIR else "application/json", f.length(), f.lastModified(), flags))
    }

    override fun onCreate() = true

    override fun queryRoots(projection: Array<out String>?): Cursor =
        MatrixCursor(arrayOf(Root.COLUMN_ROOT_ID, Root.COLUMN_DOCUMENT_ID, Root.COLUMN_TITLE, Root.COLUMN_FLAGS)).apply {
            addRow(arrayOf(ROOT, ROOT, "Teste", Root.FLAG_SUPPORTS_CREATE or Root.FLAG_SUPPORTS_IS_CHILD))
        }

    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor =
        MatrixCursor(docCols).also { row(it, documentId) }

    override fun queryChildDocuments(parentDocumentId: String, projection: Array<out String>?, sortOrder: String?): Cursor {
        val parent = file(parentDocumentId)
        if (!parent.isDirectory) throw FileNotFoundException(parentDocumentId)
        return MatrixCursor(docCols).also { c -> parent.listFiles()?.sortedBy { it.name }?.forEach { row(c, "$ROOT/${it.name}") } }
    }

    override fun openDocument(documentId: String, mode: String, signal: CancellationSignal?): ParcelFileDescriptor {
        val f = file(documentId)
        if (!f.parentFile!!.isDirectory) throw FileNotFoundException(documentId)
        if (failWrites && mode.contains('w')) return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY)
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.parseMode(mode))
    }

    override fun createDocument(parentDocumentId: String, mimeType: String, displayName: String): String {
        val parent = file(parentDocumentId)
        if (!parent.isDirectory) throw FileNotFoundException(parentDocumentId)
        var f = File(parent, displayName)
        var n = 1
        while (f.exists()) f = File(parent, displayName.replace(".json", " (${n++}).json"))
        f.createNewFile()
        return "$ROOT/${f.name}"
    }

    override fun deleteDocument(documentId: String) {
        if (!file(documentId).delete()) throw FileNotFoundException(documentId)
    }

    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean =
        documentId == parentDocumentId || documentId.startsWith("$parentDocumentId/")
}
