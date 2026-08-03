package dev.matheus.fluviapp.domain.passagem

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Tipo tarifário da passagem como **tipo de domínio** (ADR-0013), no lugar da String solta pendurada no
 * catálogo genérico `Constante.Descricao` — mesmo movimento que o [StatusPassagem] fez no ADR-0012.
 *
 * Cada tipo sabe derivar sua **tarifa devida** a partir da `tarifaBase` (a tarifa da inteira da célula
 * tabelada na Viagem): inteira = base, **meia = metade** (metade da tabelada; só abaixo disso é desconto),
 * gratuidade = **zero**. Dinheiro sempre em `BigDecimal` scale 2 / `RoundingMode.UP` (ADR-0013 §6).
 *
 * `de()` converte na fronteira (tolerante à grafia legada), `name` é o valor canônico gravado, `rotulo()`
 * formata para exibição.
 */
enum class TipoPassagem {
    INTEIRA,
    MEIA,
    GRATUIDADE;

    /**
     * Tarifa devida desta categoria a partir da tarifa tabelada da inteira (`tarifaBase`), em scale 2 /
     * round up. Não depende do subtipo de gratuidade — qualquer gratuidade zera a tarifa.
     */
    fun tarifaDevida(tarifaBase: BigDecimal): BigDecimal = when (this) {
        INTEIRA -> tarifaBase.setScale(ESCALA_MOEDA, ARREDONDAMENTO)
        MEIA -> tarifaBase.divide(DOIS, ESCALA_MOEDA, ARREDONDAMENTO)
        GRATUIDADE -> BigDecimal.ZERO.setScale(ESCALA_MOEDA)
    }

    /** Valor de exibição/impressão. */
    fun rotulo(): String = when (this) {
        INTEIRA -> "Inteira"
        MEIA -> "Meia"
        GRATUIDADE -> "Gratuidade"
    }

    companion object {
        private const val ESCALA_MOEDA = 2
        private val ARREDONDAMENTO = RoundingMode.UP
        private val DOIS = BigDecimal(2)

        /**
         * Converte o tipo persistido (String) no enum canônico; `null` se desconhecido (fail-closed).
         * Tolerante à grafia legada: normaliza espaços→underscore e caixa.
         */
        fun de(valor: String?): TipoPassagem? {
            val normalizado = valor?.trim()?.uppercase()?.replace(" ", "_") ?: return null
            return entries.firstOrNull { it.name == normalizado }
        }
    }
}