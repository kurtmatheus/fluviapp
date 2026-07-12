package dev.matheus.fluviapp.ui.viewmodel.helpers.agente

import dev.matheus.fluviapp.ui.states.FormAgenteUiState

/**
 * Validação do formulário de agente — pura e JVM-testável ((state) -> resultado, sem mutar estado).
 * agência, nome (agente) e lotação obrigatórios.
 */
data class ErrosAgente(
    val agencia: Boolean = false,
    val agente: Boolean = false,
    val lotacao: Boolean = false,
) {
    val valido: Boolean get() = !agencia && !agente && !lotacao
}

fun validarAgente(state: FormAgenteUiState): ErrosAgente = ErrosAgente(
    agencia = state.agencia.isBlank(),
    agente = state.agente.isBlank(),
    lotacao = state.lotacao.isBlank(),
)
