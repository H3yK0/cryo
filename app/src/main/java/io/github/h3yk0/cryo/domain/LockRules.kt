// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain

/**
 * RF-BLQ-02, RF-BLQ-07 e RF-BLQ-08 (1.7, LA2 e LA6, ERR-070): quando o Cryo volta a pedir o desbloqueio e quando a janela
 * fica protegida contra prints. Regras puras (sem Android), para testar cada caso sem celular.
 */
object LockRules {
    /** "Bloquear de novo", em segundos fora do app: na hora, 1, 5 ou 15 minutos. */
    val CHOICES = listOf(0, 60, 300, 900)

    /** O padrão continua 1 minuto, como até a 1.6: quem já usa não leva susto. */
    const val DEFAULT = 60

    /**
     * RF-BLQ-08: voltar do que o próprio Cryo abriu (escolher um arquivo, compartilhar, o navegador, a foto do perfil, a
     * confirmação do sistema) não conta como sair do app por até 5 minutos. Sem isso, "Na hora" bloquearia no meio da
     * importação ou do backup.
     */
    const val OWN_LAUNCH_GRACE_MS = 5 * 60_000L

    /** Uma abertura de tela pelo próprio Cryo vale para a saída que vem logo depois dela (até 3 s). */
    const val OWN_LAUNCH_WINDOW_MS = 3_000L

    /** Qualquer valor guardado que não seja uma das escolhas volta ao padrão. */
    fun normalize(seconds: Int): Int = if (seconds in CHOICES) seconds else DEFAULT

    /**
     * Se, ao voltar, o app precisa pedir o desbloqueio de novo. [awayMs] = quanto tempo a tela ficou fora de vista (null =
     * não saiu); [ownLaunch] = a saída foi para algo que o próprio Cryo abriu.
     */
    fun relock(lockOn: Boolean, awayMs: Long?, lockAfterSeconds: Int, ownLaunch: Boolean): Boolean {
        if (!lockOn || awayMs == null || awayMs < 0) return false
        if (ownLaunch && awayMs <= OWN_LAUNCH_GRACE_MS) return false
        val limit = normalize(lockAfterSeconds) * 1000L
        // "Na hora" (0): qualquer saída. Os outros: só depois do tempo escolhido (passar exatamente do limite bloqueia).
        return if (limit == 0L) true else awayMs > limit
    }

    /** Se a saída que acabou de acontecer foi causada por uma tela que o próprio Cryo abriu. */
    fun stopByOwnLaunch(ownLaunchAt: Long, stoppedAt: Long): Boolean =
        ownLaunchAt > 0 && stoppedAt >= ownLaunchAt && stoppedAt - ownLaunchAt <= OWN_LAUNCH_WINDOW_MS

    /**
     * RF-BLQ-07 e RF-BLQ-05: a janela fica protegida (prints, gravações e a miniatura em Recentes saem pretos). Só com o
     * bloqueio ligado. No Android 12 ou mais antigo ([sdk] < 33), é o único jeito de esconder a miniatura em Recentes, então
     * ali vale sempre que o bloqueio está ligado; no 13 ou mais novo, só com o modo privacidade.
     */
    fun secure(lockOn: Boolean, privacy: Boolean, sdk: Int): Boolean = lockOn && (privacy || sdk < 33)

    /** No Android 12 ou mais antigo, o modo privacidade não é escolha: com o bloqueio, a proteção já vem ligada. */
    fun privacyLocked(sdk: Int): Boolean = sdk < 33
}
