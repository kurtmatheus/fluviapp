package br.com.gruponaveg.extensions

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

fun BigDecimal.formataParaMoedaBrasileira(): String {
    val formatador: NumberFormat = NumberFormat
        .getCurrencyInstance(Locale("pt", "br"))
    return formatador.format(this)
}

fun BigDecimal.getValorFormatadoOrEmpty(): String {
    return if (this > BigDecimal.ZERO) this.formataParaMoedaBrasileira() else ""
}

fun String.toBigDecimal() = BigDecimal(ifEmpty { "0" })

fun Double?.converterParaBigDecimal(): BigDecimal {
    return this?.let {
        toBigDecimal()
    } ?: BigDecimal(0)
}
