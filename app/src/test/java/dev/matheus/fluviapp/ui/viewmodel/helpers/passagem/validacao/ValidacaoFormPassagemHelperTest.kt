package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.validacao

import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.states.passagem.FormPassageiroUiState
import dev.matheus.fluviapp.ui.states.passagem.FormPassagemUiState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidacaoFormPassagemHelperTest {

    private val dataFutura = "31/12/2999"
    private val dataPassada = "01/01/2000"

    private fun validar(passagem: FormPassagemUiState): Pair<Boolean, FormPassagemUiState> {
        val flowPassagem = MutableStateFlow(passagem)
        val flowPassageiro = MutableStateFlow(FormPassageiroUiState())
        val valido = ValidacaoFormPassagemHelper(flowPassagem, flowPassageiro).isFormularioPassagemValido()
        return valido to flowPassagem.value
    }

    /** Forma de pagamento é sempre habilitada no form (isFormaPagamentoEnabled = true hardcoded). */
    @Test
    fun `data futura, hora e uma forma de pagamento com valor e valido`() {
        val (valido, _) = validar(
            FormPassagemUiState(
                dataViagem = dataFutura,
                horaViagem = "12:00",
                isPixChecked = true,
                valorPix = "100",
            ),
        )
        assertTrue(valido)
    }

    @Test
    fun `sem nenhuma forma de pagamento marcada (nao gratuidade) invalida`() {
        val (valido, s) = validar(
            FormPassagemUiState(dataViagem = dataFutura, horaViagem = "12:00"),
        )
        assertFalse(valido)
        assertTrue(s.isFormaPagamentoError)
    }

    @Test
    fun `data no passado marca error_data_menor e invalida`() {
        val (valido, s) = validar(
            FormPassagemUiState(
                dataViagem = dataPassada,
                horaViagem = "12:00",
                isPixChecked = true,
                valorPix = "100",
            ),
        )
        assertFalse(valido)
        assertTrue(s.isDataViagemError)
        assertEquals(R.string.error_data_menor, s.textDataViagemError)
    }

    @Test
    fun `data em branco (nova) marca error_camp_obrig e invalida`() {
        val (valido, s) = validar(
            FormPassagemUiState(
                dataViagem = "",
                horaViagem = "12:00",
                isPixChecked = true,
                valorPix = "100",
            ),
        )
        assertFalse(valido)
        assertTrue(s.isDataViagemError)
        assertEquals(R.string.error_camp_obrig, s.textDataViagemError)
    }

    @Test
    fun `em edicao, data no passado NAO invalida (regra da data e ignorada ao editar)`() {
        val (valido, s) = validar(
            FormPassagemUiState(
                dataViagem = dataPassada,
                horaViagem = "12:00",
                isPixChecked = true,
                valorPix = "100",
                isEditing = true,
            ),
        )
        assertFalse(s.isDataViagemError)
        assertTrue(valido)
    }

    @Test
    fun `hora em branco invalida`() {
        val (valido, s) = validar(
            FormPassagemUiState(
                dataViagem = dataFutura,
                horaViagem = "",
                isPixChecked = true,
                valorPix = "100",
            ),
        )
        assertFalse(valido)
        assertTrue(s.isHoraViagemError)
    }

    /** Lock de comportamento sutil: agência/agente marcam erro mas NÃO entram no veredito. */
    @Test
    fun `agencia e agente em branco marcam erro mas nao invalidam`() {
        val (valido, s) = validar(
            FormPassagemUiState(
                dataViagem = dataFutura,
                horaViagem = "12:00",
                isPixChecked = true,
                valorPix = "100",
                agencia = "",
                agente = "",
            ),
        )
        assertTrue(valido)
        assertTrue(s.isAgenciaError)
        assertTrue(s.isAgenteError)
    }
}