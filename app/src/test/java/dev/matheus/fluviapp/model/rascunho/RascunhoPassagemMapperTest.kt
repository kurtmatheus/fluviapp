package dev.matheus.fluviapp.model.rascunho

import dev.matheus.fluviapp.ui.states.passagem.FormPassageiroUiState
import dev.matheus.fluviapp.ui.states.passagem.FormPassagemUiState
import dev.matheus.fluviapp.ui.states.passagem.FormVeiculoUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class RascunhoPassagemMapperTest {

    private val passagem = FormPassagemUiState(
        dataViagem = "10/06/2024",
        horaViagem = "12:00",
        agencia = "MATRIZ",
        agente = "Agente Modelo",
        isPixChecked = true,
        valorPix = "100",
        desconto = "30",
        observacao = "obs",
        viagemId = "viagem-abc",
        navioId = "navio-xyz",
        empresaId = "empresa-123",
        codigoViagem = "PN-IC-001",
    )
    private val passageiro = FormPassageiroUiState(
        nomePassageiro1 = "Passageiro Um",
        documentoPassageiro1 = "RG",
        dataNascimentoPassageiro1 = "01/01/1990",
        acomodacao = "REDE",
        tipoPassagem = "INTEIRA",
    )
    private val veiculo = FormVeiculoUiState(
        tipoVeiculo = "CARRO",
        placaVeiculo = "ABC1D23",
    )

    @Test
    fun `montar captura os valores dos tres states`() {
        val snapshot = montarRascunho(passagem, passageiro, veiculo)

        assertEquals("10/06/2024", snapshot.dataViagem)
        assertEquals("100", snapshot.valorPix)
        assertEquals("Passageiro Um", snapshot.nomePassageiro1)
        assertEquals("REDE", snapshot.acomodacao)
        assertEquals("ABC1D23", snapshot.placaVeiculo)
    }

    @Test
    fun `aplicar restaura valores e preserva campos fora do snapshot`() {
        val snapshot = montarRascunho(passagem, passageiro, veiculo)

        // states-alvo default, mas com um campo NÃO pertencente ao snapshot marcado.
        val restaurado = snapshot.aplicarEm(
            FormPassagemUiState(titleForm = 999),
            FormPassageiroUiState(),
            FormVeiculoUiState(),
        )

        assertEquals("10/06/2024", restaurado.passagem.dataViagem)
        assertEquals(true, restaurado.passagem.isPixChecked)
        assertEquals("Passageiro Um", restaurado.passageiro.nomePassageiro1)
        assertEquals("CARRO", restaurado.veiculo.tipoVeiculo)
        // campo fora do snapshot permanece intacto (restore não clobbera).
        assertEquals(999, restaurado.passagem.titleForm)
    }

    @Test
    fun `round-trip montar - aplicar preserva os valores editados`() {
        val restaurado = montarRascunho(passagem, passageiro, veiculo)
            .aplicarEm(FormPassagemUiState(), FormPassageiroUiState(), FormVeiculoUiState())

        assertEquals(passagem.dataViagem, restaurado.passagem.dataViagem)
        assertEquals(passagem.viagemId, restaurado.passagem.viagemId)
        assertEquals(passagem.navioId, restaurado.passagem.navioId)
        assertEquals(passagem.empresaId, restaurado.passagem.empresaId)
        assertEquals(passagem.agencia, restaurado.passagem.agencia)
        assertEquals(passagem.valorPix, restaurado.passagem.valorPix)
        assertEquals(passagem.desconto, restaurado.passagem.desconto)
        assertEquals(passageiro.documentoPassageiro1, restaurado.passageiro.documentoPassageiro1)
        assertEquals(passageiro.tipoPassagem, restaurado.passageiro.tipoPassagem)
        assertEquals(veiculo.placaVeiculo, restaurado.veiculo.placaVeiculo)
    }
}