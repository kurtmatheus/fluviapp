package dev.matheus.fluviapp.ui.states

/**
 * Estado da busca de viagens. O filtro é a **rota** — digitando parte do nome de um porto, aparecem as
 * saídas que a percorrem. É a pergunta que se faz de verdade ("o que sai daqui?").
 *
 * [semConcessao] separa dois estados vazios que pareceriam iguais e não são: *"a empresa não recebeu
 * nada"* e *"não há viagem cadastrada no que ela recebeu"*. O primeiro se resolve com a plataforma, o
 * segundo com o botão de criar — e mostrar a mesma tela para os dois mandaria a pessoa para o lugar
 * errado.
 */
data class PesquisaViagemUiState(
    val filtro: String = "",
    val resultados: List<ViagemResultado> = emptyList(),
    /** Inativar é ato de plataforma (ADR-0022 D3): a viagem é o que a passagem aponta. */
    val podeInativar: Boolean = false,
    val semConcessao: Boolean = false,
)

/**
 * Projeção de uma viagem para a lista (ADR-0019 — DTO por caso de uso), com os ids já resolvidos em
 * nomes e a hora já formatada.
 *
 * A lista mostra **as ativas e as inativas**, e as segundas marcadas — o descartado é registro.
 *
 * [chegada] carrega o dia junto quando a travessia atravessa a noite ("Qui 00:00"), porque é aí que a
 * hora sozinha engana: "chega às 00:00" sem o dia parece a mesma madrugada da saída.
 */
data class ViagemResultado(
    val id: String,
    /** "Porto de Val-de-Cães · Belém/PA → Porto de Parintins · Parintins/AM". */
    val rota: String,
    val embarcacao: String,
    /** "Terça-feira · 18:00". */
    val partida: String,
    /** "Qui 00:00" — dia junto, porque a travessia longa chega noutro. */
    val chegada: String,
    val ativa: Boolean,
)