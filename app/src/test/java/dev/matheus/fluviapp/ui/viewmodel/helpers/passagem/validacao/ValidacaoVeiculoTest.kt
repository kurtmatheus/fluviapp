package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.validacao

import dev.matheus.fluviapp.ui.states.passagem.FormVeiculoUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validação pura do sub-form de veículo (molde ADR-0006). Primeira fatia da refatoração do form de
 * passagem para validação pura (espelha `ValidacaoViagemTest`).
 */
class ValidacaoVeiculoTest {

    private fun veiculoValido() = FormVeiculoUiState(
        tipoVeiculo = "CARRO",
        modeloVeiculo = "Modelo",
        placaVeiculo = "ABC1D23",
        isDocumentoResponsavelRetiradaReadOnly = true, // responsável opcional, sem doc
    )

    @Test
    fun `veiculo completo e valido`() {
        assertTrue(validarVeiculo(veiculoValido()).valido)
    }

    @Test
    fun `tipo, modelo e placa em branco sao invalidos`() {
        val erros = validarVeiculo(FormVeiculoUiState(isDocumentoResponsavelRetiradaReadOnly = true))
        assertTrue(erros.tipoVeiculo)
        assertTrue(erros.modeloVeiculo)
        assertTrue(erros.placaVeiculo)
        assertFalse(erros.valido)
    }

    @Test
    fun `moto exige cilindrada`() {
        val erros = validarVeiculo(veiculoValido().copy(tipoVeiculo = "MOTO", cilindrada = ""))
        assertTrue(erros.cilindrada)
        assertFalse(erros.valido)
    }

    @Test
    fun `moto com cilindrada e valida`() {
        val erros = validarVeiculo(veiculoValido().copy(tipoVeiculo = "MOTO", cilindrada = "250"))
        assertFalse(erros.cilindrada)
        assertTrue(erros.valido)
    }

    @Test
    fun `cilindrada so vale para moto - carro sem cilindrada e valido`() {
        val erros = validarVeiculo(veiculoValido().copy(tipoVeiculo = "CARRO", cilindrada = ""))
        assertFalse(erros.cilindrada)
        assertTrue(erros.valido)
    }

    @Test
    fun `responsavel opcional - readOnly nao valida o documento`() {
        // tipo de doc não escolhido (readOnly) → documento em branco é ok.
        val erros = validarVeiculo(veiculoValido().copy(documentoResponsavelRetirada = ""))
        assertFalse(erros.documentoResponsavel)
    }

    @Test
    fun `responsavel com tipo de doc escolhido exige o numero`() {
        val erros = validarVeiculo(
            veiculoValido().copy(
                isDocumentoResponsavelRetiradaReadOnly = false,
                documentoResponsavelRetirada = "",
            ),
        )
        assertTrue(erros.documentoResponsavel)
        assertFalse(erros.valido)
    }
}