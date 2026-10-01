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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import io.github.h3yk0.cryo.ui.theme.isDarkTheme
import kotlinx.coroutines.runBlocking

class MainActivity : FragmentActivity() {
    private var locked by mutableStateOf(false)
    private var lastStop = 0L
    private var authenticating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as CryoApp).container
        val openOnStart = intent?.getStringExtra(Reminders.EXTRA_OPEN)
        if (savedInstanceState == null) {
            // Leitura rápida para não mostrar nada antes do bloqueio.
            locked = runBlocking { container.settings.current().lockEnabled }
        }

        setContent {
            val settings by container.settings.flow.collectAsStateWithLifecycle(initialValue = null)
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
                            when {
                                locked && s.lockEnabled -> LockScreen(onUnlock = ::authenticate)
                                !s.onboardingDone -> OnboardingScreen()
                                else -> CryoRoot(openOnStart)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!authenticating) lastStop = SystemClock.elapsedRealtime()
    }

    override fun onStart() {
        super.onStart()
        // Volta a bloquear se o app ficou mais de 1 minuto em segundo plano.
        if (lastStop > 0 && SystemClock.elapsedRealtime() - lastStop > 60_000) locked = true
        lastStop = 0
    }

    private fun authenticate() {
        if (authenticating) return
        authenticating = true
        val prompt = BiometricPrompt(
            this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    authenticating = false
                    locked = false
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    authenticating = false
                    // Sem nenhuma trava configurada no aparelho: libera para não prender a pessoa fora do app.
                    if (errorCode == BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL || errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT ||
                        errorCode == BiometricPrompt.ERROR_HW_UNAVAILABLE
                    ) locked = false
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
