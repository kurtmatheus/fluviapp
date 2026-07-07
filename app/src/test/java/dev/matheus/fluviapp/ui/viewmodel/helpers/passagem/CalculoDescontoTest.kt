package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem

import dev.matheus.fluviapp.model.cadastro.constantes.Constante
import dev.matheus.fluviapp.ui.states.passagem.FormPassageiroUiState
import dev.matheus.fluviapp.ui.states.passagem.FormPassagemUiState
import org.junit.Assert.assertEquals
import org.junit.Test

/** Rede de regressão: regra de desconto ANTAC (DESCONTO_ANTAC = "50"). */
class CalculoDescontoTest {

    private val rede = Constante.Descricao.REDE.name
    private val meia = Constante.Descricao.MEIA.name
    private val gratuidade = Constante.Descricao.GRATUIDADE.name

    @Test
    fun `rede, inteira, nova passagem sem desconto informado acumula 50`() {
        val desconto = calcularDesconto(
            FormPassageiroUiState(acomodacao = rede),
            FormPassagemUiState(isEditing = false, desconto = ""),
        )
        assertEquals(50.0, desconto, 0.0)
    }

    @Test
    fun `rede e meia-passagem acumula metade (25)`() {
        val desconto = calcularDesconto(
            FormPassageiroUiState(acomodacao = rede, tipoPassagem = meia),
            FormPassagemUiState(isEditing = false, desconto = ""),
        )
        assertEquals(25.0, desconto, 0.0)
    }

    @Test
    fun `rede soma o ANTAC ao desconto ja informado`() {
        val desconto = calcularDesconto(
            FormPassageiroUiState(acomodacao = rede),
            FormPassagemUiState(isEditing = false, desconto = "30"),
        )
        assertEquals(80.0, desconto, 0.0)
    }

    @Test
    fun `em edicao nao acumula ANTAC, mantem so o informado`() {
        val desconto = calcularDesconto(
            FormPassageiroUiState(acomodacao = rede),
            FormPassagemUiState(isEditing = true, desconto = "30"),
        )
        assertEquals(30.0, desconto, 0.0)
    }

    @Test
    fun `gratuidade nao acumula ANTAC`() {
        val desconto = calcularDesconto(
            FormPassageiroUiState(acomodacao = rede, tipoPassagem = gratuidade),
            FormPassagemUiState(isEditing = false, desconto = ""),
        )
        assertEquals(0.0, desconto, 0.0)
    }

    @Test
    fun `sem acomodacao rede mantem so o desconto informado`() {
        val desconto = calcularDesconto(
            FormPassageiroUiState(acomodacao = ""),
            FormPassagemUiState(isEditing = false, desconto = "30"),
        )
        assertEquals(30.0, desconto, 0.0)
    }
}