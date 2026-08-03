package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.validacao

import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.states.passagem.FormPassageiroUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validação pura do sub-form de passageiro (molde ADR-0006, fatia 2). Migra a cobertura do antigo
 * `ValidacaoFormPassageiroHelperTest` (impuro) + a regra de idade da criança (CRIANCA_ATE_5, ADR-0013).
 */
class ValidacaoPassageiroTest {

    private val dataViagem = "10/06/2024"

    private fun p1Valido() = FormPassageiroUiState(
        acomodacao = "REDE",
        tipoPassagem = "INTEIRA",
        nomePassageiro1 = "Fulano de Tal",
        dataNascimentoPassageiro1 = "01/01/1990",
    )

    @Test
    fun `passageiro 1 completo (sem 2 e 3) e valido`() {
        assertTrue(validarPassageiro(p1Valido(), dataViagem).valido)
    }

    // --- documento: validade, não só presença (ADR-0020 D2) ---

    @Test
    fun `cpf valido do titular passa`() {
        val e = validarPassageiro(
            p1Valido().copy(
                tipoDocumentoPassageiro1 = "CPF",
                documentoPassageiro1 = "529.982.247-25",
            ),
            dataViagem,
        )
        assertFalse(e.documentoP1)
        assertTrue(e.valido)
    }

    @Test
    fun `cpf invalido do titular acusa, com a mensagem de invalido`() {
        val e = validarPassageiro(
            p1Valido().copy(
                tipoDocumentoPassageiro1 = "CPF",
                documentoPassageiro1 = "000.000.000-00",
            ),
            dataViagem,
        )
        assertTrue(e.documentoP1)
        assertEquals(R.string.error_documento_invalido, e.textDocumentoP1)
        assertFalse(e.valido)
    }

    @Test
    fun `documento de acompanhante nao marcado nao e validado`() {
        val e = validarPassageiro(
            p1Valido().copy(
                isPassageiro2Checked = false,
                tipoDocumentoPassageiro2 = "CPF",
                documentoPassageiro2 = "000.000.000-00",
            ),
            dataViagem,
        )
        assertFalse(e.documentoP2)
        assertTrue(e.valido)
    }

    @Test
    fun `obrigatorios em branco invalidam com msg de obrigatorio na data`() {
        val e = validarPassageiro(FormPassageiroUiState(), dataViagem)
        assertTrue(e.acomodacao)
        // Tipo tarifário só é exigido na REDE (ADR-0013); acomodação em branco não é rede.
        assertFalse(e.tipoPassagem)
        assertTrue(e.nomeP1)
        assertTrue(e.dataNascimentoP1)
        assertEquals(R.string.error_camp_obrig, e.textDataNascimentoP1)
        assertFalse(e.valido)
    }

    @Test
    fun `rede exige tipo tarifario`() {
        val e = validarPassageiro(p1Valido().copy(tipoPassagem = ""), dataViagem)
        assertTrue(e.tipoPassagem)
        assertFalse(e.valido)
    }

    @Test
    fun `fora da rede nao exige tipo tarifario nem gratuidade`() {
        val e = validarPassageiro(p1Valido().copy(acomodacao = "SUITE", tipoPassagem = ""), dataViagem)
        assertFalse(e.tipoPassagem)
        assertFalse(e.tipoGratuidade)
        assertTrue(e.valido)
    }

    @Test
    fun `gratuidade crianca ate 5 anos e valida`() {
        val e = validarPassageiro(
            p1Valido().copy(
                tipoPassagem = "GRATUIDADE",
                tipoGratuidade = "CRIANCA_ATE_5",
                dataNascimentoPassageiro1 = "01/01/2020", // ~4 anos na viagem de 10/06/2024
            ),
            dataViagem,
        )
        assertFalse(e.dataNascimentoP1)
        assertTrue(e.valido)
    }

    @Test
    fun `gratuidade crianca acima do limite marca erro data_crianca`() {
        val e = validarPassageiro(
            p1Valido().copy(
                tipoPassagem = "GRATUIDADE",
                tipoGratuidade = "CRIANCA_ATE_5",
                dataNascimentoPassageiro1 = "01/01/2010", // ~14 anos
            ),
            dataViagem,
        )
        assertTrue(e.dataNascimentoP1)
        assertEquals(R.string.error_data_crianca, e.textDataNascimentoP1)
        assertFalse(e.valido)
    }

    @Test
    fun `passageiro 2 marcado sem nome e documento invalida`() {
        val e = validarPassageiro(
            p1Valido().copy(
                isPassageiro2Checked = true,
                nomePassageiro2 = "",
                documentoPassageiro2 = "",
                dataNascimentoPassageiro2 = "",
            ),
            dataViagem,
        )
        assertTrue(e.nomeP2)
        assertTrue(e.dataNascimentoP2)
        assertFalse(e.valido)
    }

    @Test
    fun `passageiro 2 nao marcado nao gera erro`() {
        val e = validarPassageiro(p1Valido().copy(isPassageiro2Checked = false), dataViagem)
        assertFalse(e.nomeP2)
        assertFalse(e.dataNascimentoP2)
        assertTrue(e.valido)
    }

    @Test
    fun `doc do titular exige numero quando o tipo foi escolhido`() {
        val e = validarPassageiro(
            p1Valido().copy(tipoDocumentoPassageiro1 = "CPF", documentoPassageiro1 = ""),
            dataViagem,
        )
        assertTrue(e.documentoP1)
        assertFalse(e.valido)
    }
}