package dev.matheus.fluviapp.domain.passagem

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

// O `tarifaMotoBase` — piso à centena da cilindrada, 1:1 em reais — saiu em 2026-09-03 (ADR-0031).
//
// Ele era a regra provisória do ADR-0013 e **já não tinha chamador em produção** desde que *preço é I/O*
// (2026-08-11): a emissão não calcula valor, o operador informa o praticado. O que o ADR-0031 acrescentou
// foi tirar-lhe também a razão conceitual — com a cilindrada derivando da natureza, some o último vínculo
// entre a classe do veículo e dinheiro.
//
// Se a inferência tarifária vier a precisar de uma régua por cilindrada, ela nasce no módulo de
// faturamento, sobre o agregado de passagens — não aqui.