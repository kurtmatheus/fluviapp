package dev.matheus.fluviapp.ui.viewmodel.helpers.navio

import dev.matheus.fluviapp.ui.states.FormNavioUiState

/**
 * Validação do formulário de navio — pura e JVM-testável ((state) -> resultado, sem mutar estado).
 * nome e empresa (vínculo N-1) obrigatórios; capacidades são dígitos-only no state (sempre válidas),
 * então não entram na validação.
 */
data class ErrosNavio(
    val nome: Boolean = false,
    val empresa: Boolean = false,
) {
    val valido: Boolean get() = !nome && !empresa
}

fun validarNavio(state: FormNavioUiState): ErrosNavio = ErrosNavio(
    nome = state.nome.isBlank(),
    empresa = state.empresa.isBlank(),
)
