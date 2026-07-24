package dev.matheus.fluviapp.ui.states.passagem

import androidx.room.Ignore
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.extensions.formatarDataBarrasBr
import dev.matheus.fluviapp.model.cadastro.constantes.Constante
import dev.matheus.fluviapp.model.cadastro.passagem.Agente
import java.time.LocalDate

data class FormPassagemUiState(
    val titleForm: Int = R.string.subtitle_nova_passagem,

    val isVeiculoChecked: Boolean = false,
    val onCheckVeiculo: (Boolean) -> Unit = {},

    val listaTipoDocumento: List<Constante> = emptyList(),

    val viagemId: String = "",
    val navioId: String = "",
    val empresaId: String = "",
    val empresaViagem: String = "",
    val navioViagem: String = "",
    val origemViagem: String = "",
    val destinoViagem: String = "",
    val codigoViagem: String = "",
    // Tabela de tarifas da viagem selecionada (ADR-0013), chave (acomodação) → valor da inteira. Alimenta
    // o preview de tarifa/valor no form; a célula é escolhida pela acomodação do passageiro.
    val tarifasViagem: Map<String, Double> = emptyMap(),

    val horaViagem: String = "",
    val onHoraViagemChange: (String) -> Unit = {},
    val isHoraViagemError: Boolean = false,

    val dataViagem: String = LocalDate.now().formatarDataBarrasBr(),
    val onDataViagemChange: (String) -> Unit = {},
    val isDataViagemError: Boolean = false,
    val textDataViagemError: Int = 0,

    val listaAgencia: List<String> = emptyList(),
    val agencia: String = "",
    val onAgenciaChange: (String) -> Unit = {},
    val isAgenciaError: Boolean = false,

    val listaAgente: List<Agente> = emptyList(),
    val agente: String = "",
    val onAgenteChange: (String) -> Unit = {},
    val isAgenteError: Boolean = false,
    val isAgenteDisabled: Boolean = true,

    val listaFormaPagamento: List<Constante> = emptyList(),
    val isPixChecked: Boolean = false,
    val onCheckPix: (Boolean) -> Unit = {},
    val isDinheiroChecked: Boolean = false,
    val onCheckDinheiro: (Boolean) -> Unit = {},
    val isDebitoChecked: Boolean = false,
    val onCheckDebito: (Boolean) -> Unit = {},
    val isCreditoChecked: Boolean = false,
    val onCheckCredito: (Boolean) -> Unit = {},
    val isFormaPagamentoError: Boolean = false,

    val valorPago: String = "",
    val onValorPagoChange: (String) -> Unit = {},
    val isValorPagoError: Boolean = false,
    val isValorPagoEnabled: Boolean = true,

    val valorPix: String = "",
    val onValorPixChange: (String) -> Unit = {},
    val isValorPixError: Boolean = false,

    val valorDinheiro: String = "",
    val onValorDinheiroChange: (String) -> Unit = {},
    val isValorDinheiroError: Boolean = false,

    val valorDebito: String = "",
    val onValorDebitoChange: (String) -> Unit = {},
    val isValorDebitoError: Boolean = false,

    val valorCredito: String = "",
    val onValorCreditoChange: (String) -> Unit = {},
    val isValorCreditoError: Boolean = false,

    val desconto: String = "",
    val onDescontoChange: (String) -> Unit = {},
    val isDescontoError: Boolean = false,
    val isDescontoEnabled: Boolean = true,

    val observacao: String = "",
    val onObservacaoChange: (String, Boolean) -> Unit = { _, _ -> },

    val listaSituacaoPassagem: List<Constante> = emptyList(),
    val listaCategoriaPassagem: List<Constante> = emptyList(),

    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    val isEditing: Boolean = false
) {
    @Ignore
    val isFormaPagamentoEnabled = true
}
