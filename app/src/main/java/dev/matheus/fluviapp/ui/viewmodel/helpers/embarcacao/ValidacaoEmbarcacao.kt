package dev.matheus.fluviapp.ui.viewmodel.helpers.embarcacao

import dev.matheus.fluviapp.ui.states.FormEmbarcacaoUiState

/**
 * Validação do formulário de embarcação — pura e JVM-testável ((state) -> resultado, sem mutar estado).
 *
 * Três obrigatórios: nome, empresa (vínculo N-1) e **tipo**. O tipo está aqui por um motivo diferente dos
 * outros dois: nome e empresa são exigência de cadastro, o tipo é o invariante da entidade — `Embarcacao`
 * não aceita nulo, então sem esta validação o formulário nem teria o que construir.
 *
 * As capacidades são dígitos-only no state (sempre válidas), então não entram. Nem entra "capacidade de
 * veículo em lancha": o formulário não chega a perguntar
 * ([FormEmbarcacaoUiState.perguntaCapacidadeVeiculo]), e o VM zera o valor ao trocar o tipo. Validar aqui
 * seria acusar de um erro que a tela não permitiu cometer.
 */
data class ErrosEmbarcacao(
    val nome: Boolean = false,
    val empresa: Boolean = false,
    val tipo: Boolean = false,
) {
    val valido: Boolean get() = !nome && !empresa && !tipo
}

fun validarEmbarcacao(state: FormEmbarcacaoUiState): ErrosEmbarcacao = ErrosEmbarcacao(
    nome = state.nome.isBlank(),
    empresa = state.empresa.isBlank(),
    tipo = state.tipo == null,
)