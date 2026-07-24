package dev.matheus.fluviapp.model.passagem

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Regras puras de tarifa/desconto da passagem (ADR-0013 §5). Substituem a conta circular do antigo
 * `getValorTotal` (total reconstruído somando pagamentos) e o acúmulo ANTAC de `CalculoDesconto` — aqui
 * a **tarifa tabelada é a referência** e o desconto é o **resíduo** medido contra ela.
 *
 * A tarifa devida por categoria mora em [TipoPassagem.tarifaDevida].
 */

private const val ESCALA_MOEDA = 2
private val ARREDONDAMENTO = RoundingMode.UP

/**
 * Desconto concedido = só o que se abriu **abaixo** da tarifa devida (redução discricionária). Meia e
 * gratuidade já estão na `tarifaDevida` (reduções mandatórias) e não contam como desconto. Piso em zero:
 * cobrar acima da devida não vira desconto negativo. Resultado em scale 2 / round up.
 */
fun descontoDerivado(tarifaDevida: BigDecimal, valorCobrado: BigDecimal): BigDecimal {
    val residuo = tarifaDevida.subtract(valorCobrado)
    return residuo.max(BigDecimal.ZERO).setScale(ESCALA_MOEDA, ARREDONDAMENTO)
}

/**
 * Tarifa da inteira da **moto** (ADR-0013, regra provisória): **piso à centena da cilindrada, 1:1 em
 * reais** — `floor(cc/100)*100`. 125cc→100, 250cc→200, 300cc→300. Abaixo de 100cc resulta em 0 (consequência
 * do piso). Substituível por tabela cadastrada por viagem no futuro (ver ADR *Alternativas futuras*).
 */
fun tarifaMotoBase(cilindradaCc: Int): BigDecimal =
    BigDecimal((cilindradaCc / 100) * 100).setScale(ESCALA_MOEDA, ARREDONDAMENTO)