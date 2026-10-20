// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.h3yk0.cryo.data.ThemeMode
import io.github.h3yk0.cryo.notify.Reminders
import io.github.h3yk0.cryo.ui.CryoRoot
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

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = app.container
        // Só na primeira abertura: ao girar a tela, a navegação já está restaurada.
        val openOnStart = if (savedInstanceState == null) intent?.getStringExtra(Reminders.EXTRA_OPEN) else null
        // Pede o desbloqueio sempre que o app abre do zero, inclusive quando o Android fechou o app em segundo plano e
        // a tela volta sozinha (aí a tela antiga vem "restaurada"). Girar a tela ou trocar para o modo de teste não pede
        // de novo. Leitura rápida, para não mostrar nada antes do bloqueio.
        lockOn = runBlocking { container.lockSettings.current().lockEnabled }
        if (!lockOn) app.unlocked = true
        locked = lockOn && !app.unlocked

        setContent {
            val settings by container.settings.flow.collectAsStateWithLifecycle(initialValue = null)
            val lockSettings by container.lockSettings.flow.collectAsStateWithLifecycle(initialValue = null)
            val lockNow = lockSettings?.lockEnabled ?: lockOn
            SideEffect {
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
                            Box(Modifier.fillMaxSize()) {
                                // O app continua montado por baixo do bloqueio: ao desbloquear, a pessoa volta exatamente
                                // para onde estava (inclusive no meio de uma importação). Enquanto isso, nada dele aparece,
                                // recebe toque ou fica legível para leitores de tela.
                                Box(Modifier.fillMaxSize().then(if (showLock) Modifier.clearAndSetSemantics {} else Modifier)) {
                                    when {
                                        !s.onboardingDone && container.sandbox -> Column(Modifier.fillMaxSize()) {
                                            TestModeStrip(insetTop = true)
                                            Box(Modifier.weight(1f).consumeWindowInsets(WindowInsets.statusBars)) { OnboardingScreen() }
                                        }
                                        !s.onboardingDone -> OnboardingScreen()
                                        else -> CryoRoot(openOnStart)
                                    }
                                }
                                if (showLock) {
                                    // Surface bloqueia os toques: nada chega ao app por baixo.
                                    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                                        LockScreen(onUnlock = ::authenticate)
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
    }

    private fun authenticate() {
        if (authenticating) return
        authenticating = true
        val prompt = BiometricPrompt(
            this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    authenticating = false
                    app.unlocked = true
                    locked = false
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    authenticating = false
                    // Sem nenhuma trava configurada no aparelho: libera para não prender a pessoa fora do app.
                    if (errorCode == BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL || errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT ||
                        errorCode == BiometricPrompt.ERROR_HW_UNAVAILABLE
                    ) {
                        app.unlocked = true
                        locked = false
                    }
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Desbloquear o Cryo")
            .setSubtitle("Use sua digital, rosto ou senha do celular")
            .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            .build()
        prompt.authenticate(info)
    }

    private companion object {
        val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
