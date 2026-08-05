package dev.matheus.fluviapp.ui.states

/**
 * Estado da busca de embarcacoes (molde: busca separada do formulário). Único filtro é a **empresa**
 * (dropdown). O vínculo Embarcacao→Empresa é por id (ADR-0008), então o nome da empresa é resolvido na
 * leitura (na VM) e materializado em [EmbarcacaoResultado] — a tela não conhece `empresaId`.
 */
data class PesquisaEmbarcacaoUiState(
    val empresa: String = "",
    val listaEmpresas: List<String> = emptyList(),
    val resultados: List<EmbarcacaoResultado> = emptyList(),
)

/** Projeção de um embarcacao para a lista de resultados, já com o nome da empresa resolvido. */
data class EmbarcacaoResultado(
    val id: String,
    val nome: String,
    val empresaNome: String,
)
