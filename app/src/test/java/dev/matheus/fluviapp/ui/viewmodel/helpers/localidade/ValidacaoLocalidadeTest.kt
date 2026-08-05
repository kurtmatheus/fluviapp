package dev.matheus.fluviapp.ui.viewmodel.helpers.localidade

import dev.matheus.fluviapp.domain.localidade.Uf
import dev.matheus.fluviapp.ui.states.FormLocalidadeUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A validação da localidade, e em especial **a verificação que não precisa de rede**: os dois primeiros
 * dígitos do código do IBGE são o código da UF (ADR-0016 §5).
 */
class ValidacaoLocalidadeTest {

    private fun estado(
        municipio: String = "Belém",
        uf: Uf? = Uf.PA,
        codigoIbge: String = "1501402",
    ) = FormLocalidadeUiState(municipio = municipio, uf = uf, codigoIbge = codigoIbge)

    @Test
    fun `formulario vazio e invalido nos tres campos`() {
        val erros = validarLocalidade(FormLocalidadeUiState())

        assertTrue(erros.municipio)
        assertTrue(erros.uf)
        assertTrue(erros.codigoIbge)
        assertFalse(erros.valido)
    }

    @Test
    fun `municipio, uf e codigo coerentes sao validos`() {
        assertTrue(validarLocalidade(estado()).valido)
    }

    /** `15` é o Pará: um código de Belém numa localidade do Amazonas é incoerente. */
    @Test
    fun `codigo de outra uf e recusado`() {
        val erros = validarLocalidade(estado(uf = Uf.AM))

        assertTrue(erros.codigoIbge)
        assertFalse(erros.valido)
    }

    @Test
    fun `codigo com menos de sete digitos e recusado`() {
        assertTrue(validarLocalidade(estado(codigoIbge = "150140")).codigoIbge)
    }

    /** Prefixo que não é UF nenhuma (não existe `99`): não é código do IBGE, mesmo sem UF escolhida. */
    @Test
    fun `prefixo que nao e uf e recusado ate sem uf escolhida`() {
        assertTrue(validarLocalidade(estado(uf = null, codigoIbge = "9901402")).codigoIbge)
    }

    /**
     * Com o código coerente e a UF ainda em branco, o erro é **da UF**, não do código. A distinção evita
     * a mensagem que manda corrigir o campo certo pela razão errada.
     */
    @Test
    fun `codigo valido sem uf escolhida acusa a uf, nao o codigo`() {
        val erros = validarLocalidade(estado(uf = null))

        assertTrue(erros.uf)
        assertFalse(erros.codigoIbge)
    }

    @Test
    fun `municipio em branco e invalido`() {
        assertTrue(validarLocalidade(estado(municipio = "  ")).municipio)
    }

    /** A tabela do IBGE inteira, pelos prefixos: toda UF tem código e todo código volta para a UF. */
    @Test
    fun `todo prefixo de uf e reconhecido`() {
        Uf.entries.forEach { uf ->
            val codigo = uf.codigo + "01402"
            assertTrue(
                "código $codigo deveria ser aceito para ${uf.sigla}",
                validarLocalidade(estado(uf = uf, codigoIbge = codigo)).valido,
            )
        }
    }
}