package dev.matheus.fluviapp.ui.states.passagem

import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.extensions.formatarDataBarrasBr
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante
import java.time.LocalDate

data class FormPassagemUiState(
    val titleForm: Int = R.string.subtitle_nova_passagem,

    val isVeiculoChecked: Boolean = false,

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
    val isHoraViagemError: Boolean = false,

    val dataViagem: String = LocalDate.now().formatarDataBarrasBr(),
    val isDataViagemError: Boolean = false,
    val textDataViagemError: Int = 0,



    val listaFormaPagamento: List<Constante> = emptyList(),
    val isPixChecked: Boolean = false,
    val isDinheiroChecked: Boolean = false,
    val isDebitoChecked: Boolean = false,
    val isCreditoChecked: Boolean = false,
    val isFormaPagamentoError: Boolean = false,

    val valorPix: String = "",
    val isValorPixError: Boolean = false,

    val valorDinheiro: String = "",
    val isValorDinheiroError: Boolean = false,

    val valorDebito: String = "",
    val isValorDebitoError: Boolean = false,

    val valorCredito: String = "",
    val isValorCreditoError: Boolean = false,

    val observacao: String = "",

    // Bloqueio de emissão fail-closed (ADR-0013): res id da causa (0 = sem bloqueio) + arg opcional
    // (categoria da gratuidade). Vira banner persistente na tela; substitui o toast transiente.
    val emissaoBloqueadaMsg: Int = 0,
    val emissaoBloqueadaArg: String = "",

    val listaSituacaoPassagem: List<Constante> = emptyList(),
    val listaCategoriaPassagem: List<Constante> = emptyList(),

    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    val isEditing: Boolean = false
)
