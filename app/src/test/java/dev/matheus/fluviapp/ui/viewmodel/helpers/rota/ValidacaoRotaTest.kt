package dev.matheus.fluviapp.ui.viewmodel.helpers.rota

import dev.matheus.fluviapp.domain.rota.Rota
import dev.matheus.fluviapp.ui.states.ErroParRota
import dev.matheus.fluviapp.ui.states.FormRotaUiState
import dev.matheus.fluviapp.ui.states.PortoOpcao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A validação da rota, e a **ordem** das três regras do par: faltando → repetido → duplicado.
 *
 * A ordem não é estética: acusar duplicidade antes de saber que os dois portos existem seria comparar
 * com metade da informação, e acusá-la antes do sentido diria "já existe" para algo que nem é travessia.
 */
class ValidacaoRotaTest {

    private val portos = listOf(
        PortoOpcao("porto-a", "Porto A · Belém/PA"),
        PortoOpcao("porto-b", "Porto B · Parintins/AM"),
    )

    private fun estado(
        origem: String = "Porto A · Belém/PA",
        destino: String = "Porto B · Parintins/AM",
        distancia: String = "420",
        tempo: String = "30",
        existentes: List<Rota> = emptyList(),
    ) = FormRotaUiState(
        portoOrigem = origem,
        portoDestino = destino,
        distanciaMn = distancia,
        tempoMedioH = tempo,
        portos = portos,
        rotasExistentes = existentes,
    )

    @Test
    fun `par valido com medidas positivas passa`() {
        assertTrue(validarRota(estado()).valido)
    }

    @Test
    fun `porto faltando e obrigatorio`() {
        assertEquals(ErroParRota.OBRIGATORIO, validarRota(estado(origem = "")).par)
        assertEquals(ErroParRota.OBRIGATORIO, validarRota(estado(destino = "")).par)
    }

    /** Rótulo que não casa com opção nenhuma vale como ausência: não aponta para porto nenhum. */
    @Test
    fun `rotulo desconhecido conta como faltando`() {
        assertEquals(ErroParRota.OBRIGATORIO, validarRota(estado(origem = "Porto Fantasma")).par)
    }

    @Test
    fun `mesmo porto nos dois lados nao e travessia`() {
        val erros = validarRota(estado(destino = "Porto A · Belém/PA"))

        assertEquals(ErroParRota.MESMO_PORTO, erros.par)
        assertFalse(erros.valido)
    }

    @Test
    fun `par ja existente e duplicidade`() {
        val existente = Rota(id = "r1", portoOrigemId = "porto-a", portoDestinoId = "porto-b")

        assertEquals(ErroParRota.DUPLICADA, validarRota(estado(existentes = listOf(existente))).par)
    }

    /** **Ida e volta são rotas diferentes** — o par é ordenado, e a duplicidade respeita isso. */
    @Test
    fun `o sentido inverso nao e duplicidade`() {
        val volta = Rota(id = "r1", portoOrigemId = "porto-b", portoDestinoId = "porto-a")

        assertTrue(validarRota(estado(existentes = listOf(volta))).valido)
    }

    /** Recusar por causa de uma inativa impediria de recriar o que se acabou de corrigir. */
    @Test
    fun `rota inativa com o mesmo par nao bloqueia`() {
        val inativa = Rota(id = "r1", portoOrigemId = "porto-a", portoDestinoId = "porto-b", ativo = false)

        assertTrue(validarRota(estado(existentes = listOf(inativa))).valido)
    }

    // --- Medidas: são o que justifica a Rota existir em vez de ser derivada dos portos ---

    @Test
    fun `distancia e tempo precisam ser maiores que zero`() {
        assertTrue(validarRota(estado(distancia = "")).distancia)
        assertTrue(validarRota(estado(distancia = "0")).distancia)
        assertTrue(validarRota(estado(tempo = "0")).tempo)
        assertFalse(validarRota(estado(distancia = "0.5", tempo = "0.5")).valido.not())
    }
}