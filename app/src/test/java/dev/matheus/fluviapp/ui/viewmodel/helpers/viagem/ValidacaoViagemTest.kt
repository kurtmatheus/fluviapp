package dev.matheus.fluviapp.ui.viewmodel.helpers.viagem

import dev.matheus.fluviapp.revitalizacao.ForaDoEscopo
import org.junit.experimental.categories.Category
import dev.matheus.fluviapp.ui.states.FormViagemUiState
import dev.matheus.fluviapp.ui.states.TarifaInputUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@Category(ForaDoEscopo::class)
class ValidacaoViagemTest {

    private fun preenchida(tarifas: List<TarifaInputUiState>) = FormViagemUiState(
        empresa = "ACME", navio = "F/B", trechoOrigem = "A", trechoDestino = "B", tarifas = tarifas,
    )

    @Test
    fun `campos em branco sao invalidos`() {
        val erros = validarViagem(FormViagemUiState())
        assertTrue(erros.empresa)
        assertTrue(erros.navio)
        assertTrue(erros.trechoOrigem)
        assertTrue(erros.trechoDestino)
        assertFalse(erros.valido)
    }

    @Test
    fun `estado completo e valido`() {
        val erros = validarViagem(
            FormViagemUiState(
                empresa = "NAVEGACAO MODELO",
                navio = "F/B Modelo",
                trechoOrigem = "Porto Norte",
                trechoDestino = "Ilha Central",
            ),
        )
        assertTrue(erros.valido)
    }

    @Test
    fun `empresa passa a ser validada`() {
        val erros = validarViagem(
            FormViagemUiState(navio = "F/B", trechoOrigem = "A", trechoDestino = "B"),
        )
        assertTrue(erros.empresa)
        assertFalse(erros.valido)
    }

    @Test
    fun `tarifa em branco e valida (nao ofertada)`() {
        val erros = validarViagem(preenchida(listOf(TarifaInputUiState("REDE", valor = ""))))
        assertTrue(erros.tarifasInvalidas.isEmpty())
        assertTrue(erros.valido)
    }

    @Test
    fun `tarifa preenchida positiva e valida (aceita virgula)`() {
        val erros = validarViagem(
            preenchida(listOf(TarifaInputUiState("REDE", "300"), TarifaInputUiState("SUITE", "450,50"))),
        )
        assertTrue(erros.valido)
    }

    @Test
    fun `tarifa nao numerica, zero ou negativa e invalida`() {
        val erros = validarViagem(
            preenchida(
                listOf(
                    TarifaInputUiState("REDE", "abc"),
                    TarifaInputUiState("SUITE", "0"),
                    TarifaInputUiState("CAMAROTE", "-5"),
                ),
            ),
        )
        assertEquals(setOf("REDE", "SUITE", "CAMAROTE"), erros.tarifasInvalidas)
        assertFalse(erros.valido)
    }
}
