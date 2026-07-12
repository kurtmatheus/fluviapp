package dev.matheus.fluviapp.ui.states

import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.model.cadastro.constantes.Constante
import dev.matheus.fluviapp.model.viagem.Empresa
import dev.matheus.fluviapp.model.viagem.Navio

/**
 * Estado do formulário de viagem — puro (só dados + flags). Sem lambdas embutidas: os eventos são
 * métodos no FormViagemViewModel (molde cadastro-modulos §7.2).
 */
data class FormViagemUiState(
    val titulo: Int = R.string.subtitle_cadastrar_nova_viagem,

    val empresa: String = "",
    val isEmpresaError: Boolean = false,

    val navio: String = "",
    val isNavioError: Boolean = false,
    val navioDesabilitado: Boolean = true,

    val trechoOrigem: String = "",
    val isTrechoOrigemError: Boolean = false,

    val trechoDestino: String = "",
    val isTrechoDestinoError: Boolean = false,
    val trechoDestinoDesabilitado: Boolean = true,

    val listaEmpresas: List<Empresa> = emptyList(),
    val listaNavios: List<Navio> = emptyList(),
    val listaMunicipios: List<Constante> = emptyList(),

    val isProcessando: Boolean = false,
)
