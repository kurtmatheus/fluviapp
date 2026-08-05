package dev.matheus.fluviapp.ui.viewmodel.helpers.embarcacao

import dev.matheus.fluviapp.ui.states.FormEmbarcacaoUiState

/**
 * Validação do formulário de embarcacao — pura e JVM-testável ((state) -> resultado, sem mutar estado).
 * nome e empresa (vínculo N-1) obrigatórios; capacidades são dígitos-only no state (sempre válidas),
 * então não entram na validação.
 */
data class ErrosEmbarcacao(
    val nome: Boolean = false,
    val empresa: Boolean = false,
) {
    val valido: Boolean get() = !nome && !empresa
}

fun validarEmbarcacao(state: FormEmbarcacaoUiState): ErrosEmbarcacao = ErrosEmbarcacao(
    nome = state.nome.isBlank(),
    empresa = state.empresa.isBlank(),
)
