package dev.matheus.fluviapp.ui.states.passagem

/**
 * **A projeção do bilhete para quem está na doca** ([ADR-0025] D4) — o primeiro DTO por consumidor da
 * Passagem.
 *
 * O que ela substitui é o `DadosPassagem`: ~58 campos servindo **quatro** consumidores ao mesmo tempo, e por
 * isso do tamanho da união deles — com sintomas no próprio arquivo, como `idPassageiro1 = ""` preenchido com
 * vazio porque *algum* consumidor talvez o quisesse. **O corte é por consumidor**, e um campo só existe aqui
 * se a pergunta *"posso deixar esta pessoa embarcar?"* o exigir.
 *
 * São cinco. O [numero] e a [identificacao] respondem *que bilhete é este e de quem*; a [travessia] e a
 * [partida] respondem *se é esta viagem mesmo* — o erro honesto mais comum na doca é a pessoa chegar no dia
 * certo e na saída errada; o [status] responde *se ele ainda vale*.
 *
 * Texto já formatado porque **formatar é da apresentação**, e este DTO é de apresentação — o mesmo desenho do
 * `ViagemDisponivelCard` da F8. O que o [ADR-0024] D8 tirou de circulação foi o texto formatado na **camada
 * de dados**, onde ele impedia somar dinheiro e obrigava o teste a comparar aparência.
 */
data class ConferenciaDeEmbarque(
    /** "#12" — a identidade **exibida**, por ocorrência. */
    val numero: String,
    /**
     * Quem embarca: o nome do titular, ou a placa quando o sujeito é o veículo.
     *
     * **Pode vir vazio, e isso não é falha de carregamento** — ver [IdentificacaoIndisponivel] no mapper.
     */
    val identificacao: String,
    /** "Porto de Val-de-Cães · Belém/PA → Porto de Parintins · Parintins/AM". */
    val travessia: String,
    /** "Terça-feira, 18/08 · 18:00". */
    val partida: String,
    /** "EMITIDA", "EMBARCADA", "CANCELADA" — o rótulo da FSM. */
    val status: String,
)