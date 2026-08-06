package dev.matheus.fluviapp.ui.viewmodel.helpers.porto

import dev.matheus.fluviapp.domain.porto.Porto
import dev.matheus.fluviapp.ui.states.ErroNomePorto
import dev.matheus.fluviapp.ui.states.FormPortoUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A validação do porto — e o invariante que a Localidade não precisava ter: o par `(nome, localidade)`
 * é único (ADR-0016 §5), porque o porto não tem chave natural que o resolva de graça.
 */
class ValidacaoPortoTest {

    private val central = Porto(id = "p1", nome = "Porto Central", localidadeId = "belem")

    private fun estado(
        nome: String = "Porto Novo",
        localidadeId: String = "belem",
        outros: List<Porto> = emptyList(),
    ) = FormPortoUiState(nome = nome, localidadeId = localidadeId, outrosPortos = outros)

    @Test
    fun `formulario completo e sem homonimo e valido`() {
        val erros = validarPorto(estado(outros = listOf(central)))

        assertTrue(erros.valido)
        assertEquals(ErroNomePorto.NENHUM, erros.nome)
    }

    @Test
    fun `nome em branco e obrigatorio`() {
        val erros = validarPorto(estado(nome = "   "))

        assertFalse(erros.valido)
        assertEquals(ErroNomePorto.OBRIGATORIO, erros.nome)
    }

    @Test
    fun `localidade e obrigatoria`() {
        val erros = validarPorto(estado(localidadeId = ""))

        assertFalse(erros.valido)
        assertTrue(erros.localidade)
    }

    /** Sem lugar não há "dentro do lugar": a queixa é da localidade, e o nome não é acusado junto. */
    @Test
    fun `sem localidade, o nome nao e acusado de duplicidade`() {
        val erros = validarPorto(estado(nome = "Porto Central", localidadeId = "", outros = listOf(central)))

        assertEquals(ErroNomePorto.NENHUM, erros.nome)
        assertTrue(erros.localidade)
    }

    @Test
    fun `mesmo nome na mesma localidade e duplicidade`() {
        val erros = validarPorto(estado(nome = "Porto Central", outros = listOf(central)))

        assertFalse(erros.valido)
        assertEquals(ErroNomePorto.DUPLICADO, erros.nome)
    }

    /** Caixa e espaços não fazem dois portos diferentes — a comparação é do nome, não da digitação. */
    @Test
    fun `duplicidade ignora caixa e espacos nas bordas`() {
        val erros = validarPorto(estado(nome = "  porto central ", outros = listOf(central)))

        assertEquals(ErroNomePorto.DUPLICADO, erros.nome)
    }

    /** É o par que é único: o mesmo nome em outra cidade é outro porto, e existe aos montes. */
    @Test
    fun `mesmo nome em outra localidade e permitido`() {
        val erros = validarPorto(estado(nome = "Porto Central", localidadeId = "manaus", outros = listOf(central)))

        assertTrue(erros.valido)
    }

    /**
     * Porto inativado não bloqueia: recusar por causa de um registro que a pessoa não pode ver seria uma
     * mensagem apontando para o que não está na tela.
     */
    @Test
    fun `homonimo inativo nao bloqueia o cadastro`() {
        val erros = validarPorto(estado(nome = "Porto Central", outros = listOf(central.copy(ativo = false))))

        assertTrue(erros.valido)
    }
}