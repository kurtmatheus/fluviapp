package dev.matheus.fluviapp.ui.states

import dev.matheus.fluviapp.model.operacoes.Funcionario

/**
 * Estado da busca de funcionarios — separado do formulário (molde: form e busca não compartilham VM/state).
 * `resultados` já vem filtrado pela agência (filtro no VM, não no composable).
 */
data class PesquisaFuncionarioUiState(
    val agencia: String = "",
    val listaAgencia: List<String> = emptyList(),
    val lotacao: String = "",
    val listaLotacao: List<String> = emptyList(),
    val resultados: List<Funcionario> = emptyList(),
)
