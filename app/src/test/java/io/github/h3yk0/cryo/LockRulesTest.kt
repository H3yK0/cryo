// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import io.github.h3yk0.cryo.domain.LockRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** RF-BLQ-02, RF-BLQ-07 e RF-BLQ-08 (1.7, LA2 e LA6, ERR-070): as regras puras do bloqueio por tempo e do modo privacidade. */
class LockRulesTest {
    /** ERR-070: o tempo escolhido manda na volta; "Na hora" pede sempre (até a 1.6 era sempre 1 minuto). */
    @Test fun tempoEscolhidoMandaNaVolta() {
        // Na hora: qualquer saída, até a de meio segundo (apagar e acender a tela).
        assertTrue(LockRules.relock(lockOn = true, awayMs = 500, lockAfterSeconds = 0, ownLaunch = false))
        assertTrue(LockRules.relock(true, 0, 0, false))
        // 1 minuto: 30 s não, 61 s sim; exatamente 60 s ainda não.
        assertFalse(LockRules.relock(true, 30_000, 60, false))
        assertFalse(LockRules.relock(true, 60_000, 60, false))
        assertTrue(LockRules.relock(true, 61_000, 60, false))
        // 5 e 15 minutos.
        assertFalse(LockRules.relock(true, 4 * 60_000L, 300, false))
        assertTrue(LockRules.relock(true, 5 * 60_000L + 1, 300, false))
        assertFalse(LockRules.relock(true, 14 * 60_000L, 900, false))
        assertTrue(LockRules.relock(true, 15 * 60_000L + 1, 900, false))
    }

    @Test fun semBloqueioOuSemSaidaNuncaPede() {
        assertFalse(LockRules.relock(lockOn = false, awayMs = 3_600_000, lockAfterSeconds = 0, ownLaunch = false))
        assertFalse(LockRules.relock(true, null, 0, false))
        assertFalse(LockRules.relock(true, -5, 0, false))
    }

    /** RF-BLQ-08: voltar do que o próprio Cryo abriu não bloqueia por até 5 minutos; depois disso, vale a regra normal. */
    @Test fun voltarDoQueOCryoAbriuTemFolga() {
        assertFalse(LockRules.relock(true, 10_000, 0, ownLaunch = true))
        assertFalse(LockRules.relock(true, LockRules.OWN_LAUNCH_GRACE_MS, 0, ownLaunch = true))
        assertTrue(LockRules.relock(true, LockRules.OWN_LAUNCH_GRACE_MS + 1, 0, ownLaunch = true))
        // A saída conta como "do Cryo" só se veio logo depois de ele abrir a outra tela.
        assertTrue(LockRules.stopByOwnLaunch(ownLaunchAt = 1_000, stoppedAt = 1_400))
        assertFalse(LockRules.stopByOwnLaunch(ownLaunchAt = 1_000, stoppedAt = 1_000 + LockRules.OWN_LAUNCH_WINDOW_MS + 1))
        assertFalse(LockRules.stopByOwnLaunch(ownLaunchAt = 0, stoppedAt = 500))
        assertFalse(LockRules.stopByOwnLaunch(ownLaunchAt = 2_000, stoppedAt = 1_000))
    }

    /** RF-BLQ-07 e RF-BLQ-05: quando a janela fica protegida contra prints. */
    @Test fun janelaProtegida() {
        // Android 13+: só com o modo privacidade.
        assertTrue(LockRules.secure(lockOn = true, privacy = true, sdk = 35))
        assertFalse(LockRules.secure(lockOn = true, privacy = false, sdk = 35))
        assertFalse(LockRules.secure(lockOn = false, privacy = true, sdk = 35))
        // Android 8 a 12: com o bloqueio, sempre (é o único jeito de esconder a miniatura em Recentes).
        assertTrue(LockRules.secure(lockOn = true, privacy = false, sdk = 32))
        assertFalse(LockRules.secure(lockOn = false, privacy = false, sdk = 26))
        assertTrue(LockRules.privacyLocked(32))
        assertFalse(LockRules.privacyLocked(33))
    }

    @Test fun valorGuardadoEstranhoVoltaAoPadrao() {
        assertEquals(LockRules.DEFAULT, LockRules.normalize(42))
        assertEquals(0, LockRules.normalize(0))
        assertEquals(900, LockRules.normalize(900))
        assertEquals(60, LockRules.DEFAULT)
    }
}
