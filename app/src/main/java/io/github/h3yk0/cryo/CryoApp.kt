// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import android.app.Application
import io.github.h3yk0.cryo.data.AutoBackupRunner
import io.github.h3yk0.cryo.data.AutoBackupStore
import io.github.h3yk0.cryo.data.BackupPlaces
import io.github.h3yk0.cryo.data.DevSettings
import io.github.h3yk0.cryo.data.FinanceRepository
import io.github.h3yk0.cryo.data.SafBackupStorage
import io.github.h3yk0.cryo.data.SettingsRepository
import io.github.h3yk0.cryo.data.db.CryoDatabase
import io.github.h3yk0.cryo.data.SampleData
import io.github.h3yk0.cryo.importer.ImportReport
import io.github.h3yk0.cryo.importer.SampleFiles
import io.github.h3yk0.cryo.notify.AutoBackupWork
import io.github.h3yk0.cryo.notify.Reminders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/** Mensagem rápida (barra na parte de baixo), com ação opcional, como "Desfazer". */
data class UiMessage(val text: String, val actionLabel: String? = null, val action: (suspend () -> Unit)? = null)

/**
 * Guarda as peças principais do app (banco, ajustes) e um escopo que não morre ao trocar de tela.
 * [sandbox]: modo de teste do modo desenvolvedor, com banco de dados e ajustes separados dos de verdade.
 * [lockFrom]: de onde vem o bloqueio com digital ou senha (no modo de teste, dos ajustes de verdade).
 */
class AppContainer(val app: Application, val sandbox: Boolean = false, lockFrom: SettingsRepository? = null) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val db = CryoDatabase.build(app, if (sandbox) CryoDatabase.TEST_NAME else CryoDatabase.NAME)
    val repo = FinanceRepository(db, appScope)
    val settings = SettingsRepository(app, sandbox)
    /** O bloqueio vale para o app inteiro, inclusive no modo de teste. */
    val lockSettings: SettingsRepository = lockFrom ?: settings
    val autoBackup = AutoBackupStore(app)
    val backupRunner = AutoBackupRunner(repo, autoBackup, SafBackupStorage(app), enabled = !sandbox)

    /** Modo desenvolvedor (ligado com 7 toques na versão, em Ajustes). */
    val dev = DevSettings(app)

    /** Como foi a leitura do último extrato (para o relatório do modo desenvolvedor). */
    val lastImport = MutableStateFlow<ImportReport?>(null)

    /** Arquivo de exemplo escolhido no modo desenvolvedor, esperando a tela de importação abrir. */
    val pendingSample = MutableStateFlow<SampleFiles.Sample?>(null)

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 8)
    val messages: SharedFlow<UiMessage> = _messages

    fun launch(block: suspend CoroutineScope.() -> Unit) = appScope.launch(block = block)
    fun message(text: String, actionLabel: String? = null, action: (suspend () -> Unit)? = null) {
        _messages.tryEmit(UiMessage(text, actionLabel, action))
    }

}

class CryoApp : Application() {
    private var realData: AppContainer? = null
    private var testData: AppContainer? = null

    /**
     * Os dados de verdade. Lembretes e backup automático usam sempre estes, mesmo quando as telas estão no
     * modo de teste: quem esquecer o modo de teste ligado continua recebendo os avisos das próprias contas.
     */
    val real: AppContainer
        get() = synchronized(this) {
            realData ?: AppContainer(this, sandbox = false).also { c ->
                realData = c
                c.launch { c.repo.ensureSeeded() }
            }
        }

    private val test: AppContainer
        get() = synchronized(this) {
            testData ?: AppContainer(this, sandbox = true, lockFrom = real.settings).also { c ->
                testData = c
                c.launch { c.repo.ensureSeeded() }
            }
        }

    /** O que as telas mostram: os dados de verdade ou, no modo de teste, os de exemplo. */
    val container: AppContainer get() = if (DevSettings(this).sandbox) test else real

    /** Já desbloqueado com digital ou senha neste processo (some quando o Android fecha o app). */
    @Volatile var unlocked = false

    /** Quando a tela saiu de vista pela última vez (para bloquear de novo depois de 1 minuto). */
    @Volatile var lastStop = 0L

    override fun onCreate() {
        super.onCreate()
        real // abre o banco de verdade logo (e cria as categorias na primeira vez)
        // Lembretes nunca podem impedir o app de abrir.
        runCatching { Reminders.createChannel(this) }
        runCatching { Reminders.schedule(this) }
        runCatching { AutoBackupWork.createChannel(this) }
        startBackup()
    }

    /**
     * Backup automático: confere as permissões, confirma o agendamento e, se estiver atrasado, faz logo depois.
     * Nada disso pode impedir o app de abrir. Sempre com os dados de verdade.
     */
    private fun startBackup() {
        val c = real
        c.launch {
            runCatching {
                BackupPlaces.audit(this@CryoApp, c.autoBackup).forEach { AutoBackupWork.notifyProblem(this@CryoApp, it) }
                val on = c.autoBackup.current().let { it.enabled && it.hasTarget }
                AutoBackupWork.sync(this@CryoApp, on)
                if (on) AutoBackupWork.checkSoon(this@CryoApp)
            }
        }
    }

    /**
     * Troca o que as telas mostram entre os dados de verdade e os de teste (modo desenvolvedor). Quem chama recria
     * a tela depois. Os dois bancos ficam abertos e intactos: trocar de novo volta tudo como estava.
     */
    fun switchData(sandbox: Boolean) {
        DevSettings(this).sandbox = sandbox
    }

    /**
     * Prepara os dados de teste: na primeira vez (ou com [fresh]), 4 meses de dados de exemplo, já sem a apresentação
     * inicial. Nunca toca nos dados de verdade.
     */
    suspend fun prepareTestData() = withTestData { t ->
        if (t.repo.loadOnce().accounts.isEmpty()) fillWithSamples(t)
    }

    private val testDataLock = Mutex()

    /** Uma mudança de cada vez nos dados de teste (entrar no modo de teste, recriar os exemplos, começar do zero). */
    suspend fun withTestData(block: suspend (AppContainer) -> Unit) = testDataLock.withLock { block(test) }

    companion object {
        /**
         * Troca o que tem no [c] (sempre o de teste) por dados de exemplo, numa transação só: se algo interromper
         * no meio, fica como estava.
         */
        suspend fun fillWithSamples(c: AppContainer) {
            check(c.sandbox) { "Dados de exemplo só no modo de teste" }
            c.repo.inTransaction {
                c.repo.eraseAll()
                SampleData.fill(c.repo, LocalDate.now())
            }
            c.settings.setDefaultAccount(c.repo.loadOnce().accounts.firstOrNull { !it.archived }?.id)
            c.settings.setOnboardingDone(true)
        }
    }
}
