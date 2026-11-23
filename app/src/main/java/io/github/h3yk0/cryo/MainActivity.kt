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
import io.github.h3yk0.cryo.domain.LockRules
import io.github.h3yk0.cryo.ui.screens.LockScreen
import io.github.h3yk0.cryo.ui.screens.OnboardingScreen
import io.github.h3yk0.cryo.ui.screens.TestModeStrip
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import io.github.h3yk0.cryo.ui.theme.isDarkTheme
import kotlinx.coroutines.runBlocking

/**
 * RF-BLQ: quando um erro do desbloqueio pode abrir o app. Só num aparelho **sem trava nenhuma** (nem senha, nem digital):
 * ali não há como pedir o desbloqueio e a pessoa ficaria presa do lado de fora. Num aparelho com trava, erro do sensor
 * (ocupado, indisponível, desligado) nunca abre: a pessoa tenta de novo ou usa a senha do celular. (Até a 1.7, "sensor
 * indisponível" abria o app mesmo com trava configurada.)
 */
object LockPolicy {
    private val NO_LOCK_ERRORS = setOf(
        BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL, BiometricPrompt.ERROR_HW_NOT_PRESENT, BiometricPrompt.ERROR_HW_UNAVAILABLE,
        BiometricPrompt.ERROR_NO_BIOMETRICS,
    )

    fun unlockOnError(errorCode: Int, deviceSecure: Boolean): Boolean = !deviceSecure && errorCode in NO_LOCK_ERRORS
}

class MainActivity : FragmentActivity() {
    private var locked by mutableStateOf(false)
    /** O bloqueio está ligado (copiado dos ajustes, para usar ao voltar de segundo plano). */
    private var lockOn = false
    /** RF-BLQ-02: segundos fora do app até pedir o desbloqueio de novo (0 = na hora). */
    private var lockAfter = LockRules.DEFAULT
    /** RF-BLQ-07: o modo privacidade (a janela protegida contra prints e gravações). */
    private var privacy = false
    /** Para os testes: o tempo de "Bloquear de novo" que esta tela já leu dos ajustes. */
    @VisibleForTesting internal val lockAfterNow: Int get() = lockAfter
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
        val fromAlert = savedInstanceState == null && intent?.hasExtra(io.github.h3yk0.cryo.notify.Alerts.EXTRA_KIND) == true
        // RF-WID-05: o "Lançar" do widget também é dos dados de verdade.
        if ((openOnStart == "bills" || openOnStart == "debts" || openOnStart == "voice" || openOnStart == "entry" || fromAlert) && app.container.sandbox) app.switchData(false)
        // RF-AVI-04: abriu o aviso (não ignorou): aquele tipo volta à frequência normal.
        if (fromAlert) runCatching {
            io.github.h3yk0.cryo.notify.AlertState(this).markOpened(io.github.h3yk0.cryo.notify.AlertKind.valueOf(intent.getStringExtra(io.github.h3yk0.cryo.notify.Alerts.EXTRA_KIND).orEmpty()))
        }
        val container = app.container
        shown = container
        // Pede o desbloqueio sempre que o app abre do zero, inclusive quando o Android fechou o app em segundo plano e
        // a tela volta sozinha (aí a tela antiga vem "restaurada"). Girar a tela ou trocar para o modo de teste não pede
        // de novo. Leitura rápida, para não mostrar nada antes do bloqueio.
        val lockStart = runBlocking { container.lockSettings.current() }
        lockOn = lockStart.lockEnabled
        lockAfter = lockStart.lockAfter
        privacy = lockStart.privacyMode
        // RF-BLQ-02 (ERR-039): abrir do zero pede o desbloqueio mesmo com o processo ainda vivo (a pessoa tirou o app dos
        // Recentes e abriu de novo antes de 1 minuto: o "desbloqueado" continuava valendo). Girar a tela e as recriações do
        // próprio app (modo de teste, língua) chegam com o estado salvo e não pedem de novo.
        if (lockOn && savedInstanceState == null) app.unlocked = false
        if (!lockOn) app.unlocked = true
        locked = lockOn && !app.unlocked
        applyPrivacy(lockOn, privacy)

