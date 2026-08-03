package dev.matheus.fluviapp.ui.states

import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante
import dev.matheus.fluviapp.domain.screendata.DadosViagemCard
import dev.matheus.fluviapp.domain.viagem.Empresa
import dev.matheus.fluviapp.domain.viagem.Navio

/**
 * Estado da pesquisa de viagem — puro (só dados/flags), sem lambdas. Eventos são métodos no
 * PesquisarViagemViewModel (molde ADR-0006). A state é compartilhada pelas 3 telas do grafo
 * (filtro → resultados → detalhes) via VM escopado ao grafo.
 */
data class PesquisarViagemUiState(
    val listaEmpresas: List<Empresa> = emptyList(),
    val isCheckedEmpresa: Boolean = false,
    val empresa: String = "",
    val isEmpresaError: Boolean = false,

    val listaNavios: List<Navio> = emptyList(),
    val isCheckedNavio: Boolean = false,
    val navio: String = "",
    val isNavioError: Boolean = false,

    val listaMunicipios: List<Constante> = emptyList(),
    val isCheckedTrecho: Boolean = false,
    val isTrechoError: Boolean = false,
    val textTrechoError: Int = R.string.error_selecione_opcao,
    val origem: String = "",
    val destino: String = "",

    val listaResultadoViagens: List<DadosViagemCard> = emptyList(),
    val dadosViagemCard: DadosViagemCard = DadosViagemCard(),
    val isShowDeleteDialog: Boolean = false,
) {
    val filtrarPorOrigem: Boolean get() = isCheckedTrecho && origem.isNotBlank() && destino.isBlank()
    val filtrarPorDestino: Boolean get() = isCheckedTrecho && origem.isBlank() && destino.isNotBlank()
    val filtrarPorOrigemDestino: Boolean get() = isCheckedTrecho && origem.isNotBlank() && destino.isNotBlank()
}
