package dev.matheus.fluviapp.extensions

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * O que sobrou do `FluviAppUnitTest` — o arquivo de exemplo do Android Studio, que virou o último
 * consumidor da cadeia antiga de formatação de documento.
 *
 * Ele tinha oito casos; seis testavam funções que morreram em 2026-09-07 (`formatarCampoCPF`,
 * `formatarCNPJ`, `preencherCampo` e o `extrairDocumentoFormatado` sobre `Constante.Descricao`), e o que
 * elas faziam hoje é do [dev.matheus.fluviapp.domain.documento.TipoDocumento], coberto por
 * `TipoDocumentoTest` com vinte e seis casos em vez de três — inclusive o CNPJ com filial, que a versão
 * antiga errava por fixar `0001`.
 *
 * **Ficam os dois que testam código vivo.** Apagar um teste junto com o sujeito dele é limpeza; apagar
 * junto o teste do vizinho é perda.
 */
class ExtensoesDeFormatacaoTest {

    /** Usado pelo seletor de data: o `DatePicker` do Material devolve millis, e a tela mostra dd/MM/yyyy. */
    @Test
    fun `millis do date picker viram data brasileira`() {
        assertEquals("19/01/2024", 1705622400000.convertMillisToLocalDateToString())
    }

    /** É o que separa o documento digitado (com máscara) do documento guardado (só dígitos). */
    @Test
    fun `extrair numeros descarta os separadores`() {
        assertEquals("38394453000112", "38.394.453/0001-12".extrairNumeros())
    }
}
