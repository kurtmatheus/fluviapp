package dev.matheus.fluviapp.ui.viewmodel.helpers.viagem

import dev.matheus.fluviapp.ui.states.FormViagemUiState

/**
 * Validação do formulário de viagem — pura e JVM-testável ((state) -> resultado, sem mutar estado).
 * Todos os campos são obrigatórios (empresa passa a ser validada, o que não ocorria antes).
 */
data class ErrosViagem(
    val empresa: Boolean = false,
    val navio: Boolean = false,
    val trechoOrigem: Boolean = false,
    val trechoDestino: Boolean = false,
) {
    val valido: Boolean get() = !empresa && !navio && !trechoOrigem && !trechoDestino
}

fun validarViagem(state: FormViagemUiState): ErrosViagem = ErrosViagem(
    empresa = state.empresa.isBlank(),
    navio = state.navio.isBlank(),
    trechoOrigem = state.trechoOrigem.isBlank(),
    trechoDestino = state.trechoDestino.isBlank(),
)
