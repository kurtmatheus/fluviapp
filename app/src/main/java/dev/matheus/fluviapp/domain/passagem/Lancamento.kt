package dev.matheus.fluviapp.domain.passagem

import java.math.BigDecimal

/**
 * Um **lançamento de pagamento** — o fato de que um valor entrou por uma forma ([ADR-0018] D11).
 *
 * Substitui as quatro colunas fixas (`valorPix`/`valorDinheiro`/`valorDebito`/`valorCredito`), e a razão é a
 * mesma que o analista aplicou ao pool de clientes: **somar por forma no momento da escrita descarta informação
 * de forma irreversível**. O lançamento é o fato; a soma é derivável dele, e o inverso nunca.
 *
 * ### O que ele deliberadamente NÃO tem
 *
 * Nem NSU, nem txid, nem taxa, nem recebedor — *"seria over-engineering"* (decisão de 2026-08-01). Esses campos
 * voltam a fazer sentido no **módulo de faturamento** (ADR-0018 D12), que é onde conciliação e estorno moram. E
 * duas lacunas se resolvem fora daqui, melhor: *quando se pagou* é o `criadoEm` da passagem — porque **a emissão
 * é pós-pagamento** —, e *quem recebeu* é o emissor, que já está nos metadados.
 *
 * ### O `id`, que parece supérfluo e não é
 *
 * Ele existe para que promover a lista a uma coleção seja **mover, não redesenhar**, se o dia chegar. É gerado
 * no cliente e **opaco** — nunca o índice na lista, que muda se a lista for reescrita e faria a identidade
 * mentir justamente quando ela precisaria valer.
 *
 * Dinheiro em [BigDecimal] no domínio (ADR-0013 §6); `Double` só na fronteira ([ADR-0024] D4).
 */
data class Lancamento(
    val id: String,
    val forma: FormaPagamento,
    val valor: BigDecimal,
)

/**
 * O **total praticado** — a soma dos lançamentos.
 *
 * É calculado, e **não** existe campo gravado ao lado (ADR-0024 D4: *o total é inferido*). Coerente com *preço é
 * I/O*: o valor entra pelo operador e o sistema não recalcula nada a partir de tarifa.
 */
val List<Lancamento>.total: BigDecimal
    get() = fold(BigDecimal.ZERO) { soma, lancamento -> soma + lancamento.valor }

/**
 * Quanto entrou **por forma**, para quem precisa olhar o caixa por dentro.
 *
 * Vive aqui como função pura porque é assim que a análise vai usá-la: sobre uma lista já lida (por período, por
 * ocorrência), agregando em memória — não como consulta por documento, que o Firestore não faz sobre item de
 * array e ninguém pediu.
 */
fun List<Lancamento>.totalPorForma(): Map<FormaPagamento, BigDecimal> =
    groupBy { it.forma }.mapValues { (_, lancamentos) -> lancamentos.total }