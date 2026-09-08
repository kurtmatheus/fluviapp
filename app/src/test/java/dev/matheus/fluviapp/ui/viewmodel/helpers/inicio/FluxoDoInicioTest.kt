package dev.matheus.fluviapp.ui.viewmodel.helpers.inicio

import dev.matheus.fluviapp.domain.operacoes.Atuacao
import dev.matheus.fluviapp.domain.operacoes.Convite
import dev.matheus.fluviapp.domain.operacoes.Funcionario
import dev.matheus.fluviapp.domain.operacoes.Usuario
import dev.matheus.fluviapp.domain.rota.Rota
import dev.matheus.fluviapp.domain.viagem.AtuacaoDaEmpresa
import dev.matheus.fluviapp.domain.viagem.Embarcacao
import dev.matheus.fluviapp.domain.viagem.EscopoDoPool
import dev.matheus.fluviapp.domain.viagem.TipoEmbarcacao
import dev.matheus.fluviapp.domain.viagem.Viagem
import dev.matheus.fluviapp.fakes.FakeConviteRepository
import dev.matheus.fluviapp.fakes.FakeEmbarcacaoRepository
import dev.matheus.fluviapp.fakes.FakeLocalidadeRepository
import dev.matheus.fluviapp.fakes.FakePortoRepository
import dev.matheus.fluviapp.fakes.FakeRelogio
import dev.matheus.fluviapp.fakes.FakeRotaRepository
import dev.matheus.fluviapp.fakes.FakeUsuarioRepository
import dev.matheus.fluviapp.fakes.FakeViagemRepository
import dev.matheus.fluviapp.ui.states.InicioDaTela
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    private val usuarios = FakeUsuarioRepository()
    private val convites = FakeConviteRepository()
    private val relogio = FakeRelogio(tercaDeManha)

    /**
     * [comAcesso] é a política já respondida: `null` nas fontes significa *quem olha não é `ADM`*, e o
     * fluxo então não lê `users` nem `convites` ([ADR-0032] D6).
     */
    private fun fluxo(
        escopo: EscopoDoPool = EscopoDoPool.Concedido(atuacao),
        comAcesso: Boolean = false,
    ) = fluxoDoInicio(
        escopo = escopo,
        viagemRepository = viagens,
        rotaRepository = rotas,
        portoRepository = portos,
        localidadeRepository = localidades,
        embarcacaoRepository = embarcacoes,
        relogio = relogio,
        acesso = FontesDoAcesso(usuarios, convites).takeIf { comAcesso },
    )

    private fun cardsDe(tela: InicioDaTela) = (tela as InicioDaTela.DaEmpresa).disponiveis

    private fun usuario(email: String, ativo: Boolean = true) = Usuario(
        id = "uid-$email",
        email = email,
        username = email.substringBefore('@'),
        papel = Usuario.Papel.OPERADOR.name,
        ativo = ativo,
    )

    private fun convite(email: String) = Convite(
        email = email,
        nome = email.substringBefore('@'),
        papel = Usuario.Papel.OPERADOR,
        empresaId = "empresa-1",
        cargo = Funcionario.Cargo.AGENTE,
    )

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

    /**
     * O outro lado do mesmo relato: **a rota inativada** também tira o card, e sem recarga.
     *
     * Aqui as duas correções se encontram — o fluxo entrega o snapshot novo, e o domínio deixou de tratar
     * "rota inativada" como "rota existente" (`noEscopo`). Sem a segunda, este teste falharia com o card
     * ainda no lugar mesmo com a emissão em dia.
     */
    @Test
    fun `rota inativada tira o card sem recarga`() = runTest(UnconfinedTestDispatcher()) {
        rotas.rotas = listOf(rota)
        viagens.viagens = listOf(viagem)

        val vistos = mutableListOf<InicioDaTela>()
        val coleta = launch { fluxo().collect { vistos += it } }
        advanceUntilIdle()

        assertEquals(1, cardsDe(vistos.last()).size)

        rotas.inativar("r1")
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
     * A plataforma não vende, então não há saída a emitir para ela — e o fluxo diz isso **sem ler coleção
     * de viagem nenhuma**.
     *
     * O contador de leituras é o que mudou em 2026-09-08: antes, o fluxo ligava os cinco listeners, esperava os
     * cinco primeiros snapshots e só então perguntava ao domínio — que descartava tudo. Cinco esperas para
     * desenhar uma tela que não depende delas.
     */
    @Test
    fun `plataforma recebe o painel dela, e nao le as colecoes da operacao`() =
        runTest(UnconfinedTestDispatcher()) {
            rotas.rotas = listOf(rota)
            viagens.viagens = listOf(viagem)

            val vistos = mutableListOf<InicioDaTela>()
            val coleta = launch { fluxo(EscopoDoPool.Todo).collect { vistos += it } }
            advanceUntilIdle()

            assertEquals(InicioDaTela.DaPlataforma(), vistos.last())
            assertEquals("a plataforma não precisa das viagens", 0, viagens.leituras)
            coleta.cancel()
        }

    /** Sem concessão também não depende de leitura: a ausência já é conhecida antes de perguntar. */
    @Test
    fun `sem concessao responde sem ler nada`() = runTest(UnconfinedTestDispatcher()) {
        val vistos = mutableListOf<InicioDaTela>()
        val coleta = launch { fluxo(EscopoDoPool.Nenhum).collect { vistos += it } }
        advanceUntilIdle()

        assertEquals(InicioDaTela.SemConcessao, vistos.last())
        assertEquals(0, viagens.leituras)
        coleta.cancel()
    }

    // --- O Início do painel da plataforma: o acesso ([ADR-0032] D6) ---

    /**
     * **As métricas de acesso são o Início do `ADM`** (decisão do analista em 2026-09-08).
     *
     * O caso mede a ligação inteira: as duas coleções → a agregação do domínio → a face da tela. Que a
     * contagem em si está certa é assunto do `MetricasDeAcessoTest`.
     */
    @Test
    fun `plataforma com fontes de acesso recebe as metricas`() = runTest(UnconfinedTestDispatcher()) {
        usuarios.usuarios = listOf(
            usuario("ativo@x.com"),
            usuario("desativado@x.com", ativo = false),
        )
        convites.convites = listOf(convite("nao-veio@x.com"))

        val vistos = mutableListOf<InicioDaTela>()
        val coleta = launch { fluxo(EscopoDoPool.Todo, comAcesso = true).collect { vistos += it } }
        advanceUntilIdle()

        val acesso = (vistos.last() as InicioDaTela.DaPlataforma).acesso
        assertEquals(1, acesso?.ativos)
        assertEquals(1, acesso?.desativados)
        assertEquals(1, acesso?.convitesPendentes)
        coleta.cancel()
    }

    /**
     * **O número cai sozinho quando alguém entra** — a mesma exigência da lista de saídas, no assunto novo.
     * Ninguém pede recarga: o perfil aparece na coleção e o convite deixa de estar pendente.
     */
    @Test
    fun `convite pendente deixa de contar quando o perfil aparece`() = runTest(UnconfinedTestDispatcher()) {
        convites.convites = listOf(convite("vai-entrar@x.com"))

        val vistos = mutableListOf<InicioDaTela>()
        val coleta = launch { fluxo(EscopoDoPool.Todo, comAcesso = true).collect { vistos += it } }
        advanceUntilIdle()

        assertEquals(1, (vistos.last() as InicioDaTela.DaPlataforma).acesso?.convitesPendentes)

        usuarios.usuarios = listOf(usuario("vai-entrar@x.com"))
        advanceUntilIdle()

        assertEquals(0, (vistos.last() as InicioDaTela.DaPlataforma).acesso?.convitesPendentes)
        coleta.cancel()
    }

    /**
     * **Sem `ADM`, sem leitura** — e isso não é cosmética: `allow list` de `convites` é `ehAdm()`, então
     * ligar o listener para um `GESTOR` produziria *permission denied* e um não-fatal no Crashlytics. Um
     * alarme tocando no caso normal deixa de ser alarme.
     */
    @Test
    fun `plataforma sem fontes de acesso nao le usuarios nem convites`() =
        runTest(UnconfinedTestDispatcher()) {
            val vistos = mutableListOf<InicioDaTela>()
            val coleta = launch { fluxo(EscopoDoPool.Todo, comAcesso = false).collect { vistos += it } }
            advanceUntilIdle()

            assertEquals(InicioDaTela.DaPlataforma(), vistos.last())
            assertEquals("o listener de perfis não deve subir", 0, usuarios.leituras)
            assertEquals("nem o de convites", 0, convites.leituras)
            coleta.cancel()
        }

    /**
     * **O destaque de hoje vem do relógio, e não da ordem da lista.**
     *
     * A mesma viagem semanal, colhida em dois dias diferentes: na terça ela é a saída de hoje; na quarta
     * ela é a terça **seguinte**, continua sendo o primeiro item da lista e deixa de vir marcada. É o caso
     * que justifica o selo existir — *primeiro* e *hoje* são informações diferentes, e só a primeira a
     * ordenação sabia dar.
     *
     * O teste mede a ligação `Relogio → fluxoDoInicio → paraTela → card`; que a comparação de datas está
     * certa é assunto do `InicioDaTelaMapperTest`.
     */
    @Test
    fun `o destaque de hoje vem do relogio`() = runTest(UnconfinedTestDispatcher()) {
        rotas.rotas = listOf(rota)
        viagens.viagens = listOf(viagem)

        val naTerca = mutableListOf<InicioDaTela>()
        val coletaTerca = launch { fluxo().collect { naTerca += it } }
        advanceUntilIdle()
        assertTrue(cardsDe(naTerca.last()).single().ehHoje)
        coletaTerca.cancel()

        relogio.instante = tercaDeManha.plusDays(1)

        val naQuarta = mutableListOf<InicioDaTela>()
        val coletaQuarta = launch { fluxo().collect { naQuarta += it } }
        advanceUntilIdle()
        val card = cardsDe(naQuarta.last()).single()
        assertFalse("a terça seguinte não é hoje", card.ehHoje)
        assertEquals("Terça-feira, 18/08 · 18:00", card.partida)
        coletaQuarta.cancel()
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