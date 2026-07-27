package dev.matheus.fluviapp.ui.states

import dev.matheus.fluviapp.R

/**
 * Estado do formulário de funcionário — puro (só dados + flags), sem lambdas. Eventos são métodos no
 * FormFuncionarioViewModel (molde cadastro-modulos §7.2). Só campos de formulário — a busca tem estado
 * próprio (PesquisaFuncionarioUiState).
 */
data class FormFuncionarioUiState(
    val titulo: Int = R.string.subtitle_cadastrar_novo_agente,
    val agencia: String = "",
    val isAgenciaError: Boolean = false,
    val funcionario: String = "",
    val isFuncionarioError: Boolean = false,
    val lotacao: String = "",
    val isLotacaoError: Boolean = false,
    val listaAgencia: List<String> = emptyList(),
    val listaMunicipios: List<String> = emptyList(),
    val isProcessing: Boolean = false,
)
