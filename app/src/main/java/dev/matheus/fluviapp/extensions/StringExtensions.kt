package dev.matheus.fluviapp.extensions

import dev.matheus.fluviapp.domain.documento.TipoDocumento

/**
 * O arquivo tinha onze funções de formatação de documento e ficou com **duas**.
 *
 * As nove que saíram em 2026-09-07 (`formatarCampoCPF`, `formatarCampoPassaporte`, `formatarCPF`,
 * `mascararCPF`, `formatarCNPJ`, `formatarPassaporte`, `mascararPassaporte`, `mascararRG`, `mascararCNH`,
 * `extrairLetrasOuNumeros`) eram a **cadeia antiga**: fatiavam por índice fixo, cada uma com o seu guarda
 * de tamanho, e a política de ocultação estava repartida entre elas. O [TipoDocumento] assumiu tudo isso
 * no ADR-0020 F2 — formatar, mascarar, validar e normalizar viraram comportamento do tipo, e o `when`
 * exaustivo cobra o caso novo em vez de cair num `else` silencioso.
 *
 * O último chamador delas era o `FluviAppUnitTest`, o arquivo de exemplo do Android Studio.
 */

/** Máscara progressiva do CNPJ, a única que ainda tem uso próprio (`CnpjVisualTransformation`). */
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

fun String.extrairNumeros(): String {
    return filter { it.isDigit() }
}

// `isTextoNaoNulo()` saiu na F9.2, com os dois chamadores que tinha. Ela existia para um defeito, não para
// uma necessidade: a rota de navegação passava o texto **"null"** como argumento ausente, e o repositório
// da passagem usava a mesma função para decidir entre criar e atualizar. Os dois sumiram — a porta nova não
// tem "salvar por cima" —, e o argumento opcional de rota é decisão do ADR-0026 D6, na F9.5.

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
