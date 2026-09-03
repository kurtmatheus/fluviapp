package dev.matheus.fluviapp.ui.viewmodel.helpers.empresa

import dev.matheus.fluviapp.domain.operacoes.Atuacao
import dev.matheus.fluviapp.ui.states.FormEmpresaUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidacaoEmpresaTest {

    @Test
    fun `formulario vazio e invalido — falta o nome e falta a atuacao`() {
        val erros = validarEmpresa(FormEmpresaUiState())
        assertTrue(erros.nome)
        assertTrue(erros.atuacoes)
        // O CNPJ ausente **não** é erro: opcional desde 2026-09-03.
        assertFalse(erros.cnpj)
        assertFalse(erros.valido)
    }

    /** O que a decisão de 2026-09-03 mede: nome e atuação bastam, e mais nada é cobrado. */
    @Test
    fun `so o nome e a atuacao ja bastam`() {
        val erros = validarEmpresa(
            FormEmpresaUiState(nome = "ACME", atuacoes = setOf(Atuacao.AGENCIAMENTO)),
        )
        assertTrue(erros.valido)
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

    /** A parte que **não** foi relaxada: a atuação habilita a empresa, e sem ela não há o que habilitar. */
    @Test
    fun `nome sozinho nao basta — a atuacao continua obrigatoria`() {
        val erros = validarEmpresa(FormEmpresaUiState(nome = "ACME"))
        assertFalse(erros.nome)
        assertTrue(erros.atuacoes)
        assertFalse(erros.valido)
    }

    /** Opcional não é livre: vazio passa, preenchido tem de ser CNPJ de verdade. */
    @Test
    fun `cnpj preenchido e invalido reprova o formulario`() {
        val erros = validarEmpresa(
            FormEmpresaUiState(
                nome = "ACME",
                cnpj = "11222333000180",
                atuacoes = setOf(Atuacao.AGENCIAMENTO),
            ),
        )
        assertTrue(erros.cnpj)
        assertFalse(erros.valido)
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
