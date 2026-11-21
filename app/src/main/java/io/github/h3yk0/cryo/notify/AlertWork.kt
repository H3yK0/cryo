// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.h3yk0.cryo.AppContainer
import io.github.h3yk0.cryo.CryoApp
import io.github.h3yk0.cryo.data.db.AccountType
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.Ledger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/**
 * Os dois horários do Cryo (RF-AVI-04): de manhã (~9h) as contas do dia e no máximo um aviso comum; à noite, no horário que a
 * pessoa escolheu, o lembrete do dia ou o resumo de domingo. Sempre os dados de verdade, mesmo com as telas no modo de teste.
 */
object AlertRun {
    suspend fun run(ctx: Context, slot: AlertSlot): Int {
        val c = (ctx.applicationContext as CryoApp).real
        if (!Reminders.canNotify(ctx)) return 0
        val state = AlertState(ctx)
        val today = LocalDate.now()
        val snapshot = c.repo.loadOnce()
        val l = Ledger(snapshot, today)
        // Os contas ficam valendo o interruptor antigo de lembretes (Ajustes › Lembretes).
        if (!c.settings.current().remindersEnabled) state.setEnabled(AlertGroup.BILLS, false)
        val report = if (slot == AlertSlot.MORNING) runCatching { c.engine.run(snapshot, today, c.engineStore.options.value) }.getOrNull() else null
        return AlertPlanner(l, report, state).plan(slot).count { Alerts.post(ctx, it, state) }
    }
}

/** De manhã. (O nome ficou o da 1.0: o Android guarda o agendamento por ele em quem já usa o app.) */
class BillReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        runCatching { AlertRun.run(applicationContext, AlertSlot.MORNING) }
        return Result.success()
    }
}

/** À noite, no horário escolhido (RF-AVI-11). */
class EveningAlertWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        runCatching { AlertRun.run(applicationContext, AlertSlot.EVENING) }
        return Result.success()
    }
}

/**
 * RF-AVI-03: os botões da notificação. Os que mudam dados ("Já paguei", "Dia sem gastos", "Registrar") só chegam aqui depois
 * de o celular ser desbloqueado (o Android pede). "Dispensou sem abrir" conta para espaçar aquele tipo (RF-AVI-04).
 */
class AlertActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val ctx = context.applicationContext
        val pending = goAsync()
        scope.launch {
            try {
                handle(ctx, intent)
            } catch (_: Exception) {
                // Nada aqui pode derrubar o app: o pior caso é o botão não fazer nada.
            } finally {
                pending.finish()
            }
        }
    }

    /** O que cada botão faz (separado do recebimento para os testes chamarem direto). */
    internal suspend fun handle(ctx: Context, i: Intent) {
        val action = i.getStringExtra(EXTRA_ACTION) ?: return
        if (action == SHOP_END) { Shopping.stop(ctx); return }
        val kind = runCatching { AlertKind.valueOf(i.getStringExtra(EXTRA_KIND).orEmpty()) }.getOrNull() ?: return
        val id = i.getIntExtra(EXTRA_ID, 0)
        val state = AlertState(ctx)
        if (action == DISMISSED) { state.markIgnored(kind); return }
        state.markOpened(kind)
        val c: AppContainer = (ctx as CryoApp).real
        val today = LocalDate.now()
        when (AlertAction.valueOf(action)) {
            AlertAction.PAID -> {
                val billId = i.getLongExtra(EXTRA_BILL, -1)
                val ym = i.getIntExtra(EXTRA_YM, 0)
                val l = Ledger(c.repo.loadOnce(), today)
                val bill = l.s.bills.firstOrNull { it.id == billId }
                // RF-CFX-06 (ERR-045): se o pagamento já está no extrato (mesmo nome e valor), o botão liga a conta fixa
                // a ele em vez de gravar outro. Sem tela para perguntar, só quando o casamento é certo.
                // Vale tanto para a conta que ainda está em aberto quanto para a que o Cryo já reconheceu no extrato e
                // a pessoa agora confirma pelo aviso: nos dois casos `markBillPaidLinking` resolve sem duplicar.
                val semLigacao = l.s.txs.none { it.billId == billId && it.billYm == ym }
                val venceNesseMes = bill != null && l.billStatuses(YearMonth.of(ym / 100, ym % 100)).any { it.bill.id == billId }
                if (bill != null && venceNesseMes && semLigacao && bill.kind == io.github.h3yk0.cryo.data.db.CategoryKind.EXPENSE) {
                    val card = bill.cardId?.let { cid -> l.s.cards.firstOrNull { it.id == cid } }
                    c.repo.markBillPaidLinking(
                        bill, YearMonth.of(ym / 100, ym % 100), bill.amount, today, bill.accountId ?: defaultAccount(c, l), card,
                    )
                    Sounds.play(ctx, Sound.SAVE)
                }
            }
            AlertAction.SNOOZE -> state.snooze("bill:${i.getLongExtra(EXTRA_BILL, -1)}:${i.getIntExtra(EXTRA_YM, 0)}", today)
            AlertAction.NO_SPEND -> { c.repo.markNoSpend(today); Sounds.play(ctx, Sound.STREAK) }
            AlertAction.REGISTER -> {
                val amount = i.getLongExtra(EXTRA_AMOUNT, 0)
                if (amount > 0) {
                    val l = Ledger(c.repo.loadOnce(), today)
                    c.repo.saveTx(
                        Tx(
                            type = TxType.INCOME, amount = amount, date = today, description = i.getStringExtra(EXTRA_LABEL).orEmpty(),
                            categoryId = i.getLongExtra(EXTRA_CATEGORY, -1).takeIf { it > 0 },
                            accountId = i.getLongExtra(EXTRA_ACCOUNT, -1).takeIf { it > 0 } ?: defaultAccount(c, l),
                        ),
                    )
                    Sounds.play(ctx, Sound.INCOME)
                }
            }
            AlertAction.LOG, AlertAction.OPEN -> Unit
        }
        NotificationManagerCompat.from(ctx).cancel(id)
    }

    private suspend fun defaultAccount(c: AppContainer, l: Ledger): Long? {
        val active = l.s.accounts.filter { !it.archived }
        return c.settings.current().defaultAccountId?.takeIf { id -> active.any { it.id == id } }
            ?: active.firstOrNull { it.type == AccountType.CHECKING }?.id ?: active.firstOrNull()?.id
    }

    companion object {
        const val DISMISSED = "DISMISSED"
        const val SHOP_END = "SHOP_END"
        private const val EXTRA_ACTION = "action"
        private const val EXTRA_KIND = "kind"
        private const val EXTRA_ID = "id"
        private const val EXTRA_BILL = "bill"
        private const val EXTRA_YM = "ym"
        private const val EXTRA_AMOUNT = "amount"
        private const val EXTRA_ACCOUNT = "account"
        private const val EXTRA_CATEGORY = "category"
        private const val EXTRA_LABEL = "label"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun intent(ctx: Context, action: String, a: Alert, id: Int): Intent = Intent(ctx, AlertActionReceiver::class.java)
            .setAction("${ctx.packageName}.alert.$action.$id")
            .putExtra(EXTRA_ACTION, action)
            .putExtra(EXTRA_KIND, a.kind.name)
            .putExtra(EXTRA_ID, id)
            .putExtra(EXTRA_BILL, a.billId ?: -1L)
            .putExtra(EXTRA_YM, a.ym ?: 0)
            .putExtra(EXTRA_AMOUNT, a.amount ?: 0L)
            .putExtra(EXTRA_ACCOUNT, a.accountId ?: -1L)
            .putExtra(EXTRA_CATEGORY, a.categoryId ?: -1L)
            .putExtra(EXTRA_LABEL, a.label.orEmpty())
    }
}
