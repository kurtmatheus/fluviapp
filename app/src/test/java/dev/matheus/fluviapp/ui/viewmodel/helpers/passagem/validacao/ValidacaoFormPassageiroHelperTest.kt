package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.validacao

import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.model.cadastro.constantes.Constante
import dev.matheus.fluviapp.ui.states.passagem.FormPassageiroUiState
import dev.matheus.fluviapp.ui.states.passagem.FormPassagemUiState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidacaoFormPassageiroHelperTest {

    private val rede = Constante.Descricao.REDE.name
    private val gratuidade = Constante.Descricao.GRATUIDADE.name

    private fun validar(
        passageiro: FormPassageiroUiState,
        dataViagem: String = "10/06/2024",
    ): Pair<Boolean, FormPassageiroUiState> {
        val flowPassageiro = MutableStateFlow(passageiro)
        val flowPassagem = MutableStateFlow(FormPassagemUiState(dataViagem = dataViagem))
        val valido = ValidacaoFormPassageiroHelper(flowPassageiro, flowPassagem).isFormularioPassageiroValido()
        return valido to flowPassageiro.value
    }

    @Test
    fun `passageiro 1 completo (sem passageiro 2 e 3) e valido`() {
        val (valido, _) = validar(
            FormPassageiroUiState(
                acomodacao = rede,
                tipoPassagem = "INTEIRA",
                nomePassageiro1 = "Fulano de Tal",
                dataNascimentoPassageiro1 = "01/01/1990",
            ),
        )
        assertTrue(valido)
    }

    @Test
    fun `campos obrigatorios em branco marcam erros e invalidam`() {
        val (valido, s) = validar(FormPassageiroUiState())
        assertFalse(valido)
        assertTrue(s.isAcomodacaoError)
        assertTrue(s.isTipoPassagemError)
        assertTrue(s.isNomePassageiro1Error)
        assertTrue(s.isDataNascimentoPassageiro1Error)
        assertEquals(R.string.error_camp_obrig, s.textDataNascimentoError)
    }

    @Test
    fun `gratuidade crianca nascida ha menos de 6 anos e valida`() {
        val (valido, s) = validar(
            FormPassageiroUiState(
                acomodacao = rede,
                tipoPassagem = gratuidade,
                tipoGratuidade = "CRIANCA MENOR QUE 6 ANOS",
                nomePassageiro1 = "Crianca",
                dataNascimentoPassageiro1 = "01/01/2020", // ~4 anos na viagem de 10/06/2024
            ),
        )
        assertFalse(s.isDataNascimentoPassageiro1Error)
        assertTrue(valido)
    }

    @Test
    fun `gratuidade crianca com mais de 6 anos marca erro data_crianca`() {
        val (valido, s) = validar(
            FormPassageiroUiState(
                acomodacao = rede,
                tipoPassagem = gratuidade,
                tipoGratuidade = "CRIANCA MENOR QUE 6 ANOS",
                nomePassageiro1 = "Crianca",
                dataNascimentoPassageiro1 = "01/01/2010", // ~14 anos na viagem de 10/06/2024
            ),
        )
        assertFalse(valido)
        assertTrue(s.isDataNascimentoPassageiro1Error)
        assertEquals(R.string.error_data_crianca, s.textDataNascimentoError)
    }

    @Test
    fun `passageiro 2 marcado sem nome e documento invalida`() {
        val (valido, s) = validar(
            FormPassageiroUiState(
                acomodacao = rede,
                tipoPassagem = "INTEIRA",
                nomePassageiro1 = "Fulano",
                dataNascimentoPassageiro1 = "01/01/1990",
                isPassageiro2Checked = true,
                nomePassageiro2 = "",
                documentoPassageiro2 = "",
                dataNascimentoPassageiro2 = "",
            ),
        )
        assertFalse(valido)
        assertTrue(s.isNomePassageiro2Error)
        assertTrue(s.isDataNascimentoPassageiro2Error)
    }
}