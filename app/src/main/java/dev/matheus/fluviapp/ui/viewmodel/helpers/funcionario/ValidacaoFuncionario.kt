package dev.matheus.fluviapp.ui.viewmodel.helpers.funcionario

import dev.matheus.fluviapp.ui.states.FormFuncionarioUiState

/**
 * Validação do formulário de funcionário — pura e JVM-testável ((state) -> resultado, sem mutar estado).
 * agência, nome (funcionário) e lotação obrigatórios.
 */
data class ErrosFuncionario(
    val agencia: Boolean = false,
    val funcionario: Boolean = false,
    val lotacao: Boolean = false,
) {
    val valido: Boolean get() = !agencia && !funcionario && !lotacao
}

fun validarFuncionario(state: FormFuncionarioUiState): ErrosFuncionario = ErrosFuncionario(
    agencia = state.agencia.isBlank(),
    funcionario = state.funcionario.isBlank(),
    lotacao = state.lotacao.isBlank(),
)
