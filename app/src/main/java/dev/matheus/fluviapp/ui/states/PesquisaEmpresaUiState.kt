package dev.matheus.fluviapp.ui.states

import dev.matheus.fluviapp.domain.viagem.Empresa

/**
 * Estado da busca de empresas (molde: busca separada do formulário). Filtro é o campo `nome`, que
 * filtra a lista já carregada por `startsWith` sem distinção de caixa. `resultados` já vem filtrado.
 */
data class PesquisaEmpresaUiState(
    val nome: String = "",
    val resultados: List<Empresa> = emptyList(),
)
