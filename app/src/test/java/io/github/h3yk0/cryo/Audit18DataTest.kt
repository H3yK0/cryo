// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import androidx.biometric.BiometricPrompt
import androidx.test.core.app.ApplicationProvider
import io.github.h3yk0.cryo.data.Backup
import io.github.h3yk0.cryo.data.BackupCrypto
import io.github.h3yk0.cryo.data.BackupProtection
import io.github.h3yk0.cryo.data.Vault
import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.Bill
import io.github.h3yk0.cryo.data.db.Category
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Snapshot
import io.github.h3yk0.cryo.domain.key
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

/**
 * Auditoria da 1.8, a parte que precisa do banco e dos arquivos (RF-SEG-03/08, RF-BLQ): backup protegido que nunca sai
 * aberto, planilha sem fórmulas, backup estragado recusado com mensagem clara, "Paguei" duas vezes = um pagamento,
 * e erro do sensor que não destrava. Cada teste cobre uma falha confirmada na 1.7.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = TestApp::class)
class Audit18DataTest {
    private val today = LocalDate.of(2026, 11, 10)
    private val nov = YearMonth.of(2026, 11)
    private val ctx get() = ApplicationProvider.getApplicationContext<android.app.Application>()

    @After fun tearDown() { Vault.softwareWrapForTests = false }

    private fun container() = AppContainer(ctx, sandbox = true).also { runBlocking { it.repo.eraseAll() } }

    /* ---------------------------- backup com senha ---------------------------- */

    @Test fun protecaoLigadaSemCofreNaoGravaBackupAberto() {
        Vault.softwareWrapForTests = true
        ctx.getSharedPreferences("cryo_backup_lock_teste", 0).edit().clear().commit()
        val protection = BackupProtection(ctx, sandbox = true, vault = Vault(ctx, "cryo-auditoria"))
        // Desligada: o texto sai como entrou.
        assertEquals("{\"app\":\"Cryo\"}", protection.seal("{\"app\":\"Cryo\"}", 1L))
        assertTrue(protection.enable("senha forte".toCharArray(), iterations = 1_000))
        val sealed = protection.seal(Backup.export(Snapshot(accounts = listOf(Account(1, "Conta", color = 0))), exportedAt = 5L), 5L)
        assertTrue(BackupCrypto.isEncrypted(sealed))
        assertFalse(sealed.contains("Conta"))
        // O cofre para de abrir a chave (cofre do Android reiniciado): antes, o backup saía ABERTO; agora não sai.
        Vault.softwareWrapForTests = false
        assertThrows(BackupProtection.Unavailable::class.java) { protection.seal("{\"app\":\"Cryo\",\"segredo\":1}", 6L) }
    }

    /* ------------------------------- planilha ------------------------------- */

    @Test fun planilhaNaoRodaFormulasDeDescricoes() {
        val acc = Account(1, "=Conta", color = 0)
        val cat = Category(2, "@Mercado", CategoryKind.EXPENSE, "cart", 0)
        fun tx(id: Long, desc: String, note: String = "") =
            Tx(id = id, type = TxType.EXPENSE, amount = 1250, date = today, description = desc, categoryId = 2, accountId = 1, note = note)
        val csv = Backup.csv(
            Snapshot(
                accounts = listOf(acc), categories = listOf(cat),
                txs = listOf(tx(1, "=HYPERLINK(\"http://x\";\"clique\")"), tx(2, "+55 11 99999"), tx(3, "-desconto"), tx(4, "Padaria", "@SUM(A1)"), tx(5, "\tcmd")),
            ),
            today,
        )
        val cells = csv.lines().drop(1).filter { it.isNotBlank() }.flatMap { line -> line.split(';') }.map { it.trim('"') }
        // Nenhuma célula de texto começa com um caractere que a planilha leia como fórmula; os valores continuam números.
        assertTrue(cells.none { c -> c.isNotEmpty() && c[0] in "=+@\t" })
        assertTrue(cells.none { c -> c.startsWith("-") && !c.matches(Regex("-\\d+,\\d{2}")) })
        assertTrue(csv.contains("'=HYPERLINK"))
        assertTrue(csv.contains("-12,50"))
        assertTrue(csv.contains("'=Conta") && csv.contains("'@Mercado") && csv.contains("'@SUM(A1)"))
        assertTrue(csv.contains("Padaria"))
    }

    /* --------------------------- backup estragado --------------------------- */

    private fun backup(tx: String = "", card: String = "", bill: String = "") = """
        {"app":"Cryo","version":2,"exportedAt":1,
         "accounts":[{"id":1,"name":"Conta","type":"CHECKING","initialBalance":1000,"color":0}],
         "cards":[$card],"categories":[],"transactions":[$tx],"investments":[],"yields":[],"goals":[],"bills":[$bill],"budgets":[]}
    """.trimIndent()

    private fun tx(amount: String = "1250", date: String = "2026-11-01", type: String = "EXPENSE", extra: String = "") =
        """{"id":1,"type":"$type","amount":$amount,"date":"$date","accountId":1$extra}"""

    @Test fun backupEstragadoERecusadoComMensagemClara() {
        val friendly = io.github.h3yk0.cryo.domain.Res.s(R.string.backup_invalid_file)
        val bad = listOf(
            backup(tx = tx(amount = "-5000")),                       // gasto negativo viraria entrada
            backup(tx = tx(amount = "9223372036854775807")),         // estouraria as somas
            backup(tx = tx(type = "NAO_EXISTE")),
            backup(tx = tx(date = "2026-13-45")),
            backup(tx = tx(date = "99999-01-01")),
            backup(tx = tx(extra = ",\"invoiceYm\":202613")),        // mês 13: travava o app a cada abertura
            backup(tx = tx(extra = ",\"billYm\":7")),
            backup(bill = """{"id":1,"name":"Luz","amount":100,"dueDay":10,"kind":"EXPENSE","startYm":0}"""),
            """{"app":"Cryo","version":2,"accounts":"texto"}""",
            """{"app":"Cryo","version":2}""",
        )
        for (b in bad) {
            val e = assertThrows(b.take(90), IllegalArgumentException::class.java) { Backup.import(b) }
            assertEquals(friendly, e.message)
        }
        // Dias fora de 1 a 31 são acertados (o resto do backup vale).
        val fixed = Backup.import(backup(tx = tx(), card = """{"id":1,"name":"Roxo","limitAmount":1000,"closingDay":0,"dueDay":99,"color":0}"""))
        assertEquals(1, fixed.snapshot.cards.single().closingDay)
        assertEquals(31, fixed.snapshot.cards.single().dueDay)
        // E o livro-caixa abre com o que foi aceito.
        assertEquals(1000L - 1250L, Ledger(fixed.snapshot, today).balance(1))
        // Um backup de verdade continua abrindo igual.
        val real = Snapshot(accounts = listOf(Account(1, "Conta", initialBalance = -500, color = 0)), txs = listOf(Tx(1, TxType.EXPENSE, amount = 990, date = today, accountId = 1)))
        assertEquals(real.txs, Backup.import(Backup.export(real)).snapshot.txs)
    }

    /* ------------------------------ banco ------------------------------ */

    @Test fun pagueiDuasVezesGravaUmPagamentoSo() = runBlocking {
        val c = container()
        val acc = c.repo.saveAccount(Account(name = "Conta", color = 0))
        val bill = Bill(id = c.repo.saveBill(Bill(name = "Internet", amount = 12_990, dueDay = 10, startYm = 202601, accountId = acc)), name = "Internet", amount = 12_990, dueDay = 10, startYm = 202601, accountId = acc)
        // Dois toques ao mesmo tempo (ou a tela e o botão da notificação).
        val ids = (1..6).map { async(kotlinx.coroutines.Dispatchers.IO) { c.repo.markBillPaid(bill, nov, bill.amount, today, acc, null) } }.awaitAll()
        assertEquals(1, ids.toSet().size)
        assertEquals(1, c.repo.loadOnce().txs.count { it.billId == bill.id && it.billYm == nov.key() })
        // Desmarcar e pagar de novo continua funcionando.
        c.repo.unmarkBillPaid(bill, nov)
        c.repo.markBillPaid(bill, nov, bill.amount, today, acc, null)
        assertEquals(1, c.repo.loadOnce().txs.count { it.billId == bill.id })
    }

    @Test fun apagarCategoriaLevaOsRemanejamentos() = runBlocking {
        val c = container()
        val food = Category(name = "Comida", kind = CategoryKind.EXPENSE, icon = "restaurant", color = 0).let { it.copy(id = c.repo.saveCategory(it)) }
        val fun_ = Category(name = "Lazer", kind = CategoryKind.EXPENSE, icon = "celebration", color = 0).let { it.copy(id = c.repo.saveCategory(it)) }
        c.repo.saveBudget(food.id, 100_000); c.repo.saveBudget(fun_.id, 30_000)
        c.repo.adjustMonthLimit(food.id, nov.key(), 5_000)
        c.repo.deleteCategory(food)
        val s = c.repo.loadOnce()
        assertTrue(s.budgets.none { it.categoryId == food.id })
        assertTrue("remanejamento solto: ${s.budgetAdjustments}", s.budgetAdjustments.none { it.categoryId == food.id })
    }

    /* ------------------------------ bloqueio ------------------------------ */

    @Test fun erroDoSensorSoAbreEmAparelhoSemTrava() {
        val errors = listOf(
            BiometricPrompt.ERROR_HW_UNAVAILABLE, BiometricPrompt.ERROR_HW_NOT_PRESENT, BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
            BiometricPrompt.ERROR_NO_BIOMETRICS, BiometricPrompt.ERROR_LOCKOUT, BiometricPrompt.ERROR_USER_CANCELED, BiometricPrompt.ERROR_TIMEOUT,
        )
        // Aparelho com senha ou digital: nenhum erro abre o app (na 1.7, "sensor indisponível" abria).
        for (e in errors) assertFalse("erro $e", LockPolicy.unlockOnError(e, deviceSecure = true))
        // Sem trava nenhuma no aparelho: não há como pedir, então não prende a pessoa do lado de fora.
        assertTrue(LockPolicy.unlockOnError(BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL, deviceSecure = false))
        assertTrue(LockPolicy.unlockOnError(BiometricPrompt.ERROR_HW_UNAVAILABLE, deviceSecure = false))
        // Cancelar ou errar muitas vezes nunca abre, com ou sem trava.
        assertFalse(LockPolicy.unlockOnError(BiometricPrompt.ERROR_USER_CANCELED, deviceSecure = false))
        assertFalse(LockPolicy.unlockOnError(BiometricPrompt.ERROR_LOCKOUT, deviceSecure = false))
    }
}
