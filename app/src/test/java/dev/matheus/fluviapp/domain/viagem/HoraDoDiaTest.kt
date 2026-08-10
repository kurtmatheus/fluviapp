package dev.matheus.fluviapp.domain.viagem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A hora como minutos-do-dia (decisão do analista, 2026-08-10): número por dentro, `HH:mm` na fronteira.
 */
class HoraDoDiaTest {

    // --- Exibição ---

    @Test
    fun `formata com dois digitos dos dois lados`() {
        assertEquals("00:00", formatarHora(0))
        assertEquals("08:05", formatarHora(8 * 60 + 5))
        assertEquals("18:30", formatarHora(18 * 60 + 30))
        assertEquals("23:59", formatarHora(MINUTOS_POR_DIA - 1))
    }

    /**
     * Acima de um dia o relógio **dá a volta**, porque é o que a chegada de uma travessia longa produz.
     * Quantos dias adiante é a `Chegada` quem diz — o relógio não tem esse dado.
     */
    @Test
    fun `hora acima de um dia da a volta no relogio`() {
        assertEquals("00:00", formatarHora(MINUTOS_POR_DIA))
        assertEquals("03:00", formatarHora(MINUTOS_POR_DIA + 3 * 60))
        assertEquals("21:00", formatarHora(2 * MINUTOS_POR_DIA + 21 * 60))
    }

    /** `floorMod`, e não `%`: um negativo com resto negativo formataria hora com sinal. */
    @Test
    fun `hora negativa tambem da a volta, sem sinal`() {
        assertEquals("23:00", formatarHora(-60))
    }

    // --- Leitura ---

    @Test
    fun `le a hora que a tela devolve`() {
        assertEquals(0, minutosDaHora("00:00"))
        assertEquals(18 * 60 + 30, minutosDaHora("18:30"))
        assertEquals(23 * 60 + 59, minutosDaHora("23:59"))
    }

    /** Sem o zero à esquerda é o que se digita. */
    @Test
    fun `aceita hora de um digito`() {
        assertEquals(8 * 60 + 5, minutosDaHora("8:05"))
    }

    @Test
    fun `ignora espaco em volta`() {
        assertEquals(6 * 60, minutosDaHora("  06:00 "))
    }

    /**
     * `null` e não zero: `"00:00"` é meia-noite, horário legítimo de saída de balsa. Confundir os dois
     * faria o formulário aceitar campo vazio como madrugada.
     */
    @Test
    fun `texto que nao e hora devolve nulo, nao zero`() {
        assertNull(minutosDaHora(""))
        assertNull(minutosDaHora("   "))
        assertNull(minutosDaHora("1830"))
        assertNull(minutosDaHora("18:30:00"))
        assertNull(minutosDaHora("dezoito e meia"))
        assertNull(minutosDaHora("18:xx"))
    }

    /** Fora do relógio não é hora mal escrita — é outra coisa. */
    @Test
    fun `recusa hora fora do relogio`() {
        assertNull(minutosDaHora("24:00"))
        assertNull(minutosDaHora("18:60"))
        assertNull(minutosDaHora("-1:00"))
    }

    @Test
    fun `ida e volta preserva o valor`() {
        (0 until MINUTOS_POR_DIA step 7).forEach { minutos ->
            assertEquals(minutos, minutosDaHora(formatarHora(minutos)))
        }
    }

    // --- Faixa ---

    @Test
    fun `hora valida e a que cabe num dia`() {
        assertTrue(horaValida(0))
        assertTrue(horaValida(MINUTOS_POR_DIA - 1))
        assertFalse(horaValida(-1))
        assertFalse(horaValida(MINUTOS_POR_DIA))
    }
}