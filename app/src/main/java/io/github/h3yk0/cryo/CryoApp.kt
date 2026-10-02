// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import android.app.Application
import io.github.h3yk0.cryo.data.AutoBackupRunner
import io.github.h3yk0.cryo.data.AutoBackupStore
import io.github.h3yk0.cryo.data.BackupPlaces
import io.github.h3yk0.cryo.data.FinanceRepository
import io.github.h3yk0.cryo.data.SafBackupStorage
import io.github.h3yk0.cryo.data.SettingsRepository
import io.github.h3yk0.cryo.data.db.CryoDatabase
import io.github.h3yk0.cryo.notify.AutoBackupWork
import io.github.h3yk0.cryo.notify.Reminders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

/** Mensagem rápida (barra na parte de baixo), com ação opcional, como "Desfazer". */
data class UiMessage(val text: String, val actionLabel: String? = null, val action: (suspend () -> Unit)? = null)

/** Guarda as peças principais do app (banco, ajustes) e um escopo que não morre ao trocar de tela. */
class AppContainer(app: Application) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val db = CryoDatabase.build(app)
    val repo = FinanceRepository(db, appScope)
    val settings = SettingsRepository(app)
    val autoBackup = AutoBackupStore(app)
    val backupRunner = AutoBackupRunner(repo, autoBackup, SafBackupStorage(app))

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 8)
    val messages: SharedFlow<UiMessage> = _messages

    fun launch(block: suspend CoroutineScope.() -> Unit) = appScope.launch(block = block)
    fun message(text: String, actionLabel: String? = null, action: (suspend () -> Unit)? = null) {
        _messages.tryEmit(UiMessage(text, actionLabel, action))
    }
}

class CryoApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.launch { container.repo.ensureSeeded() }
        // Lembretes nunca podem impedir o app de abrir.
        runCatching { Reminders.createChannel(this) }
        runCatching { Reminders.schedule(this) }
        runCatching { AutoBackupWork.createChannel(this) }
        // Backup automático: confere as permissões, confirma o agendamento e, se estiver atrasado, faz logo depois.
        // Nada disso pode impedir o app de abrir.
        container.launch {
            runCatching {
                BackupPlaces.audit(this@CryoApp, container.autoBackup).forEach { AutoBackupWork.notifyProblem(this@CryoApp, it) }
                val on = container.autoBackup.current().let { it.enabled && it.hasTarget }
                AutoBackupWork.sync(this@CryoApp, on)
                if (on) AutoBackupWork.checkSoon(this@CryoApp)
            }
        }
    }
}
