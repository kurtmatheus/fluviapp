package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.validacao

import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.states.passagem.FormPassagemUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validação pura dos dados da passagem (molde ADR-0006, fatia 3). Migra a cobertura do antigo
 * `ValidacaoFormPassagemHelperTest`, incluindo o lock de agência/agente (marcam mas não invalidam).
 */
class ValidacaoDadosPassagemTest {

    private val dataFutura = "31/12/2999"
    private val dataPassada = "01/01/2000"

    private fun base() = FormPassagemUiState(
        dataViagem = dataFutura,
        horaViagem = "12:00",
        isPixChecked = true,
        valorPix = "100",
    )

    @Test
    fun `data futura, hora e uma forma de pagamento com valor e valido`() {
        assertTrue(validarDadosPassagem(base(), isGratuidade = false).valido)
    }

    @Test
    fun `sem nenhuma forma de pagamento (nao gratuidade) invalida`() {
        val e = validarDadosPassagem(
            FormPassagemUiState(dataViagem = dataFutura, horaViagem = "12:00"),
            isGratuidade = false,
        )
        assertTrue(e.formaPagamento)
        assertFalse(e.valido)
    }

    @Test
    fun `gratuidade dispensa forma de pagamento`() {
        val e = validarDadosPassagem(
            FormPassagemUiState(dataViagem = dataFutura, horaViagem = "12:00"),
            isGratuidade = true,
        )
        assertFalse(e.formaPagamento)
        assertTrue(e.valido)
    }

    @Test
    fun `data no passado marca error_data_menor e invalida`() {
        val e = validarDadosPassagem(base().copy(dataViagem = dataPassada), isGratuidade = false)
        assertTrue(e.dataViagem)
        assertEquals(R.string.error_data_menor, e.textDataViagem)
        assertFalse(e.valido)
    }

    @Test
    fun `data em branco (nova) marca error_camp_obrig e invalida`() {
        val e = validarDadosPassagem(base().copy(dataViagem = ""), isGratuidade = false)
        assertTrue(e.dataViagem)
        assertEquals(R.string.error_camp_obrig, e.textDataViagem)
        assertFalse(e.valido)
    }

    @Test
    fun `em edicao a regra da data e ignorada`() {
        val e = validarDadosPassagem(
            base().copy(dataViagem = dataPassada, isEditing = true),
            isGratuidade = false,
        )
        assertFalse(e.dataViagem)
        assertTrue(e.valido)
    }

    @Test
    fun `hora em branco invalida`() {
        val e = validarDadosPassagem(base().copy(horaViagem = ""), isGratuidade = false)
        assertTrue(e.horaViagem)
        assertFalse(e.valido)
    }

    // O caso "agência e agente em branco" saiu com os campos (P2.3, ADR-0015 §3): eram validações de
    // uma área que a tela não desenhava. A agência agora é derivada do emissor, não digitada.
}