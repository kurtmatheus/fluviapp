package dev.matheus.fluviapp.ui.viewmodel.helpers.usuario

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * A tradução entre o **dia** que se escolhe e o **instante** que a regra compara ([ADR-0032] Q1).
 *
 * O fuso entra fixo nos casos para que a suíte não dependa do relógio da máquina — é o mesmo cuidado do
 * `FakeRelogio`, uma camada abaixo.
 */
class PrazoDeAcessoTest {

    private val zona = ZoneId.of("America/Manaus")

    /**
     * **O prazo é o fim do dia**, e é a decisão que estas funções carregam: *"expira em 31/12"* quer dizer
     * que o dia 31 ainda vale. Converter para o início do dia seria mais fácil de escrever e impossível de
     * explicar a quem perde o acesso no dia que a tela diz ser o último.
     */
    @Test
    fun `o prazo e o fim do dia escolhido`() {
        val prazo = requireNotNull(prazoEmMillis("31/12/2026", zona))

        val inicioDoDia = LocalDate.of(2026, 12, 31).atStartOfDay(zona).toInstant().toEpochMilli()
        val inicioDoSeguinte = LocalDate.of(2027, 1, 1).atStartOfDay(zona).toInstant().toEpochMilli()

        assertTrue("o dia escolhido ainda vale", prazo > inicioDoDia)
        assertTrue("e não invade o dia seguinte", prazo < inicioDoSeguinte)
    }

    @Test
    fun `ida e volta devolve o mesmo dia`() {
        assertEquals("08/09/2026", prazoFormatado(prazoEmMillis("08/09/2026", zona), zona))
    }

    /** Sem prazo, dos dois lados: é o caso normal de quem nunca teve um. */
    @Test
    fun `texto em branco e sem prazo, e sem prazo e texto vazio`() {
        assertNull(prazoEmMillis("", zona))
        assertNull(prazoEmMillis("   ", zona))
        assertEquals("", prazoFormatado(null, zona))
    }

    /**
     * Texto ilegível vira **ausência de prazo**, e não um prazo inventado: o campo é preenchido por um
     * seletor de data, então algo fora do formato só chega por caminho que não é a tela.
     */
    @Test
    fun `data ilegivel nao vira prazo`() {
        assertNull(prazoEmMillis("31/12", zona))
        assertNull(prazoEmMillis("2026-12-31", zona))
        assertNull(prazoEmMillis("32/12/2026", zona))
        assertNull(prazoEmMillis("amanhã", zona))
    }

    @Test
    fun `espacos ao redor nao atrapalham`() {
        assertEquals(prazoEmMillis("08/09/2026", zona), prazoEmMillis("  08/09/2026 ", zona))
    }

    /** O último instante do dia é o do fuso pedido — e não o de quem roda a suíte. */
    @Test
    fun `o fuso e o pedido, nao o da maquina`() {
        val esperado = LocalDate.of(2026, 12, 31)
            .atTime(LocalTime.MAX)
            .atZone(zona)
            .toInstant()
            .toEpochMilli()

        assertEquals(esperado, prazoEmMillis("31/12/2026", zona))
    }
}
