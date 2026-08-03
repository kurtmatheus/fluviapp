package dev.matheus.fluviapp.ui.states.passagem

import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.extensions.formatarDataBarrasBr
import dev.matheus.fluviapp.domain.documento.TipoDocumento
import dev.matheus.fluviapp.domain.passagem.FormaPagamento
import dev.matheus.fluviapp.domain.passagem.StatusPassagem
import java.time.LocalDate

data class FormPassagemUiState(
    val titleForm: Int = R.string.subtitle_nova_passagem,

    val isVeiculoChecked: Boolean = false,

    // As listas de vocabulário deixaram de ser carregadas (ADR-0020 F2): são os tipos do domínio, não
    // linhas de uma coleção. Sem I/O, sem espelho e sem o estado "lista vazia porque não sincronizou".
    val listaTipoDocumento: List<String> = TipoDocumento.entries.map { it.name },

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



    val listaFormaPagamento: List<String> = FormaPagamento.entries.map { it.name },
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

    val listaSituacaoPassagem: List<String> = StatusPassagem.entries.map { it.name },
    // `listaCategoriaPassagem` saiu: era a categoria VEICULO/PASSAGEIRO, o modo embrionário
    // (ADR-0018 §11.3), e não tinha um único consumidor de UI.

    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    val isEditing: Boolean = false
)
