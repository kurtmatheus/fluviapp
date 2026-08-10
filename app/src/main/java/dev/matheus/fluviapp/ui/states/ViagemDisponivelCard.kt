package dev.matheus.fluviapp.ui.states

/**
 * Projeção de uma `ViagemSemana` para o Início da empresa (ADR-0019 — DTO por caso de uso).
 *
 * É a herdeira do `DadosViagemCard` que a F8.0 demoliu, e a diferença entre os dois é o que a
 * revitalização entregou: aquele carregava `codigo`, `origem` e `destino` **digitados** na Viagem-trecho,
 * mais as capacidades da embarcação; este carrega a **partida datada** de uma ocorrência real, com os
 * lugares resolvidos por id a partir dos portos da rota.
 *
 * Tudo já formatado, porque formatar é da apresentação — e porque o card não deve saber somar minutos.
 */
data class ViagemDisponivelCard(
    /** `viagemId@data` — identifica a **ocorrência**, não a viagem semanal. */
    val id: String,
    /** O id da viagem, que é o que a emissão vai receber (F9). */
    val viagemId: String,
    /** "Terça-feira, 12/08 · 18:00". */
    val partida: String,
    /** "Porto de Val-de-Cães · Belém/PA → Porto de Parintins · Parintins/AM". */
    val rota: String,
    val embarcacao: String,
    /** "Qui 00:00", ou vazio quando a rota não diz o tempo. */
    val chegada: String,
)