package dev.matheus.fluviapp.ui.viewmodel.helpers.viagem

import dev.matheus.fluviapp.ui.states.PesquisarViagemUiState

/**
 * Validação da pesquisa de viagem — pura ((state) -> resultado, sem mutar estado). Um filtro
 * marcado exige seu valor: empresa/navio preenchidos; trecho com ao menos origem ou destino.
 */
data class ErrosPesquisaViagem(
    val empresa: Boolean = false,
    val navio: Boolean = false,
    val trecho: Boolean = false,
) {
    val valido: Boolean get() = !empresa && !navio && !trecho
}

fun validarPesquisaViagem(state: PesquisarViagemUiState): ErrosPesquisaViagem = ErrosPesquisaViagem(
    empresa = state.isCheckedEmpresa && state.empresa.isBlank(),
    navio = state.isCheckedNavio && state.navio.isBlank(),
    trecho = state.isCheckedTrecho && state.origem.isBlank() && state.destino.isBlank(),
)
