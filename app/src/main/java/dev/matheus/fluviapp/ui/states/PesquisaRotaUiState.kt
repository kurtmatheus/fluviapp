package dev.matheus.fluviapp.ui.states

/**
 * Estado da busca de rotas. O filtro é o **porto** — digitando parte do nome, aparecem as rotas que
 * saem dele ou chegam nele. É a pergunta que se faz de verdade ("o que liga daqui?"), e ela não teria
 * resposta num filtro por origem só.
 */
data class PesquisaRotaUiState(
    val porto: String = "",
    val resultados: List<RotaResultado> = emptyList(),
    /** Inativar é ato de plataforma (ADR-0022 D3): tira do pool o que todo mundo enxerga. */
    val podeInativar: Boolean = false,
    /**
     * Sem porto concedido não há ligação a mostrar, e a tela precisa dizer isso — vazio por falta de
     * provisionamento se resolve com a plataforma, vazio por pool novo se resolve com o botão de criar.
     */
    val semConcessao: Boolean = false,
)

/**
 * Projeção de uma rota para a lista (ADR-0019 — DTO por caso de uso), com os ids já resolvidos em nomes.
 *
 * A lista mostra **as ativas e as inativas**, e as segundas marcadas: o descartado é registro, e um
 * pool em que só se vê o que está em uso não responde por que um bilhete antigo aponta para onde
 * aponta.
 */
data class RotaResultado(
    val id: String,
    val origem: String,
    val destino: String,
    /** "420,5 mn · 30 h" — já formatado, porque formatar é da apresentação. */
    val medidas: String,
    val ativa: Boolean,
)