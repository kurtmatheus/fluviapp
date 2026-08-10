package dev.matheus.fluviapp.domain.viagem

import dev.matheus.fluviapp.domain.operacoes.Atuacao
import dev.matheus.fluviapp.domain.rota.Rota
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * **A divisão entre plataforma e empresa no Início, decidida pelo domínio** (decisão do analista,
 * 2026-08-10).
 *
 * Antes da revitalização a home listava viagens para todo mundo. Estes casos são o contrato de que ela
 * não volta assim: a plataforma monta o universo e não vende, então uma lista de saídas não responde
 * pergunta nenhuma dela.
 */
class InicioDoPainelTest {

    private val terca = LocalDate.of(2026, 8, 11)
    private val agora = terca.atStartOfDay()

    private val rotaConcedida = Rota("r1", "porto-a", "porto-b", tempoMedioH = 30.0)
    private val rotaDeFora = Rota("r2", "porto-c", "porto-b")
    private val rotasPorId = listOf(rotaConcedida, rotaDeFora).associateBy { it.id }

    private val atuacao = AtuacaoDaEmpresa(
        atuacao = Atuacao.AGENCIAMENTO,
        embarcacaoIds = setOf("e1"),
        portoIds = setOf("porto-a", "porto-b"),
    )

    private fun viagem(id: String = "v1", rotaId: String = "r1", embarcacaoId: String = "e1") =
        Viagem(id, rotaId, embarcacaoId, DayOfWeek.TUESDAY, 18 * 60)

    /** Quem administra a plataforma não vende: uma lista de saídas não é o Início dela. */
    @Test
    fun `a plataforma nao ve lista de saidas`() {
        val inicio = inicioDoPainel(EscopoDoPool.Todo, listOf(viagem()), rotasPorId, agora)

        assertEquals(InicioDoPainel.DaPlataforma, inicio)
    }

    @Test
    fun `a empresa ve as saidas que pode ofertar`() {
        val inicio = inicioDoPainel(
            EscopoDoPool.Concedido(atuacao),
            listOf(viagem()),
            rotasPorId,
            agora,
        )

        assertEquals(
            listOf("v1"),
            (inicio as InicioDoPainel.DaEmpresa).disponiveis.map { it.viagem.id },
        )
    }

    /** O recorte é o mesmo da busca e do cadastro — é o que impede o Início de discordar delas. */
    @Test
    fun `o Inicio nao mostra o que a busca esconderia`() {
        val viagens = listOf(
            viagem(id = "minha"),
            viagem(id = "rota-alheia", rotaId = "r2"),
            viagem(id = "navio-alheio", embarcacaoId = "e2"),
        )

        val inicio = inicioDoPainel(EscopoDoPool.Concedido(atuacao), viagens, rotasPorId, agora)

        assertEquals(
            listOf("minha"),
            (inicio as InicioDoPainel.DaEmpresa).disponiveis.map { it.viagem.id },
        )
    }

    /**
     * **Sem concessão é estado próprio, não lista vazia** — e a diferença é o recado: uma manda esperar
     * a próxima semana, a outra manda procurar a plataforma.
     */
    @Test
    fun `sem concessao e distinto de empresa sem saida`() {
        val semNada = inicioDoPainel(EscopoDoPool.Nenhum, listOf(viagem()), rotasPorId, agora)
        assertEquals(InicioDoPainel.SemConcessao, semNada)

        val semSaida = inicioDoPainel(EscopoDoPool.Concedido(atuacao), emptyList(), rotasPorId, agora)
        assertTrue((semSaida as InicioDoPainel.DaEmpresa).disponiveis.isEmpty())
    }

    /** A janela atravessa o domínio inteiro: o Início herda "já partiu" de `disponiveisAPartirDe`. */
    @Test
    fun `a saida que ja partiu nao aparece no Inicio`() {
        val manha = Viagem("v1", "r1", "e1", DayOfWeek.TUESDAY, 6 * 60)

        val inicio = inicioDoPainel(
            EscopoDoPool.Concedido(atuacao),
            listOf(manha),
            rotasPorId,
            terca.atTime(18, 0),
        )

        assertTrue((inicio as InicioDoPainel.DaEmpresa).disponiveis.isEmpty())
    }
}