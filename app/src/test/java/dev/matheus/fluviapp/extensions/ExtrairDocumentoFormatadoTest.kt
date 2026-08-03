package dev.matheus.fluviapp.extensions

import dev.matheus.fluviapp.revitalizacao.ForaDoEscopo
import org.junit.experimental.categories.Category
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Descricao.CNPJ
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Descricao.CPF
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Descricao.PASSAPORTE
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Descricao.RG
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Trava a robustez de [extrairDocumentoFormatado]: os formatadores fatiam por índice fixo e estouram
 * StringIndexOutOfBounds em documento vazio/incompleto. Uma passagem com dado ruim NÃO pode derrubar
 * a listagem/detalhe (era crash fatal em PesquisarPassagem). Dado insuficiente → valor cru, sem exceção.
 */
@Category(ForaDoEscopo::class)
class ExtrairDocumentoFormatadoTest {

    @Test
    fun `documento vazio retorna vazio sem estourar`() {
        assertEquals("", "".extrairDocumentoFormatado(comMascara = true, tipoDocumento = CPF.name))
        assertEquals("", "".extrairDocumentoFormatado(comMascara = true, tipoDocumento = CNPJ.name))
        assertEquals("", "".extrairDocumentoFormatado(comMascara = true, tipoDocumento = PASSAPORTE.name))
    }

    @Test
    fun `documento incompleto retorna cru sem estourar`() {
        // menos que o tamanho exigido pelo formatador -> devolve o proprio valor, sem slice.
        assertEquals("123", "123".extrairDocumentoFormatado(comMascara = true, tipoDocumento = CPF.name))
        assertEquals("123456", "123456".extrairDocumentoFormatado(tipoDocumento = CNPJ.name))
        assertEquals("AB", "AB".extrairDocumentoFormatado(comMascara = true, tipoDocumento = PASSAPORTE.name))
        assertEquals("12", "12".extrairDocumentoFormatado(comMascara = true, tipoDocumento = RG.name))
    }

    @Test
    fun `CPF completo continua sendo formatado`() {
        // regressao: dado valido (11 digitos) mantem a formatacao com mascara.
        // A politica mudou no ADR-0020 F1: esconde os 6 primeiros digitos e mostra os 5 ultimos.
        val formatado = "12345678901".extrairDocumentoFormatado(comMascara = true, tipoDocumento = CPF.name)
        assertEquals("###.###.789-01", formatado)
    }

    @Test
    fun `CNPJ completo continua sendo formatado`() {
        val formatado = "12345678000199".extrairDocumentoFormatado(tipoDocumento = CNPJ.name)
        assertEquals("12.345.678/0001-99", formatado)
    }
}
