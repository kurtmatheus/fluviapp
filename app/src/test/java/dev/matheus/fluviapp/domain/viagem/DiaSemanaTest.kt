package dev.matheus.fluviapp.domain.viagem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek

/**
 * As **duas fronteiras** do dia da semana, separadas como em `TipoEmbarcacao`: o Firestore grava o `name`
 * (estável), a tela devolve o rótulo (reescrevível sem migrar dado).
 */
class DiaSemanaTest {

    @Test
    fun `os sete dias tem rotulo, comecando na segunda`() {
        assertEquals(7, DIAS_DA_SEMANA.size)
        assertEquals(DayOfWeek.MONDAY, DIAS_DA_SEMANA.first())
        assertEquals(DayOfWeek.SUNDAY, DIAS_DA_SEMANA.last())
        assertEquals("Segunda-feira", DayOfWeek.MONDAY.rotulo)
        assertEquals("Sábado", DayOfWeek.SATURDAY.rotulo)
    }

    @Test
    fun `a forma curta serve o card`() {
        assertEquals("Ter", DayOfWeek.TUESDAY.rotuloCurto)
        assertEquals("Dom", DayOfWeek.SUNDAY.rotuloCurto)
    }

    @Test
    fun `o rotulo volta a ser tipo`() {
        DIAS_DA_SEMANA.forEach { dia ->
            assertEquals(dia, diaSemanaPorRotulo(dia.rotulo))
        }
    }

    @Test
    fun `tolera caixa e espaco, como o dropdown devolve`() {
        assertEquals(DayOfWeek.TUESDAY, diaSemanaPorRotulo(" terça-feira "))
    }

    /**
     * Fail-closed: texto desconhecido não vira segunda-feira por descuido. O `name` do Firestore também
     * **não** resolve aqui — é a outra fronteira, e confundi-las ataria a persistência ao texto da tela.
     */
    @Test
    fun `texto que nao e rotulo devolve nulo`() {
        assertNull(diaSemanaPorRotulo(null))
        assertNull(diaSemanaPorRotulo(""))
        assertNull(diaSemanaPorRotulo("Terça"))
        assertNull(diaSemanaPorRotulo("TUESDAY"))
    }
}