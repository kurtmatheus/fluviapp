package dev.matheus.fluviapp.ui.states

/**
 * Estado da busca de navios (molde: busca separada do formulário). Único filtro é a **empresa**
 * (dropdown). O vínculo Navio→Empresa é por id (ADR-0008), então o nome da empresa é resolvido na
 * leitura (na VM) e materializado em [NavioResultado] — a tela não conhece `empresaId`.
 */
data class PesquisaNavioUiState(
    val empresa: String = "",
    val listaEmpresas: List<String> = emptyList(),
    val resultados: List<NavioResultado> = emptyList(),
)

/** Projeção de um navio para a lista de resultados, já com o nome da empresa resolvido. */
data class NavioResultado(
    val id: String,
    val nome: String,
    val empresaNome: String,
)
