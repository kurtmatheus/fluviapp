package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.validacao

import dev.matheus.fluviapp.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regra pura do par (tipo, número) de documento (ADR-0020 D2). A parte nova é a terceira: até aqui o campo
 * era texto livre e nenhum dos `when` sobre `Constante.Descricao` validava — só decoravam.
 */
class ValidacaoDocumentoTest {

    private val cpfValido = "529.982.247-25"
    private val cnpjValido = "11.222.333/0001-81"

    @Test
    fun `sem tipo escolhido nao se cobra numero — o documento e opcional`() {
        val e = validarDocumento(tipo = "", numero = "")
        assertFalse(e.erro)
        assertEquals(0, e.texto)
    }

    @Test
    fun `sem tipo escolhido, numero preenchido tambem nao acusa`() {
        assertFalse(validarDocumento(tipo = "", numero = "qualquer coisa").erro)
    }

    @Test
    fun `tipo escolhido e numero vazio e obrigatorio`() {
        val e = validarDocumento(tipo = "CPF", numero = "")
        assertTrue(e.erro)
        assertEquals(R.string.error_camp_obrig, e.texto)
    }

    @Test
    fun `cpf valido passa, com ou sem formatacao`() {
        assertFalse(validarDocumento("CPF", cpfValido).erro)
        assertFalse(validarDocumento("CPF", "52998224725").erro)
    }

    @Test
    fun `cpf invalido acusa com a mensagem certa — nao com campo obrigatorio`() {
        val e = validarDocumento("CPF", "000.000.000-00")
        assertTrue(e.erro)
        assertEquals(R.string.error_documento_invalido, e.texto)
    }

    @Test
    fun `cpf com digito verificador errado nao passa`() {
        assertTrue(validarDocumento("CPF", "529.982.247-26").erro)
        assertTrue(validarDocumento("CPF", "111.111.111-11").erro)
    }

    @Test
    fun `cpf incompleto nao passa`() {
        assertTrue(validarDocumento("CPF", "529.982.247").erro)
    }

    @Test
    fun `cnpj valido passa e invalido acusa`() {
        assertFalse(validarDocumento("CNPJ", cnpjValido).erro)
        assertTrue(validarDocumento("CNPJ", "00.000.000/0001-00").erro)
    }

    @Test
    fun `rg cnh e passaporte valem pelo comprimento`() {
        assertFalse(validarDocumento("RG", "12345678").erro)
        assertTrue(validarDocumento("RG", "1234").erro)
        assertFalse(validarDocumento("CNH", "12345678901").erro)
        assertFalse(validarDocumento("PASSAPORTE", "AB123456").erro)
        assertTrue(validarDocumento("PASSAPORTE", "AB12345").erro)
    }

    @Test
    fun `tipo desconhecido e erro (fail-closed) — nao ha regra que o valide`() {
        val e = validarDocumento("RNE", "123456789")
        assertTrue(e.erro)
        assertEquals(R.string.error_documento_invalido, e.texto)
    }

    @Test
    fun `tipo tolera grafia legada`() {
        assertFalse(validarDocumento(" cpf ", cpfValido).erro)
    }
}
