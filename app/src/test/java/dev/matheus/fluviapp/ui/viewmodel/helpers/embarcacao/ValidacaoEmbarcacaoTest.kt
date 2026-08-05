package dev.matheus.fluviapp.ui.viewmodel.helpers.embarcacao

import dev.matheus.fluviapp.domain.viagem.TipoEmbarcacao
import dev.matheus.fluviapp.ui.states.FormEmbarcacaoUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidacaoEmbarcacaoTest {

    private fun preenchido(
        nome: String = "FLUVI I",
        empresa: String = "ACME",
        tipo: TipoEmbarcacao? = TipoEmbarcacao.FERRY_BOAT,
    ) = FormEmbarcacaoUiState(nome = nome, empresa = empresa, tipo = tipo)

    @Test
    fun `campos obrigatorios em branco sao invalidos`() {
        val erros = validarEmbarcacao(FormEmbarcacaoUiState())
        assertTrue(erros.nome)
        assertTrue(erros.empresa)
        assertTrue(erros.tipo)
        assertFalse(erros.valido)
    }

    @Test
    fun `nome, tipo e empresa preenchidos sao validos (capacidades opcionais)`() {
        val erros = validarEmbarcacao(preenchido())
        assertFalse(erros.nome)
        assertFalse(erros.empresa)
        assertFalse(erros.tipo)
        assertTrue(erros.valido)
    }

    @Test
    fun `so o nome sem empresa e invalido`() {
        val erros = validarEmbarcacao(preenchido(empresa = ""))
        assertFalse(erros.nome)
        assertTrue(erros.empresa)
        assertFalse(erros.valido)
    }

    /**
     * O invariante do domínio virando regra de formulário: `Embarcacao` não tem tipo nulo, então um form
     * sem tipo escolhido não tem o que construir. Sem esta linha, a única defesa seria um `!!`.
     */
    @Test
    fun `sem tipo escolhido e invalido, mesmo com o resto completo`() {
        val erros = validarEmbarcacao(preenchido(tipo = null))
        assertFalse(erros.nome)
        assertFalse(erros.empresa)
        assertTrue(erros.tipo)
        assertFalse(erros.valido)
    }

    /**
     * Capacidade de veículo com tipo que não leva veículo **não** é erro de validação: a pergunta nem
     * aparece na tela e o VM zera o valor ao trocar de tipo. Se um dia isto virar `true`, é sinal de que
     * alguém conseguiu digitar o que a tela não oferece — e o lugar de consertar seria lá, não aqui.
     */
    @Test
    fun `lancha com capacidade de veiculo nao e erro de validacao — a tela nao deixa chegar aqui`() {
        val estado = preenchido(tipo = TipoEmbarcacao.LANCHA).copy(capacidadeVeiculo = "12")

        assertTrue(validarEmbarcacao(estado).valido)
        assertFalse(estado.perguntaCapacidadeVeiculo)
    }
}