package dev.matheus.fluviapp.ui.states

/**
 * Estado da busca de portos. O filtro é o **nome do porto**; a localidade acompanha cada linha, mas não
 * filtra — quem procura um porto sabe o nome dele, e quem quer "os portos de Belém" lê a coluna.
 */
data class PesquisaPortoUiState(
    val nome: String = "",
    val resultados: List<PortoResultado> = emptyList(),
)

/**
 * Projeção de um porto para a lista, já formatada (ADR-0019 — DTO por caso de uso, e este caso é
 * exibir). É aqui que a referência vira leitura: o `localidadeId` do documento chega como o
 * [rotuloLocalidade] que a pessoa lê — "Porto de Val-de-Cães" · "Belém/PA" —, e a tela não faz ideia de
 * que houve um id no caminho.
 *
 * Só chegam aqui os portos **ativos**. O rótulo, porém, pode vir de uma localidade inativa: quem resolve
 * por id não filtra (é a regra da `LocalidadeRepository`), senão desativar um município apagaria o lugar
 * dos portos que continuam operando.
 */
data class PortoResultado(
    val id: String,
    val nome: String,
    val rotuloLocalidade: String,
)