        setContent {
            val settings by container.settings.flow.collectAsStateWithLifecycle(initialValue = null)
            val lockSettings by container.lockSettings.flow.collectAsStateWithLifecycle(initialValue = null)
            val lockNow = lockSettings?.lockEnabled ?: lockOn
            val privacyNow = lockSettings?.privacyMode ?: privacy
            val appState = rememberSaveableStateHolder()
            SideEffect {
                if (lockOn != lockNow || privacy != privacyNow) applyPrivacy(lockNow, privacyNow)
                lockOn = lockNow
                privacy = privacyNow
                lockAfter = lockSettings?.lockAfter ?: lockAfter
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
        // Girar a tela e as recriações do próprio app (língua, modo de teste) não são sair do app: com "Na hora", contariam.
        if (!authenticating && !isChangingConfigurations) {
            val now = SystemClock.elapsedRealtime()
            app.lastStop = now
            app.stopByOwnLaunch = LockRules.stopByOwnLaunch(app.ownLaunchAt, now)
        }
    }

    override fun onStart() {
        super.onStart()
        // RF-BLQ-02 e RF-BLQ-08 (ERR-070): volta a bloquear depois do tempo que a pessoa escolheu fora do app (na hora,
        // 1, 5 ou 15 minutos). Até a 1.6 era sempre 1 minuto: sair e voltar logo, ou apagar e acender a tela, nunca pedia.
        val away = if (app.lastStop > 0) SystemClock.elapsedRealtime() - app.lastStop else null
        if (LockRules.relock(lockOn, away, lockAfter, app.stopByOwnLaunch)) {
            app.unlocked = false
            locked = true
        }
        app.lastStop = 0
        app.stopByOwnLaunch = false
        app.ownLaunchAt = 0
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
                    val secure = runCatching { getSystemService(android.app.KeyguardManager::class.java)?.isDeviceSecure }.getOrNull() ?: true
                    if (LockPolicy.unlockOnError(errorCode, secure)) unlock()
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

    /** Desbloqueado (digital, rosto ou senha do celular): vale até a pessoa sair do app pelo tempo escolhido (RF-BLQ-02). */
    @VisibleForTesting
    internal fun unlock() {
        // Desbloqueado, não há mais pergunta em andamento: a próxima saída do app conta (RF-BLQ-02). Sem isso, um
        // desbloqueio que não passasse pelo retorno da digital deixaria o app achando que ainda pergunta, e ele nunca
        // mais voltaria a bloquear nesta sessão.
        authenticating = false
        app.unlocked = true
        locked = false
    }

    /**
     * RF-BLQ-05 e RF-BLQ-07: com o bloqueio ligado, a miniatura do app em Recentes (que mostra a última tela, com saldos e
     * movimentações) não é guardada (Android 13 ou mais novo). Com o modo privacidade, a janela inteira fica protegida:
     * prints, gravações e o compartilhamento de tela saem pretos, como nos apps de banco. Folhas, diálogos e menus herdam a
     * proteção (o padrão do Compose é seguir a janela de baixo). No Android 8 a 12 não há como tirar só a miniatura: ali,
     * com o bloqueio ligado, a janela fica sempre protegida.
     */
    private fun applyPrivacy(lock: Boolean, privacyMode: Boolean) {
        if (Build.VERSION.SDK_INT >= 33) runCatching { setRecentsScreenshotEnabled(!lock) }
        runCatching {
            val flag = android.view.WindowManager.LayoutParams.FLAG_SECURE
            if (LockRules.secure(lock, privacyMode, Build.VERSION.SDK_INT)) window.addFlags(flag) else window.clearFlags(flag)
        }
    }

    /* RF-BLQ-08: o que o próprio Cryo abre (escolher arquivo, compartilhar, navegador, foto do perfil) fica marcado, para a
     * volta não pedir o desbloqueio no meio do que a pessoa está fazendo. */
    private fun markOwnLaunch() {
        app.ownLaunchAt = SystemClock.elapsedRealtime()
    }

    override fun startActivity(intent: android.content.Intent, options: Bundle?) {
        markOwnLaunch()
        super.startActivity(intent, options)
    }

    @Deprecated("Deprecated in Java")
    override fun startActivityForResult(intent: android.content.Intent, requestCode: Int, options: Bundle?) {
        markOwnLaunch()
        @Suppress("DEPRECATION")
        super.startActivityForResult(intent, requestCode, options)
    }

    @Deprecated("Deprecated in Java")
    override fun startIntentSenderForResult(
        intent: android.content.IntentSender, requestCode: Int, fillInIntent: android.content.Intent?,
        flagsMask: Int, flagsValues: Int, extraFlags: Int, options: Bundle?,
    ) {
        markOwnLaunch()
        @Suppress("DEPRECATION")
        super.startIntentSenderForResult(intent, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags, options)
    }

    private companion object {
        val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
