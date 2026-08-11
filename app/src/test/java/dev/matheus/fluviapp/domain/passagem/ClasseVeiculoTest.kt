package dev.matheus.fluviapp.domain.passagem

import dev.matheus.fluviapp.revitalizacao.ForaDoEscopo
import org.junit.experimental.categories.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Classe do veículo como tipo de domínio (ADR-0018 D7, entrando pela F1 do ADR-0020).
 *
 * **A F9.1 acrescentou duas classes e uma regra** (ADR-0023 D4): `VAN` e `SUV`, que têm modelo nomeado, e
 * `exigeModelo` — falso em carreta e caminhão, onde *o tipo já é o modelo*. É a correção, na origem, da primeira
 * divergência do ADR-0018 D19.
 */
@Category(ForaDoEscopo::class)
class ClasseVeiculoTest {

    @Test
    fun `de converte o name canonico das seis classes`() {
        assertEquals(ClasseVeiculo.CARRO, ClasseVeiculo.de("CARRO"))
        assertEquals(ClasseVeiculo.MOTO, ClasseVeiculo.de("MOTO"))
        assertEquals(ClasseVeiculo.VAN, ClasseVeiculo.de("VAN"))
        assertEquals(ClasseVeiculo.SUV, ClasseVeiculo.de("SUV"))
        assertEquals(ClasseVeiculo.CAMINHAO, ClasseVeiculo.de("CAMINHAO"))
        assertEquals(ClasseVeiculo.CARRETA, ClasseVeiculo.de("CARRETA"))
    }

    @Test
    fun `de tolera espaco e caixa, e recusa desconhecido`() {
        assertEquals(ClasseVeiculo.MOTO, ClasseVeiculo.de(" moto "))
        assertEquals(ClasseVeiculo.SUV, ClasseVeiculo.de("suv"))
        assertNull(ClasseVeiculo.de(null))
        assertNull(ClasseVeiculo.de("ONIBUS"))
    }

    @Test
    fun `so a moto exige cilindrada — e e por isso que a tarifa dela e por faixa`() {
        assertTrue(ClasseVeiculo.MOTO.exigeCilindrada)
        listOf(ClasseVeiculo.CARRO, ClasseVeiculo.VAN, ClasseVeiculo.SUV, ClasseVeiculo.CAMINHAO, ClasseVeiculo.CARRETA)
            .forEach { assertFalse("$it não deveria exigir cilindrada", it.exigeCilindrada) }
    }

    /**
     * O caso que o validador de hoje errava: ele exigia modelo **sempre**, então **carreta e caminhão não
     * passavam**. Perguntar o modelo de uma carreta é perguntar duas vezes a mesma coisa.
     */
    @Test
    fun `carreta e caminhao nao exigem modelo - o tipo ja e o modelo`() {
        assertFalse(ClasseVeiculo.CARRETA.exigeModelo)
        assertFalse(ClasseVeiculo.CAMINHAO.exigeModelo)
    }

    @Test
    fun `carro moto van e suv exigem modelo`() {
        listOf(ClasseVeiculo.CARRO, ClasseVeiculo.MOTO, ClasseVeiculo.VAN, ClasseVeiculo.SUV)
            .forEach { assertTrue("$it deveria exigir modelo", it.exigeModelo) }
    }

    /** As duas regras não são a mesma: a moto exige as duas coisas, e é a única. */
    @Test
    fun `exigir modelo e exigir cilindrada sao eixos independentes`() {
        assertTrue(ClasseVeiculo.MOTO.exigeModelo && ClasseVeiculo.MOTO.exigeCilindrada)
        assertTrue(ClasseVeiculo.CARRO.exigeModelo && !ClasseVeiculo.CARRO.exigeCilindrada)
        assertTrue(!ClasseVeiculo.CARRETA.exigeModelo && !ClasseVeiculo.CARRETA.exigeCilindrada)
    }

    @Test
    fun `pesado e caminhao e carreta`() {
        assertTrue(ClasseVeiculo.CAMINHAO.ehPesado)
        assertTrue(ClasseVeiculo.CARRETA.ehPesado)
        assertFalse(ClasseVeiculo.CARRO.ehPesado)
        assertFalse(ClasseVeiculo.MOTO.ehPesado)
    }

    /** Van e SUV são de porte de automóvel: entram onde o carro entra, e não no recorte da balsa. */
    @Test
    fun `van e suv nao sao pesados`() {
        assertFalse(ClasseVeiculo.VAN.ehPesado)
        assertFalse(ClasseVeiculo.SUV.ehPesado)
    }
}
