package dev.matheus.fluviapp.domain.veiculo

import dev.matheus.fluviapp.domain.passagem.ClasseVeiculo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * O veículo como entidade de pool (ADR-0018 D5), com a regra governada pelo **tipo** (ADR-0023 D4).
 *
 * O caso que dá razão a este arquivo existir é o da **carreta**: o validador de hoje exige modelo *sempre*
 * (`ValidacaoVeiculo.kt:50`), de modo que carreta e caminhão **não passam** — é a primeira divergência do
 * ADR-0018 D19, e aqui ela está corrigida na origem.
 *
 * **De volta ao escopo na F9.6**: o portador acendeu (ver `AcomodacaoTest`).
 */
class VeiculoTest {

    @Test
    fun `carreta sem modelo esta completa - o tipo ja e o modelo`() {
        val carreta = Veiculo(placa = "ABC-1234", tipo = ClasseVeiculo.CARRETA, cor = "Branca")

        assertTrue(carreta.completo)
        assertEquals(emptySet<Veiculo.Pendencia>(), carreta.pendencias())
    }

    @Test
    fun `caminhao sem modelo esta completo`() {
        val caminhao = Veiculo(placa = "ABC-1234", tipo = ClasseVeiculo.CAMINHAO)

        assertTrue(caminhao.completo)
    }

    @Test
    fun `carro sem modelo tem pendencia de modelo`() {
        val carro = Veiculo(placa = "ABC-1234", tipo = ClasseVeiculo.CARRO)

        assertEquals(setOf(Veiculo.Pendencia.MODELO), carro.pendencias())
        assertFalse(carro.completo)
    }

    @Test
    fun `van e suv pedem modelo, como o carro`() {
        listOf(ClasseVeiculo.VAN, ClasseVeiculo.SUV).forEach { tipo ->
            assertEquals(setOf(Veiculo.Pendencia.MODELO), Veiculo(placa = "ABC-1234", tipo = tipo).pendencias())
        }
    }

    @Test
    fun `moto sem cilindrada tem pendencia de cilindrada`() {
        val moto = Veiculo(placa = "ABC-1234", tipo = ClasseVeiculo.MOTO, modelo = "CG 160")

        assertEquals(setOf(Veiculo.Pendencia.CILINDRADA), moto.pendencias())
    }

    /** Cilindrada zero ou negativa não é cilindrada informada — é campo preenchido com nada. */
    @Test
    fun `cilindrada zero conta como ausente`() {
        val moto = Veiculo(placa = "ABC-1234", tipo = ClasseVeiculo.MOTO, modelo = "CG 160", cilindrada = 0)

        assertEquals(setOf(Veiculo.Pendencia.CILINDRADA), moto.pendencias())
    }

    @Test
    fun `moto completa tem modelo e cilindrada`() {
        val moto = Veiculo(placa = "ABC-1234", tipo = ClasseVeiculo.MOTO, modelo = "CG 160", cilindrada = 162)

        assertTrue(moto.completo)
    }

    /** Só a moto exige cilindrada: pedir ao carro seria inventar exigência. */
    @Test
    fun `carro com modelo nao precisa de cilindrada`() {
        val carro = Veiculo(placa = "ABC-1234", tipo = ClasseVeiculo.CARRO, modelo = "Onix")

        assertTrue(carro.completo)
    }

    @Test
    fun `placa em branco e pendencia - ela e a chave natural`() {
        val semPlaca = Veiculo(placa = "  ", tipo = ClasseVeiculo.CARRETA)

        assertTrue(Veiculo.Pendencia.PLACA in semPlaca.pendencias())
    }

    @Test
    fun `pendencias acumulam`() {
        val vazio = Veiculo(placa = "", tipo = ClasseVeiculo.MOTO)

        assertEquals(
            setOf(Veiculo.Pendencia.PLACA, Veiculo.Pendencia.MODELO, Veiculo.Pendencia.CILINDRADA),
            vazio.pendencias(),
        )
    }

    @Test
    fun `descricao usa o modelo quando existe e o tipo quando nao`() {
        assertEquals("Onix", Veiculo(placa = "A", tipo = ClasseVeiculo.CARRO, modelo = "Onix").descricao)
        assertEquals("Carreta", Veiculo(placa = "A", tipo = ClasseVeiculo.CARRETA).descricao)
        assertEquals("Carreta", Veiculo(placa = "A", tipo = ClasseVeiculo.CARRETA, modelo = " ").descricao)
    }
}
