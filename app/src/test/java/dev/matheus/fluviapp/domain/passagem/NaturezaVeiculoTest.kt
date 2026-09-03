package dev.matheus.fluviapp.domain.passagem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A natureza do veículo ([ADR-0031] D2) — **a única coluna de comportamento** que sobrou da classe.
 *
 * Ela não existe para encurtar tabela: existe pela capacidade analítica futura (*"quantos rebocados
 * atravessaram em agosto"*), e organizar o código é o efeito colateral. Os casos abaixo guardam as duas
 * coisas — que ela **classifica** todas as dezessete, e que **decide** o que se pergunta.
 */
class NaturezaVeiculoTest {

    @Test
    fun `sao quatro naturezas, e toda uma tem rotulo`() {
        assertEquals(4, NaturezaVeiculo.entries.size)
        NaturezaVeiculo.entries.forEach { assertTrue("${it.name} sem rótulo", it.rotulo.isNotBlank()) }
    }

    /** A partição é total e sem sobra: toda classe cai numa natureza, e nenhuma natureza fica vazia. */
    @Test
    fun `a particao cobre todas as classes e nenhuma natureza fica vazia`() {
        assertEquals(ClasseVeiculo.entries.toSet(), NaturezaVeiculo.entries.flatMap { it.classes }.toSet())
        NaturezaVeiculo.entries.forEach {
            assertTrue("a natureza $it não tem classe", it.classes.isNotEmpty())
        }
    }

    /** `classes` respeita a ordem do enum — é ela que o passo da emissão apresenta. */
    @Test
    fun `as classes vem na ordem do enum`() {
        assertEquals(
            listOf(ClasseVeiculo.MOTO, ClasseVeiculo.QUADRICICLO, ClasseVeiculo.JET_SKI),
            NaturezaVeiculo.MOTOCICLO.classes,
        )
    }

    /**
     * **Só o motociclo mede motor.** Nas outras três a cilindrada não é opcional: é *sem sentido* — e é por
     * isso que o formulário não a pergunta, em vez de deixá-la em branco.
     */
    @Test
    fun `so o motociclo exige cilindrada`() {
        assertTrue(NaturezaVeiculo.MOTOCICLO.exigeCilindrada)
        listOf(NaturezaVeiculo.AUTOMOTOR, NaturezaVeiculo.MAQUINA, NaturezaVeiculo.REBOCADO)
            .forEach { assertFalse("$it não deveria exigir cilindrada", it.exigeCilindrada) }
    }

    @Test
    fun `de tolera espaco e caixa, e recusa desconhecida`() {
        assertEquals(NaturezaVeiculo.REBOCADO, NaturezaVeiculo.de(" rebocado "))
        assertEquals(NaturezaVeiculo.MAQUINA, NaturezaVeiculo.de("maquina"))
        assertNull(NaturezaVeiculo.de(null))
        assertNull(NaturezaVeiculo.de("ANFIBIO"))
    }
}
