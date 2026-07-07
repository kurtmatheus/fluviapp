package dev.matheus.fluviapp.ui.states

import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.model.cadastro.constantes.Constante
import dev.matheus.fluviapp.model.cadastro.passagem.Agente

data class AgenteUiState(
    val listaAgencia: List<String> = emptyList(),
    val agencia: String = "",
    val onAgenciaChange: (String) -> Unit = {},
    val isAgenciaError: Boolean = false,

    val agente: String = "",
    val onAgenteChange: (String) -> Unit = {},
    val isAgenteError: Boolean = false,

    val listaMunicipios: List<String> = emptyList(),
    val lotacao: String = "",
    val onLotacaoChange: (String) -> Unit = {},
    val isLotacaoError: Boolean = false,

    val resultadosListaAgente: List<Agente> = emptyList(),

    val titleJanela: Int = R.string.subtitle_cadastrar_novo_agente,
    val isProcessing: Boolean = false
)
