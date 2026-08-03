package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem

import dev.matheus.fluviapp.domain.documento.TipoDocumento
import dev.matheus.fluviapp.extensions.extrairNumeros

/**
 * Limita a digitação do documento ao que o tipo comporta e o guarda na forma **canônica** (ADR-0020 F2).
 *
 * Era o **quarto** `when` sobre `Constante.Descricao` do app — depois de máscara, teclado e ocultação —,
 * e existia **duplicado, idêntico**, em `FormPassageiroHelper` e `FormVeiculoHelper`, apoiado em três
 * constantes soltas (`TAMANHO_CPF`, `TAMANHO_CNPJ`, `TAMANHO_PASS`) que eram exatamente o comprimento que
 * o tipo já declara. Genérica no state porque a única diferença entre as duas cópias era ele.
 *
 * Duas correções vêm de graça, e ambas são consequência de o tipo saber o próprio tamanho:
 * - **RG e CNH passam a ter limite.** Caíam no `else` e aceitavam digitação sem fim;
 * - **o passaporte passa a ser normalizado** (letras e dígitos, caixa alta) em vez de guardado cru.
 */
internal fun <S> limitarDocumento(
    documento: String,
    tipoDocumento: String,
    uiState: S,
    onAtualizarDocumento: (S, String) -> S,
): S {
    // Tipo que o código não conhece: preserva o comportamento antigo (só dígitos, sem limite) em vez de
    // travar o campo. Quem recusa o valor é a validação, com mensagem — não o teclado, em silêncio.
    val tipo = TipoDocumento.de(tipoDocumento)
        ?: return onAtualizarDocumento(uiState, documento.extrairNumeros())

    val normalizado = tipo.normalizar(documento)
    return if (normalizado.length <= tipo.comprimento.last) {
        onAtualizarDocumento(uiState, normalizado)
    } else {
        uiState
    }
}