package br.com.gruponaveg

import br.com.gruponaveg.extensions.convertMillisToLocalDateToString
import br.com.gruponaveg.extensions.extrairDocumentoFormatado
import br.com.gruponaveg.extensions.extrairNumeros
import br.com.gruponaveg.extensions.formatarCNPJ
import br.com.gruponaveg.extensions.formatarCampoCPF
import br.com.gruponaveg.extensions.preencherCampo
import br.com.gruponaveg.model.cadastro.constantes.Constante
import br.com.gruponaveg.model.cadastro.constantes.Constante.Descricao
import br.com.gruponaveg.model.cadastro.constantes.Constante.Descricao.*
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class NavegAppUnitTest {
    @Test
    fun extension_ConvertLongToDateStringIsWorking() {
        val today = 1705622400000.convertMillisToLocalDateToString()

        assertEquals("19/01/2024", today)
    }

    @Test
    fun extension_FormatarCpfWorks() {
        val cpf = "12345678912".formatarCampoCPF()

        assertEquals("123.456.789-12", cpf)
    }

    @Test
    fun extension_FormatarCnpjWorks() {
        val cpf = "38394453000112".formatarCNPJ()

        assertEquals("38.394.453/0001-12", cpf)
    }

    @Test
    fun extension_ExtrairNumerosWorks() {
        val cpf = "38.394.453/0001-12".extrairNumeros()

        assertEquals("38394453000112", cpf)
    }

    @Test
    fun extension_ExtrairDocumentoFormatadoCpf() {
        val cpf = "12345678912".extrairDocumentoFormatado(tipoDocumento = CPF.name)

        assertEquals("123.456.789-12", cpf)
    }

    @Test
    fun extension_ExtrairDocumentoFormatadoCpfComMascara() {
        val cpf = "12345678912".extrairDocumentoFormatado(comMascara = true, tipoDocumento = CPF.name)

        assertEquals("###.456.789-##", cpf)
    }

    @Test
    fun extension_ExtrairDocumentoFormatadoCnpj() {
        val cpf = "38394453000112".extrairDocumentoFormatado(tipoDocumento = CNPJ.name)

        assertEquals("38.394.453/0001-12", cpf)
    }

    @Test
    fun extension_ExtrairDoubleStringOuVazio() {
        val primeiraVariavel = 50.toDouble()

        assertEquals("50", primeiraVariavel.preencherCampo())
    }
}