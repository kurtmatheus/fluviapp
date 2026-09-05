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
            listOf(ClasseVeiculo.MOTO, ClasseVeiculo.QUADRICICLO),
            NaturezaVeiculo.MOTOCICLO.classes,
        )
    }

    /**
     * **A natureza não decide a cilindrada**, e não decidir é a correção de 2026-09-05.
     *
     * Ela derivava daqui até a operação apontar dois fatos: o quadriciclo não a exige, e o jet-ski — que a
     * derivação usava como prova de que o traço era de família — é **rebocado**. Duas classes da mesma
     * natureza com exigências diferentes é o contraexemplo que a forma não suportava.
     *
     * O caso guarda a ausência: se `exigeCilindrada` voltar para cá, este teste não compila.
     */
    @Test
    fun `a natureza classifica, e nao decide a cilindrada`() {
        val motociclo = NaturezaVeiculo.MOTOCICLO.classes

        assertTrue(ClasseVeiculo.MOTO in motociclo)
        assertTrue(ClasseVeiculo.QUADRICICLO in motociclo)
        assertTrue("a moto exige cilindrada", ClasseVeiculo.MOTO.exigeCilindrada)
        assertFalse("o quadriciclo não exige", ClasseVeiculo.QUADRICICLO.exigeCilindrada)
    }

    /** O jet-ski é **moto aquática no nome e carga rebocada na doca** — e a natureza descreve a doca. */
    @Test
    fun `o jet-ski e rebocado, e nao motociclo`() {
        assertEquals(NaturezaVeiculo.REBOCADO, ClasseVeiculo.JET_SKI.natureza)
        assertFalse(ClasseVeiculo.JET_SKI in NaturezaVeiculo.MOTOCICLO.classes)
    }

    @Test
    fun `de tolera espaco e caixa, e recusa desconhecida`() {
        assertEquals(NaturezaVeiculo.REBOCADO, NaturezaVeiculo.de(" rebocado "))
        assertEquals(NaturezaVeiculo.MAQUINA, NaturezaVeiculo.de("maquina"))
        assertNull(NaturezaVeiculo.de(null))
        assertNull(NaturezaVeiculo.de("ANFIBIO"))
    }
}
