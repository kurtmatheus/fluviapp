package dev.matheus.fluviapp.ui.states

import dev.matheus.fluviapp.model.cadastro.passagem.Agente

/**
 * Estado da busca de agentes — separado do formulário (molde: form e busca não compartilham VM/state).
 * `resultados` já vem filtrado pela agência (filtro no VM, não no composable).
 */
data class PesquisaAgenteUiState(
    val agencia: String = "",
    val listaAgencia: List<String> = emptyList(),
    val resultados: List<Agente> = emptyList(),
)
