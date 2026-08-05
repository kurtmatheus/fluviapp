package dev.matheus.fluviapp.ui.states

/**
 * Estado da busca de embarcações (molde: busca separada do formulário). Único filtro é a **empresa**
 * (dropdown). O vínculo Embarcacao→Empresa é por id (ADR-0008), então o nome da empresa é resolvido na
 * leitura (na VM) e materializado em [EmbarcacaoResultado] — a tela não conhece `empresaId`.
 */
data class PesquisaEmbarcacaoUiState(
    val empresa: String = "",
    val listaEmpresas: List<String> = emptyList(),
    val resultados: List<EmbarcacaoResultado> = emptyList(),
)

/**
 * Projeção de uma embarcação para a lista de resultados: nome da empresa resolvido do id e tipo já em
 * **rótulo** (ADR-0019 — o DTO é por caso de uso, e este caso é exibir). A tela recebe texto pronto; quem
 * traduziu foi o VM.
 */
data class EmbarcacaoResultado(
    val id: String,
    val nome: String,
    val tipo: String,
    val empresaNome: String,
)
