package dev.matheus.fluviapp.domain.passagem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Classe do veículo como tipo de domínio (ADR-0018 D7, entrando pela F1 do ADR-0020). */
class ClasseVeiculoTest {

    @Test
    fun `de converte o name canonico das quatro classes`() {
        assertEquals(ClasseVeiculo.CARRO, ClasseVeiculo.de("CARRO"))
        assertEquals(ClasseVeiculo.MOTO, ClasseVeiculo.de("MOTO"))
        assertEquals(ClasseVeiculo.CAMINHAO, ClasseVeiculo.de("CAMINHAO"))
        assertEquals(ClasseVeiculo.CARRETA, ClasseVeiculo.de("CARRETA"))
    }

    @Test
    fun `de tolera espaco e caixa, e recusa desconhecido`() {
        assertEquals(ClasseVeiculo.MOTO, ClasseVeiculo.de(" moto "))
        assertNull(ClasseVeiculo.de(null))
        assertNull(ClasseVeiculo.de("ONIBUS"))
    }

    @Test
    fun `so a moto exige cilindrada — e e por isso que a tarifa dela e por faixa`() {
        assertTrue(ClasseVeiculo.MOTO.exigeCilindrada)
        listOf(ClasseVeiculo.CARRO, ClasseVeiculo.CAMINHAO, ClasseVeiculo.CARRETA)
            .forEach { assertFalse(it.exigeCilindrada) }
    }

    @Test
    fun `pesado e caminhao e carreta`() {
        assertTrue(ClasseVeiculo.CAMINHAO.ehPesado)
        assertTrue(ClasseVeiculo.CARRETA.ehPesado)
        assertFalse(ClasseVeiculo.CARRO.ehPesado)
        assertFalse(ClasseVeiculo.MOTO.ehPesado)
    }
}