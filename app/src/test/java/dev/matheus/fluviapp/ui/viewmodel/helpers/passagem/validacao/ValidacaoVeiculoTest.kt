package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.validacao

import dev.matheus.fluviapp.revitalizacao.ForaDoEscopo
import org.junit.experimental.categories.Category
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.states.passagem.FormVeiculoUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validação pura do sub-form de veículo (molde ADR-0006). Primeira fatia da refatoração do form de
 * passagem para validação pura (espelha `ValidacaoViagemTest`).
 */
@Category(ForaDoEscopo::class)
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

    // --- documento do responsável: validade, não só presença (ADR-0020 D2) ---

    @Test
    fun `documento do responsavel com cpf valido passa`() {
        val erros = validarVeiculo(
            veiculoValido().copy(
                isDocumentoResponsavelRetiradaReadOnly = false,
                tipoDocumentoResponsavelRetirada = "CPF",
                documentoResponsavelRetirada = "529.982.247-25",
            ),
        )
        assertFalse(erros.documentoResponsavel)
        assertTrue(erros.valido)
    }

    @Test
    fun `documento do responsavel com cpf invalido acusa`() {
        val erros = validarVeiculo(
            veiculoValido().copy(
                isDocumentoResponsavelRetiradaReadOnly = false,
                tipoDocumentoResponsavelRetirada = "CPF",
                documentoResponsavelRetirada = "000.000.000-00",
            ),
        )
        assertTrue(erros.documentoResponsavel)
        assertEquals(R.string.error_documento_invalido, erros.textDocumentoResponsavel)
        assertFalse(erros.valido)
    }

    @Test
    fun `documento herdado do titular (somente-leitura) nao e revalidado`() {
        val erros = validarVeiculo(
            veiculoValido().copy(
                isDocumentoResponsavelRetiradaReadOnly = true,
                tipoDocumentoResponsavelRetirada = "CPF",
                documentoResponsavelRetirada = "000.000.000-00",
            ),
        )
        assertFalse(erros.documentoResponsavel)
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
