// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/* ------------------------------------------------------------------
 * Modelo de dados do Cryo.
 * Todos os valores em dinheiro são guardados em CENTAVOS (Long),
 * assim R$ 12,34 = 1234. Isso evita erros de arredondamento.
 * ------------------------------------------------------------------ */

enum class AccountType { CHECKING, SAVINGS, CASH, BENEFIT, OTHER }

enum class CategoryKind { EXPENSE, INCOME }

/**
 * Tipos de movimentação:
 * - EXPENSE: saída (de uma conta OU de um cartão de crédito)
 * - INCOME: entrada em uma conta (ou estorno no cartão)
 * - TRANSFER: de uma conta (accountId) para outra (toAccountId)
 * - CARD_PAYMENT: pagamento de fatura (accountId -> cardId)
 * - INVEST_IN / INVEST_OUT: aporte e resgate de investimento
 * - GOAL_IN / GOAL_OUT: guardar e retirar dinheiro de uma meta/caixinha
 * - DEBT_IN: dinheiro emprestado que entrou numa conta (aumenta a dívida, não é receita)
 *
 * O pagamento de uma dívida é uma EXPENSE comum com debtId preenchido:
 * assim ele entra no "Saiu" do mês, nas categorias e na previsão como qualquer gasto.
 */
enum class TxType { EXPENSE, INCOME, TRANSFER, CARD_PAYMENT, INVEST_IN, INVEST_OUT, GOAL_IN, GOAL_OUT, DEBT_IN }

enum class InvestmentKind { SAVINGS, FIXED_INCOME, TREASURY, STOCKS, REITS, FUNDS, CRYPTO, PENSION, OTHER }

/** Empréstimo, financiamento, cartão ou cheque especial, acordo, pessoa (amigo/família), outra. */
enum class DebtKind { LOAN, FINANCING, CARD, AGREEMENT, PERSON, OTHER }

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType = AccountType.CHECKING,
    val initialBalance: Long = 0,
    val color: Int,
    val includeInTotal: Boolean = true,
    val archived: Boolean = false,
    val sortOrder: Int = 0,
)

@Entity(tableName = "cards")
data class CreditCard(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val limitAmount: Long,
    val closingDay: Int,
    val dueDay: Int,
    val color: Int,
    val payAccountId: Long? = null,
    val archived: Boolean = false,
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: CategoryKind,
    val icon: String,
    val color: Int,
    /** Palavras (separadas por vírgula) que ajudam o registro por frase a reconhecer a categoria. */
    val keywords: String = "",
    val archived: Boolean = false,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "transactions",
    indices = [
        Index("date"), Index("accountId"), Index("cardId"), Index("categoryId"),
        Index("installmentGroup"), Index("billId"), Index("investmentId"), Index("goalId"), Index("debtId"),
    ],
)
data class Tx(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TxType,
    /** Sempre positivo, em centavos. */
    val amount: Long,
    val date: LocalDate,
    val description: String = "",
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val toAccountId: Long? = null,
    val cardId: Long? = null,
    /** Fatura do cartão (ano*100 + mês de vencimento). */
    val invoiceYm: Int? = null,
    val installmentGroup: String? = null,
    val installmentNumber: Int = 0,
    val installmentTotal: Int = 0,
    val investmentId: Long? = null,
    val goalId: Long? = null,
    val billId: Long? = null,
    val billYm: Int? = null,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    /** Dívida ligada a este lançamento (pagamento ou dinheiro emprestado). */
    val debtId: Long? = null,
)

@Entity(tableName = "investments")
data class Investment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: InvestmentKind = InvestmentKind.FIXED_INCOME,
    val color: Int,
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Rendimento (positivo) ou desvalorização (negativo) registrado ao atualizar o valor de um investimento. */
@Entity(tableName = "investment_yields", indices = [Index("investmentId")])
data class InvestmentYield(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val investmentId: Long,
    val date: LocalDate,
    val amount: Long,
    val note: String = "",
)

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val target: Long,
    val deadline: LocalDate? = null,
    val emoji: String = "🎯",
    val color: Int,
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Conta fixa mensal (a pagar) ou recebimento fixo (a receber). */
@Entity(tableName = "bills")
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Long,
    val dueDay: Int,
    val kind: CategoryKind = CategoryKind.EXPENSE,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val cardId: Long? = null,
    val remind: Boolean = true,
    val remindDaysBefore: Int = 1,
    val active: Boolean = true,
    val startYm: Int,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey val categoryId: Long,
    val limitAmount: Long,
)

/**
 * Uma dívida: empréstimo, financiamento, acordo, dinheiro que você deve a alguém...
 *
 * Quanto falta pagar = initialBalance + dinheiro emprestado depois (DEBT_IN)
 *                      + ajustes (juros, descontos) − pagamentos.
 */
@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Com quem é a dívida (banco, loja, pessoa). */
    val creditor: String = "",
    val kind: DebtKind = DebtKind.LOAN,
    val color: Int,
    /** Quanto faltava pagar no dia do cadastro, em centavos. */
    val initialBalance: Long,
    /** Valor de cada parcela (0 = sem parcelas fixas: paga quando puder). */
    val installmentAmount: Long = 0,
    /** Total de parcelas do contrato. */
    val installmentCount: Int = 0,
    /** Parcelas pagas antes de cadastrar a dívida no Cryo. */
    val paidBefore: Int = 0,
    /** Dia do vencimento das parcelas. */
    val dueDay: Int = 10,
    /** Mês (ano*100 + mês) da primeira parcela a pagar acompanhada pelo Cryo. */
    val startYm: Int,
    /** Prazo combinado para quitar (dívidas sem parcelas). */
    val deadline: LocalDate? = null,
    /** Conta usada normalmente para pagar. */
    val accountId: Long? = null,
    val remind: Boolean = true,
    val remindDaysBefore: Int = 1,
    val note: String = "",
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Mudança no saldo da dívida sem dinheiro saindo da conta: juros e encargos (+) ou descontos (−). */
@Entity(tableName = "debt_adjustments", indices = [Index("debtId")])
data class DebtAdjustment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtId: Long,
    val date: LocalDate,
    val amount: Long,
    val note: String = "",
)
