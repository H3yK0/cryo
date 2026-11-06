// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.Res
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.h3yk0.cryo.data.LocaleSettings
import io.github.h3yk0.cryo.data.ThemeMode
import io.github.h3yk0.cryo.notify.Reminders
import io.github.h3yk0.cryo.ui.CryoRoot
import io.github.h3yk0.cryo.ui.components.Motion
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.screens.LockScreen
import io.github.h3yk0.cryo.ui.screens.OnboardingScreen
import io.github.h3yk0.cryo.ui.screens.TestModeStrip
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import io.github.h3yk0.cryo.ui.theme.isDarkTheme
import kotlinx.coroutines.runBlocking

class MainActivity : FragmentActivity() {
    private var locked by mutableStateOf(false)
    /** O bloqueio está ligado (copiado dos ajustes, para usar ao voltar de segundo plano). */
    private var lockOn = false
    private var authenticating = false
    private val app get() = application as CryoApp
    /** Os dados que esta tela mostra (de verdade ou de teste). */
    private lateinit var shown: AppContainer

    /** A tela nasce com a língua do app (RF-LNG-02). */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleSettings.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Só na primeira abertura: ao girar a tela, a navegação já está restaurada.
        val openOnStart = if (savedInstanceState == null) intent?.getStringExtra(Reminders.EXTRA_OPEN) else null
        // Os avisos de contas e de dívidas falam dos dados de verdade: tocar num deles com o modo de teste ligado abre
        // os dados de verdade (senão o pagamento seria registrado nos dados de exemplo).
        if ((openOnStart == "bills" || openOnStart == "debts") && app.container.sandbox) app.switchData(false)
        val container = app.container
        shown = container
        // Pede o desbloqueio sempre que o app abre do zero, inclusive quando o Android fechou o app em segundo plano e
        // a tela volta sozinha (aí a tela antiga vem "restaurada"). Girar a tela ou trocar para o modo de teste não pede
        // de novo. Leitura rápida, para não mostrar nada antes do bloqueio.
        lockOn = runBlocking { container.lockSettings.current().lockEnabled }
        if (!lockOn) app.unlocked = true
        locked = lockOn && !app.unlocked
        hideFromRecents(lockOn)

        setContent {
            val settings by container.settings.flow.collectAsStateWithLifecycle(initialValue = null)
            val lockSettings by container.lockSettings.flow.collectAsStateWithLifecycle(initialValue = null)
            val lockNow = lockSettings?.lockEnabled ?: lockOn
            val appState = rememberSaveableStateHolder()
            SideEffect {
                if (lockOn != lockNow) hideFromRecents(lockNow)
                lockOn = lockNow
                // Sem bloqueio, a sessão conta como desbloqueada: ligar o bloqueio agora não tranca quem já está dentro.
                if (!lockNow) app.unlocked = true
            }
            val s = settings
            val mode = s?.themeMode ?: ThemeMode.SYSTEM
            CryoTheme(mode = mode, dynamicColor = s?.dynamicColor ?: true) {
                val dark = isDarkTheme(mode)
                DisposableEffect(dark) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                        navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark },
                    )
                    onDispose {}
                }
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                    if (s != null) {
                        CompositionLocalProvider(LocalContainer provides container, LocalSettings provides s) {
                            val showLock = locked && lockNow
                            val focus = LocalFocusManager.current
                            val keyboard = LocalSoftwareKeyboardController.current
                            LaunchedEffect(showLock) {
                                if (showLock) {
                                    focus.clearFocus(force = true)
                                    keyboard?.hide()
                                }
                            }
                            // Bloqueado, o app sai da tela de verdade: um app só escondido por baixo ainda mostraria diálogos,
                            // menus e folhas (cada um fica numa janela própria, por cima de tudo) e receberia o Voltar e o
                            // teclado. O estado fica guardado, como ao girar a tela: ao desbloquear, a pessoa volta exatamente
                            // para onde estava (inclusive no meio de uma importação).
                            if (showLock) {
                                LockScreen(onUnlock = ::authenticate)
                            } else {
                                appState.SaveableStateProvider("app") {
                                    // DS-MOV-09: da apresentação para o app, fade. (O bloqueio, logo acima, nunca anima: DS-MOV-10.)
                                    AnimatedContent(
                                        s.onboardingDone,
                                        transitionSpec = {
                                            fadeIn(tween(Motion.MEDIUM, delayMillis = Motion.SHORT / 2)) togetherWith fadeOut(tween(Motion.SHORT))
                                        },
                                        label = "inicio",
                                    ) { done ->
                                        when {
                                            !done && container.sandbox -> Column(Modifier.fillMaxSize()) {
                                                TestModeStrip(insetTop = true)
                                                Box(Modifier.weight(1f).consumeWindowInsets(WindowInsets.statusBars)) { OnboardingScreen() }
                                            }
                                            !done -> OnboardingScreen()
                                            else -> CryoRoot(openOnStart)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!authenticating) app.lastStop = SystemClock.elapsedRealtime()
    }

    override fun onStart() {
        super.onStart()
        // Volta a bloquear se o app ficou mais de 1 minuto em segundo plano.
        if (lockOn && app.lastStop > 0 && SystemClock.elapsedRealtime() - app.lastStop > 60_000) {
            app.unlocked = false
            locked = true
        }
        app.lastStop = 0
        // A troca entre os dados de verdade e os de teste aconteceu enquanto esta tela não estava à vista (por exemplo,
        // a tela girou no meio da preparação dos dados de exemplo): recria para mostrar os dados certos. Depois do
        // bloqueio: a tela nova também pede o desbloqueio, se for o caso.
        if (app.container !== shown) recreate()
    }

    private fun authenticate() {
        if (authenticating) return
        authenticating = true
        val prompt = BiometricPrompt(
            this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    authenticating = false
                    unlock()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    authenticating = false
                    // Sem nenhuma trava configurada no aparelho: libera para não prender a pessoa fora do app.
                    if (errorCode == BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL || errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT ||
                        errorCode == BiometricPrompt.ERROR_HW_UNAVAILABLE
                    ) unlock()
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(Res.s(R.string.lock_desbloquear_cryo))
            .setSubtitle(Res.s(R.string.lock_use_sua_digital_rosto))
            .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            .build()
        prompt.authenticate(info)
    }

    /** Desbloqueado (digital, rosto ou senha do celular): vale até o app ficar 1 minuto em segundo plano. */
    @VisibleForTesting
    internal fun unlock() {
        app.unlocked = true
        locked = false
    }

    /**
     * Com o bloqueio ligado, a miniatura do app em Recentes (que mostra a última tela, com saldos e movimentações)
     * não é guardada (Android 13 ou mais novo).
     */
    private fun hideFromRecents(lock: Boolean) {
        if (Build.VERSION.SDK_INT >= 33) runCatching { setRecentsScreenshotEnabled(!lock) }
    }

    private companion object {
        val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
