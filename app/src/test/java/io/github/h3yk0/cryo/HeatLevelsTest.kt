// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import io.github.h3yk0.cryo.ui.components.heatLevels
import io.github.h3yk0.cryo.ui.components.weekInitials
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/** RF-ANA-02 e DS-COR-06 (1.7, LA6 da 1.7): os seis níveis de temperatura do calendário de calor e as iniciais da semana. */
class HeatLevelsTest {
    /**
     * A régua é a mediana dos dias com gasto do próprio mês (aqui 75,00): até a metade dela, azul-claro (1); até ela, azul
     * (2); até 1,5×, amarelo (3); até 2,5×, laranja (4); acima, vermelho (5). Sem gasto, 0.
     */
    @Test fun seisNiveisPelaMedianaDoMes() {
        // O índice 0 não é dia; os dias 1..10.
        val daily = longArrayOf(0, 0, 1_000, 2_500, 4_000, 5_000, 7_500, 9_000, 12_000, 13_000, 20_000)
        assertArrayEquals(intArrayOf(0, 0, 1, 1, 2, 2, 2, 3, 4, 4, 5), heatLevels(daily))
    }

    @Test fun osLimitesContamParaONivelDeBaixo() {
        // Mediana 10,00: exatamente a metade é azul-claro, exatamente 1,5× é amarelo e exatamente 2,5× é laranja.
        val daily = longArrayOf(0, 500, 1_000, 1_000, 1_000, 1_500, 2_500, 2_501)
        assertArrayEquals(intArrayOf(0, 1, 2, 2, 2, 3, 4, 5), heatLevels(daily))
    }

    @Test fun mesSemGastoEDiaUnico() {
        assertTrue(heatLevels(LongArray(31)).all { it == 0 })
        // Um dia só: ele é a própria mediana, azul (nada de alarme no primeiro gasto do mês).
        val one = LongArray(31).also { it[5] = 12_345 }
        assertEquals(2, heatLevels(one)[5])
    }

    /** ERR-067: as iniciais dos dias da semana vêm da língua do app (eram "D S T Q Q S S" em todas as línguas). */
    @Test fun iniciaisDaSemanaNaLinguaDoApp() {
        assertEquals(listOf("D", "S", "T", "Q", "Q", "S", "S"), weekInitials(Locale.forLanguageTag("pt-BR")))
        assertEquals(listOf("S", "M", "T", "W", "T", "F", "S"), weekInitials(Locale.US))
        assertEquals(listOf("日", "月", "火", "水", "木", "金", "土"), weekInitials(Locale.JAPAN))
    }
}
