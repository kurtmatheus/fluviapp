package dev.matheus.fluviapp.ui.viewmodel.helpers.empresa

import dev.matheus.fluviapp.domain.operacoes.Atuacao
import dev.matheus.fluviapp.ui.states.FormEmpresaUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidacaoEmpresaTest {

    @Test
    fun `campos em branco sao invalidos`() {
        val erros = validarEmpresa(FormEmpresaUiState())
        assertTrue(erros.nome)
        assertTrue(erros.razaoSocial)
        assertTrue(erros.cnpj)
        assertFalse(erros.valido)
    }

    @Test
    fun `estado completo e valido`() {
        val erros = validarEmpresa(
            FormEmpresaUiState(
                nome = "ACME",
                razaoSocial = "ACME LTDA",
                cnpj = "11222333000181",
                atuacoes = setOf(Atuacao.AGENCIAMENTO),
            ),
        )
        assertTrue(erros.valido)
    }

    @Test
    fun `cnpj valido passa`() {
        assertTrue(cnpjValido("11222333000181"))
    }

    @Test
    fun `cnpj com digito verificador errado falha`() {
        assertFalse(cnpjValido("11222333000180"))
    }

    @Test
    fun `cnpj com tamanho errado falha`() {
        assertFalse(cnpjValido("112223330001"))
    }

    @Test
    fun `cnpj com digitos repetidos falha`() {
        assertFalse(cnpjValido("11111111111111"))
    }
}
