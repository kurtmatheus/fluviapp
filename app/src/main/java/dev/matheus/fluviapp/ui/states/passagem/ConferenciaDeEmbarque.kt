package dev.matheus.fluviapp.ui.states.passagem

/**
 * **A projeção do bilhete para quem está na doca** ([ADR-0025] D4) — o primeiro DTO por consumidor da
 * Passagem.
 *
 * O que ela substitui é o `DadosPassagem`: ~58 campos servindo **quatro** consumidores ao mesmo tempo, e por
 * isso do tamanho da união deles — com sintomas no próprio arquivo, como `idPassageiro1 = ""` preenchido com
 * vazio porque *algum* consumidor talvez o quisesse. **O corte é por consumidor**, e um campo só existe aqui
 * se a pergunta que este consumidor faz o exigir.
 *
 * ### E a pergunta é sobre o bilhete, não sobre a pessoa
 *
 * *"O embarque confere bilhete e não pessoa"* — decisão do analista (2026-08-13), tomada quando a
 * implementação mostrou o conflito: o pool é PII com leitura recortada pela assinatura, mas **validar
 * embarque é um eixo aberto a qualquer papel conhecido** (quem está na doca valida, mesmo sem ter vendido).
 * Ou seja, ou a conferência por nome atravessaria agências — afrouxando a proteção do dado pessoal — ou ela
 * seria inconsistente, funcionando só para bilhete da casa.
 *
 * A decisão resolve os dois de uma vez, e **de graça**: nome e placa saem da conferência, e com eles sai a
 * leitura dos pools neste fluxo. **O embarque deixa de tocar em dado pessoal** — o que é o desenho certo
 * também pela LGPD: não se lê PII onde ela não decide nada.
 *
 * Sobram quatro campos, e cada um responde uma pergunta da doca: [numero] e [bilhete] dizem *que bilhete é
 * este e o que ele comprou*; [travessia] e [partida] dizem *se é esta saída mesmo* — o erro honesto mais
 * comum é chegar no dia certo e na viagem errada; [status] diz *se ele ainda vale*.
 *
 * Texto já formatado porque **formatar é da apresentação**, e este DTO é de apresentação — o mesmo desenho do
 * `ViagemDisponivelCard` da F8. O que o [ADR-0024] D8 tirou de circulação foi o texto formatado na **camada
 * de dados**, onde ele impedia somar dinheiro e obrigava o teste a comparar aparência.
 */
data class ConferenciaDeEmbarque(
    /** "#12" — a identidade **exibida**, por ocorrência. */
    val numero: String,
    /** O que foi vendido: "Suíte", "Rede · Meia", "Veículo". Não *quem* embarca — ver o KDoc acima. */
    val bilhete: String,
    /** "Porto de Val-de-Cães · Belém/PA → Porto de Parintins · Parintins/AM". */
    val travessia: String,
    /** "Terça-feira, 18/08 · 18:00". */
    val partida: String,
    /** "EMITIDA", "EMBARCADA", "CANCELADA" — o rótulo da FSM. */
    val status: String,
)