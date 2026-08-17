package dev.matheus.fluviapp.ui.viewmodel.helpers.inicio

import dev.matheus.fluviapp.domain.operacoes.Atuacao
import dev.matheus.fluviapp.domain.rota.Rota
import dev.matheus.fluviapp.domain.viagem.AtuacaoDaEmpresa
import dev.matheus.fluviapp.domain.viagem.Embarcacao
import dev.matheus.fluviapp.domain.viagem.EscopoDoPool
import dev.matheus.fluviapp.domain.viagem.TipoEmbarcacao
import dev.matheus.fluviapp.domain.viagem.Viagem
import dev.matheus.fluviapp.fakes.FakeEmbarcacaoRepository
import dev.matheus.fluviapp.fakes.FakeLocalidadeRepository
import dev.matheus.fluviapp.fakes.FakePortoRepository
import dev.matheus.fluviapp.fakes.FakeRelogio
import dev.matheus.fluviapp.fakes.FakeRotaRepository
import dev.matheus.fluviapp.fakes.FakeViagemRepository
import dev.matheus.fluviapp.ui.states.InicioDaTela
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime

/**
 * **A reatividade do Início** — o defeito de 2026-08-17.
 *
 * O relato: uma viagem e uma rota foram inativadas pelo painel, e o card da tela inicial continuou lá até o
 * app ser reaberto. A causa não era o dado (o listener da coleção sempre esteve vivo), e sim o consumidor: o
 * Início lia uma vez, no `init` do ViewModel, e ficava com aquela fotografia enquanto a home estivesse na
 * pilha de navegação.
 *
 * É por isso que estes testes **mudam o fake depois de o fluxo já estar coletando**, e nunca chamam nada
 * parecido com "recarregar": se passassem com uma recarga explícita, provariam o contrário do que interessa.
 */
class FluxoDoInicioTest {

    private val belem = "porto-belem"
    private val parintins = "porto-parintins"

    private val rota = Rota(id = "r1", portoOrigemId = belem, portoDestinoId = parintins)

    private val atuacao = AtuacaoDaEmpresa(
        atuacao = Atuacao.AGENCIAMENTO,
        embarcacaoIds = setOf("emb-1"),
        portoIds = setOf(belem, parintins),
    )

    /** Terça, 11 de agosto de 2026, de manhã — a saída das 18:00 do mesmo dia ainda está por vir. */
    private val tercaDeManha = LocalDateTime.of(2026, 8, 11, 9, 0)

    private val viagem = Viagem(
        id = "v1",
        rotaId = "r1",
        embarcacaoId = "emb-1",
        diaSemana = DayOfWeek.TUESDAY,
        horaMin = 18 * 60,
    )

    private val viagens = FakeViagemRepository()
    private val rotas = FakeRotaRepository()
    private val portos = FakePortoRepository()
    private val localidades = FakeLocalidadeRepository()
    private val embarcacoes = FakeEmbarcacaoRepository()
    private val relogio = FakeRelogio(tercaDeManha)

    private fun fluxo(escopo: EscopoDoPool = EscopoDoPool.Concedido(atuacao)) = fluxoDoInicio(
        escopo = escopo,
        viagemRepository = viagens,
        rotaRepository = rotas,
        portoRepository = portos,
        localidadeRepository = localidades,
        embarcacaoRepository = embarcacoes,
        relogio = relogio,
    )

    private fun cardsDe(tela: InicioDaTela) = (tela as InicioDaTela.DaEmpresa).disponiveis

    /**
     * **O defeito, em forma de teste.** Ninguém pede recarga: a viagem é inativada e a emissão seguinte já
     * chega sem ela.
     */
    @Test
    fun `viagem inativada sai do inicio sem ninguem pedir recarga`() =
        runTest(UnconfinedTestDispatcher()) {
            rotas.rotas = listOf(rota)
            viagens.viagens = listOf(viagem)

            val vistos = mutableListOf<InicioDaTela>()
            val coleta = launch { fluxo().collect { vistos += it } }
            advanceUntilIdle()

            assertEquals(1, cardsDe(vistos.last()).size)

            viagens.inativar("v1")
            advanceUntilIdle()

            assertTrue(cardsDe(vistos.last()).isEmpty())
            coleta.cancel()
        }

    /** A viagem que **entra** chega pelo mesmo caminho — o fluxo não é um observador de remoções. */
    @Test
    fun `viagem criada aparece no inicio sem recarga`() = runTest(UnconfinedTestDispatcher()) {
        rotas.rotas = listOf(rota)

        val vistos = mutableListOf<InicioDaTela>()
        val coleta = launch { fluxo().collect { vistos += it } }
        advanceUntilIdle()

        assertTrue(cardsDe(vistos.last()).isEmpty())

        viagens.criar(viagem.copy(id = ""))
        advanceUntilIdle()

        assertEquals(1, cardsDe(vistos.last()).size)
        coleta.cancel()
    }

    /**
     * Rótulo que muda **também** é mudança: renomear a embarcação reescreve o card, e é a prova de que as
     * cinco coleções entram no fluxo, não só as duas que decidem se a ocorrência existe.
     */
    @Test
    fun `renomear a embarcacao reescreve o card`() = runTest(UnconfinedTestDispatcher()) {
        rotas.rotas = listOf(rota)
        viagens.viagens = listOf(viagem)
        embarcacoes.embarcacoes = listOf(embarcacaoChamada("F/B Modelo"))

        val vistos = mutableListOf<InicioDaTela>()
        val coleta = launch { fluxo().collect { vistos += it } }
        advanceUntilIdle()

        assertEquals("F/B Modelo", cardsDe(vistos.last()).single().embarcacao)

        embarcacoes.embarcacoes = listOf(embarcacaoChamada("N/M Outro"))
        advanceUntilIdle()

        assertEquals("N/M Outro", cardsDe(vistos.last()).single().embarcacao)
        coleta.cancel()
    }

    /**
     * A plataforma não vende, então não há saída a emitir para ela — e o fluxo diz isso **uma vez**, sem
     * depender do que as coleções trazem.
     */
    @Test
    fun `plataforma recebe o painel dela, e nao uma lista`() = runTest(UnconfinedTestDispatcher()) {
        rotas.rotas = listOf(rota)
        viagens.viagens = listOf(viagem)

        val vistos = mutableListOf<InicioDaTela>()
        val coleta = launch { fluxo(EscopoDoPool.Todo).collect { vistos += it } }
        advanceUntilIdle()

        assertEquals(InicioDaTela.DaPlataforma, vistos.last())
        coleta.cancel()
    }

    /** Sem concessão é estado próprio: o recado é "falta provisionar", não "não há saída". */
    @Test
    fun `sem concessao nao vira lista vazia`() = runTest(UnconfinedTestDispatcher()) {
        val vistos = mutableListOf<InicioDaTela>()
        val coleta = launch { fluxo(EscopoDoPool.Nenhum).collect { vistos += it } }
        advanceUntilIdle()

        assertEquals(InicioDaTela.SemConcessao, vistos.last())
        coleta.cancel()
    }

    private fun embarcacaoChamada(nome: String) = Embarcacao(
        id = "emb-1",
        descricaoNome = nome,
        tipo = TipoEmbarcacao.NAVIO,
        capacidadeVeiculo = 0,
        capacidadeSuite2 = 0,
        capacidadeSuite3 = 0,
        capacidadeCamarote = 0,
        empresaId = "empresa-1",
    )
}