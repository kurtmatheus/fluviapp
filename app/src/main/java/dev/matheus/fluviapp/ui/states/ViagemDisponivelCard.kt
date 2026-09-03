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
    /**
     * A saída é **de hoje** — o único fato do card que não se lê no texto dele.
     *
     * Entra como booleano porque os outros campos já chegam formatados: a tela não tem `LocalDate` para
     * comparar, e dar-lhe um só para essa conta faria a apresentação decidir o que é *hoje* — pergunta
     * que depende do relógio, e o relógio tem porta (`Relogio`) justamente para não ser lido em qualquer
     * lugar.
     */
    val ehHoje: Boolean = false,
)