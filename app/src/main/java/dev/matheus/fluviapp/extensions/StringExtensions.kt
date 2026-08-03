package dev.matheus.fluviapp.extensions

import dev.matheus.fluviapp.domain.documento.TipoDocumento

fun String.formatarCampoCPF(): String {
    val aux = length.dec()
    return when (length) {
        in (1..3) -> slice(0..aux)
        in (4..6) -> "${slice(0..2)}.${slice(3..aux)}"
        in (7..9) -> "${slice(0..2)}.${slice(3..5)}.${slice(6..aux)}"
        in (10..11) -> "${slice(0..2)}.${slice(3..5)}.${slice(6..8)}-${slice(9..aux)}"
        else -> this
    }
}

fun String.formatarCampoCNPJ(): String {
    val aux = length.dec()
    return when (length) {
        in (1..2) -> slice(0..aux)
        in (3..5) -> "${slice(0..1)}.${slice(2..aux)}"
        in (6..8) -> "${slice(0..1)}.${slice(2..4)}.${slice(5..aux)}"
        in (9..12) -> "${slice(0..1)}.${slice(2..4)}.${slice(5..7)}/${slice(8..aux)}"
        in (13..14) -> "${slice(0..1)}.${slice(2..4)}.${slice(5..7)}/${slice(8..11)}-${slice(12..aux)}"
        else -> this
    }
}

fun String.formatarCampoPassaporte(): String {
    val aux = length.dec()
    return when (length) {
        in (1..2) -> slice(0..aux)
        in (3..8) -> "${slice(0..1)}-${slice(2..aux)}"
        else -> this
    }
}

fun String.formatarCPF(comMascara: Boolean): String {
    val cpfFormatado = "${slice(0..2)}.${slice(3..5)}.${slice(6..8)}-${slice(9..10)}"
    return if (comMascara) {
        cpfFormatado.mascararCPF()
    } else {
        cpfFormatado
    }
}

fun String.mascararCPF(): String {
    return "###.${slice(4..6)}.${slice(8..10)}-##"
}

fun String.formatarCNPJ(): String {
    return "${slice(0..1)}.${slice(2..4)}.${slice(5..7)}/0001-${slice(12..13)}"
}

fun String.extrairNumeros(): String {
    return filter { it.isDigit() }
}

fun String.formatarPassaporte(comMascara: Boolean): String {
    val passaporteFormatado = "${slice(0..1)}-${slice(2..7)}"
    return if (comMascara) {
        passaporteFormatado.mascararPassaporte()
    } else passaporteFormatado
}

fun String.extrairLetrasOuNumeros(): String {
    return filter { it.isLetterOrDigit() }
}

fun String.mascararRG(): String {
    return replaceRange(1..3, "###")
}

fun String.mascararCNH(): String {
    return replaceRange(2..7, "######")
}

fun String.mascararPassaporte(): String {
    return replaceRange(4..6, "###")
}

fun String?.isTextoNaoNulo(): Boolean = this != null && this != "null"

/**
 * Exibição do documento, delegando ao [TipoDocumento] (ADR-0020 F2).
 *
 * Era o terceiro `when` sobre `Constante.Descricao` do app, e carregava dois defeitos que a delegação
 * resolve: o `else` devolvia **string vazia**, ou seja, um tipo que o código não conhecesse fazia o
 * documento **sumir do bilhete** sem erro e sem log; e os formatadores fatiavam por índice fixo, o que
 * exigiu os guardas de tamanho espalhados por cada ramo.
 *
 * A política de ocultação também passou a ser uma só — e é lá que ela muda: o CPF agora esconde os **6
 * primeiros** dígitos e mostra os 5 últimos.
 */
fun String.extrairDocumentoFormatado(
    comMascara: Boolean = false,
    tipoDocumento: String?
): String {
    if (isBlank()) return ""
    val tipo = TipoDocumento.de(tipoDocumento) ?: return this
    return tipo.exibir(this, ocultar = comMascara)
}
