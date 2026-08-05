package dev.matheus.fluviapp.ui.states

/**
 * Estado da busca de localidades. O filtro é o **texto do município** (a UF vem junto no rótulo), e quem
 * filtra é o ViewModel — a tela só avisa o que foi digitado.
 */
data class PesquisaLocalidadeUiState(
    val municipio: String = "",
    val resultados: List<LocalidadeResultado> = emptyList(),
)

/**
 * Projeção de uma localidade para a lista, já formatada (ADR-0019 — DTO por caso de uso, e este caso é
 * exibir). Só chegam aqui as **ativas**: a inativa continua existindo para quem a resolve por id, mas não
 * volta a ser escolhida.
 */
data class LocalidadeResultado(
    val id: String,
    val rotulo: String,
    val codigoIbge: String,
)