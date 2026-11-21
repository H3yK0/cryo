// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import io.github.h3yk0.cryo.data.Backup
import io.github.h3yk0.cryo.data.BackupCrypto
import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.Category
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.Snapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * ERR-063 (auditoria da 1.6): o backup com senha é reconhecido pela chave `"cipher":` do envelope, nunca por uma palavra solta
 * no começo do arquivo. Antes, o backup aberto de quem tinha uma conta chamada "cipher" pedia senha e não abria.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = TestApp::class)
class BackupEnvelopeTest {
    @Test fun backupAbertoComContaChamadaCipherAbre() {
        val s = Snapshot(
            accounts = listOf(Account(1, "cipher", initialBalance = 100, color = 0), Account(2, "\"cipher\":\"AES-256-GCM\"", initialBalance = 0, color = 0)),
            categories = listOf(Category(1, "cipher", CategoryKind.EXPENSE, "lock", 0)),
            txs = listOf(Tx(1, TxType.EXPENSE, 500, LocalDate.of(2026, 11, 1), "cipher\":", accountId = 1, categoryId = 1)),
        )
        val json = Backup.export(s, exportedAt = 1L)
        assertTrue("o começo do arquivo tem a palavra", json.take(600).contains("\"cipher\""))
        assertFalse(BackupCrypto.isEncrypted(json))
        val back = Backup.import(json).snapshot
        assertEquals(listOf("cipher", "\"cipher\":\"AES-256-GCM\""), back.accounts.map { it.name })
        assertEquals("cipher\":", back.txs.single().description)
    }

    @Test fun backupComSenhaContinuaReconhecido() {
        val key = BackupCrypto.deriveKey("senha".toCharArray(), BackupCrypto.newSalt(), 1_000)
        val json = Backup.export(Snapshot(accounts = listOf(Account(1, "Conta", initialBalance = 100, color = 0))), exportedAt = 5L)
        val sealed = BackupCrypto.encrypt(json, key, exportedAt = 5L)
        assertTrue(BackupCrypto.isEncrypted(sealed))
        assertTrue(BackupCrypto.isEncrypted(BackupCrypto.encrypt(json, key, exportedAt = 5L, testMode = true)))
        assertEquals(json, BackupCrypto.decrypt(sealed, "senha".toCharArray()))
        // Um envelope escrito com espaços (outra ferramenta formatou o arquivo) também é reconhecido.
        assertTrue(BackupCrypto.isEncrypted(sealed.replace("\"cipher\":", "\"cipher\" : ")))
    }
}
