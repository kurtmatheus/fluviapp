package dev.matheus.fluviapp.ui.viewmodel.passagem

import dev.matheus.fluviapp.domain.documento.TipoDocumento
import dev.matheus.fluviapp.domain.passagem.Acomodacao
import dev.matheus.fluviapp.domain.passagem.CategoriaPassagem
import dev.matheus.fluviapp.domain.passagem.ClasseVeiculo
import dev.matheus.fluviapp.domain.passagem.FormaPagamento
import dev.matheus.fluviapp.domain.passagem.MetadadosPassagem
import dev.matheus.fluviapp.domain.passagem.PassagemDePassageiro
import dev.matheus.fluviapp.domain.passagem.PassagemDeVeiculo
import dev.matheus.fluviapp.domain.passagem.ResultadoEmissao
import dev.matheus.fluviapp.domain.passagem.StatusPassagem
import dev.matheus.fluviapp.domain.passagem.TipoGratuidade
import dev.matheus.fluviapp.domain.passagem.TipoPassagem
import dev.matheus.fluviapp.domain.viagem.OcorrenciaViagem
import dev.matheus.fluviapp.fakes.FakeClienteRepository
import dev.matheus.fluviapp.fakes.FakePassagemRepository
import dev.matheus.fluviapp.fakes.FakeRelogio
import dev.matheus.fluviapp.fakes.FakeSessaoUsuario
import dev.matheus.fluviapp.fakes.FakeVeiculoRepository
import dev.matheus.fluviapp.revitalizacao.ForaDoEscopo
import dev.matheus.fluviapp.ui.states.passagem.CabecalhoDaViagem
import dev.matheus.fluviapp.ui.states.passagem.ClienteEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.ErroDeEmissao
import dev.matheus.fluviapp.ui.states.passagem.LancamentoEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.PagamentoEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.ParticipanteEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.PassoEmissao
import dev.matheus.fluviapp.ui.states.passagem.VeiculoEmEdicao
import dev.matheus.fluviapp.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * **A emissão, ponta a ponta em JVM** ([ADR-0028], [ADR-0026] D1/D3).
 *
 * O que estes casos cobrem é a sequência que o ViewModel passou a orquestrar — validar, registrar no pool,
 * avaliar as guardas, reservar o número, emitir — e, principalmente, **onde ela para**: cada falha tem de
 * deixar o atendimento intacto na tela, porque digitar tudo de novo por causa de rede é o que a decisão de
 * *tolerar falha* proíbe.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Category(ForaDoEscopo::class)
class EmissaoViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val ocorrencia = OcorrenciaViagem(viagemId = "viagem-1", data = LocalDate.of(2026, 8, 18))

    private val ana = ClienteEmEdicao(
        nome = "Ana Ribeiro",
        tipoDocumento = TipoDocumento.CPF,
        numeroDocumento = "529.982.247-25",
        dataNascimento = "30/01/1996",
    )

    private val bruno = ClienteEmEdicao(
        nome = "Bruno Costa",
        tipoDocumento = TipoDocumento.CPF,
        numeroDocumento = "111.444.777-35",
        dataNascimento = "10/05/1980",
    )

    private fun vm(
        passagens: FakePassagemRepository = FakePassagemRepository(),
        clientes: FakeClienteRepository = FakeClienteRepository(),
        veiculos: FakeVeiculoRepository = FakeVeiculoRepository(),
        sessao: FakeSessaoUsuario = FakeSessaoUsuario.supervisor(),
    ) = EmissaoViewModel(
        passagemRepository = passagens,
        clienteRepository = clientes,
        veiculoRepository = veiculos,
        sessaoUsuario = sessao,
        relogio = FakeRelogio(LocalDateTime.of(2026, 8, 13, 9, 30)),
    ).also { it.iniciar(ocorrencia, CabecalhoDaViagem(travessia = "A → B", partida = "Terça, 18/08 · 18:00")) }

    /** Leva o ViewModel até o passo de pagamento com uma rede para a Ana. */
    private fun EmissaoViewModel.atePagamentoEmRede() {
        escolherCategoria(CategoriaPassagem.PASSAGEIRO)
        escolherAcomodacao(Acomodacao.REDE)
        avancar()
        preencherPessoa(0, ana)
        avancar()
    }

    private fun pagamentoEmDinheiro(valor: String = "150,00") =
        PagamentoEmEdicao(lancamentos = listOf(LancamentoEmEdicao(FormaPagamento.DINHEIRO, valor)))

    // --- Os passos ---

    @Test
    fun `sem escolher acomodacao, o passo 1 nao avanca`() = runTest {
        val viewModel = vm()

        viewModel.escolherCategoria(CategoriaPassagem.PASSAGEIRO)
        viewModel.avancar()

        assertEquals(PassoEmissao.BILHETE, viewModel.uiState.value.passo)
        assertEquals(setOf(ErroDeEmissao.ACOMODACAO_NAO_ESCOLHIDA), viewModel.uiState.value.erros)
    }

    /** O veículo não ocupa acomodação de pessoa — cobrar uma dele seria cobrar um campo que não existe. */
    @Test
    fun `bilhete de veiculo avanca sem acomodacao`() = runTest {
        val viewModel = vm()

        viewModel.escolherCategoria(CategoriaPassagem.VEICULO)
        viewModel.avancar()

        assertEquals(PassoEmissao.PARTICIPANTE, viewModel.uiState.value.passo)
    }

    /** Trocar a categoria **troca o objeto**: é o que apaga a limpeza reativa do formulário antigo. */
    @Test
    fun `trocar a categoria troca o participante inteiro`() = runTest {
        val viewModel = vm()

        viewModel.escolherCategoria(CategoriaPassagem.PASSAGEIRO)
        viewModel.escolherAcomodacao(Acomodacao.SUITE)
        viewModel.preencherPessoa(0, ana)
        viewModel.escolherCategoria(CategoriaPassagem.VEICULO)

        assertTrue(viewModel.uiState.value.participante is ParticipanteEmEdicao.DeVeiculo)
        assertNull(viewModel.uiState.value.bilhete.acomodacao)
    }

    /** Fora da rede não há meia nem gratuidade: manter a escolha anterior seria guardar estado ilegal. */
    @Test
    fun `trocar para suite devolve o tipo para inteira`() = runTest {
        val viewModel = vm()

        viewModel.escolherCategoria(CategoriaPassagem.PASSAGEIRO)
        viewModel.escolherAcomodacao(Acomodacao.REDE)
        viewModel.escolherTipo(TipoPassagem.GRATUIDADE)
        viewModel.escolherGratuidade(TipoGratuidade.IDOSO)
        viewModel.escolherAcomodacao(Acomodacao.SUITE)

        assertEquals(TipoPassagem.INTEIRA, viewModel.uiState.value.bilhete.tipo)
        assertNull(viewModel.uiState.value.bilhete.gratuidade)
    }

    /** A acomodação **redimensiona** o passo 2: o que não cabe mais é descartado, não carregado em silêncio. */
    @Test
    fun `trocar de suite para rede descarta os acompanhantes`() = runTest {
        val viewModel = vm()

        viewModel.escolherCategoria(CategoriaPassagem.PASSAGEIRO)
        viewModel.escolherAcomodacao(Acomodacao.SUITE)
        viewModel.acrescentarAcompanhante()
        viewModel.preencherPessoa(0, ana)
        viewModel.preencherPessoa(1, bruno)
        viewModel.escolherAcomodacao(Acomodacao.REDE)

        val pessoas = (viewModel.uiState.value.participante as ParticipanteEmEdicao.DePassageiro).pessoas
        assertEquals(1, pessoas.size)
        assertEquals("Ana Ribeiro", pessoas.single().nome)
    }

    @Test
    fun `a rede nao aceita acompanhante`() = runTest {
        val viewModel = vm()

        viewModel.escolherCategoria(CategoriaPassagem.PASSAGEIRO)
        viewModel.escolherAcomodacao(Acomodacao.REDE)
        viewModel.acrescentarAcompanhante()

        val pessoas = (viewModel.uiState.value.participante as ParticipanteEmEdicao.DePassageiro).pessoas
        assertEquals(1, pessoas.size)
    }

    @Test
    fun `gratuidade sem subtipo nao avanca`() = runTest {
        val viewModel = vm()

        viewModel.escolherCategoria(CategoriaPassagem.PASSAGEIRO)
        viewModel.escolherAcomodacao(Acomodacao.REDE)
        viewModel.escolherTipo(TipoPassagem.GRATUIDADE)
        viewModel.avancar()

        assertEquals(setOf(ErroDeEmissao.GRATUIDADE_NAO_ESCOLHIDA), viewModel.uiState.value.erros)
    }

    @Test
    fun `sem titular, o passo 2 nao avanca`() = runTest {
        val viewModel = vm()

        viewModel.escolherCategoria(CategoriaPassagem.PASSAGEIRO)
        viewModel.escolherAcomodacao(Acomodacao.REDE)
        viewModel.avancar()
        viewModel.avancar()

        assertEquals(PassoEmissao.PARTICIPANTE, viewModel.uiState.value.passo)
        assertEquals(setOf(ErroDeEmissao.TITULAR_INCOMPLETO), viewModel.uiState.value.erros)
    }

    /** Quem volta está corrigindo: cobrar o passo na saída seria prendê-lo nele. */
    @Test
    fun `voltar nao valida`() = runTest {
        val viewModel = vm()

        viewModel.escolherCategoria(CategoriaPassagem.PASSAGEIRO)
        viewModel.escolherAcomodacao(Acomodacao.REDE)
        viewModel.avancar()
        viewModel.voltar()

        assertEquals(PassoEmissao.BILHETE, viewModel.uiState.value.passo)
        assertTrue(viewModel.uiState.value.erros.isEmpty())
    }

    // --- A emissão ---

    @Test
    fun `emitir registra a pessoa no pool, reserva o numero e cria a passagem`() = runTest {
        val passagens = FakePassagemRepository().apply { proximoNumero = 41 }
        val clientes = FakeClienteRepository()
        val viewModel = vm(passagens, clientes)
        val eventos = mutableListOf<EventoDeEmissao>()
        val coleta = launch { viewModel.eventos.collect { eventos += it } }

        viewModel.atePagamentoEmRede()
        viewModel.preencherPagamento(pagamentoEmDinheiro())
        viewModel.avancar()
        advanceUntilIdle()

        val emitida = passagens.emitidas.single() as PassagemDePassageiro
        assertEquals("41", emitida.numero)
        assertEquals(StatusPassagem.EMITIDA, emitida.metadados.status)
        assertEquals(ocorrencia, emitida.ocorrencia)
        assertEquals(BigDecimal("150.00"), emitida.lancamentos.single().valor)
        // O bilhete referencia o cliente por id, e o id é a chave natural do pool.
        assertEquals(listOf("CPF:52998224725"), emitida.clientes)
        assertEquals(1, clientes.criados.size)
        assertTrue(eventos.single() is EventoDeEmissao.Emitida)
        coleta.cancel()
    }

    @Test
    fun `emitir veiculo registra o veiculo e dispensa responsavel`() = runTest {
        val passagens = FakePassagemRepository()
        val veiculos = FakeVeiculoRepository()
        val clientes = FakeClienteRepository()
        val viewModel = vm(passagens, clientes, veiculos)

        viewModel.escolherCategoria(CategoriaPassagem.VEICULO)
        viewModel.avancar()
        viewModel.preencherVeiculo(
            VeiculoEmEdicao(placa = "ABC1D23", classe = ClasseVeiculo.MOTO, modelo = "Fan", cilindrada = "150"),
        )
        viewModel.avancar()
        viewModel.preencherPagamento(pagamentoEmDinheiro("80,00"))
        viewModel.avancar()
        advanceUntilIdle()

        val emitida = passagens.emitidas.single() as PassagemDeVeiculo
        assertEquals("ABC1D23", emitida.veiculoId)
        assertNull(emitida.responsavelRetirada)
        assertEquals(1, veiculos.criados.size)
        assertTrue(clientes.criados.isEmpty())
    }

    /** Gratuidade é tarifa zero por lei, não pagamento de zero: o passo 3 não cobra dinheiro dela. */
    @Test
    fun `gratuidade emite sem lancamento nenhum`() = runTest {
        val passagens = FakePassagemRepository()
        val viewModel = vm(passagens)

        viewModel.escolherCategoria(CategoriaPassagem.PASSAGEIRO)
        viewModel.escolherAcomodacao(Acomodacao.REDE)
        viewModel.escolherTipo(TipoPassagem.GRATUIDADE)
        viewModel.escolherGratuidade(TipoGratuidade.IDOSO)
        viewModel.avancar()
        viewModel.preencherPessoa(0, ana)
        viewModel.avancar()
        viewModel.avancar()
        advanceUntilIdle()

        val emitida = passagens.emitidas.single() as PassagemDePassageiro
        assertTrue(emitida.lancamentos.isEmpty())
        assertEquals(TipoGratuidade.IDOSO, emitida.gratuidade)
    }

    /** A cota do ADR-0013 §8, contada **por ocorrência** e atravessando agências. */
    @Test
    fun `terceira gratuidade da mesma categoria na mesma saida e bloqueada`() = runTest {
        val passagens = FakePassagemRepository().apply {
            passagens = listOf(
                gratuidadeJaEmitida("p-1", TipoGratuidade.IDOSO),
                gratuidadeJaEmitida("p-2", TipoGratuidade.IDOSO),
            )
        }
        val viewModel = vm(passagens)
        val eventos = mutableListOf<EventoDeEmissao>()
        val coleta = launch { viewModel.eventos.collect { eventos += it } }

        viewModel.escolherCategoria(CategoriaPassagem.PASSAGEIRO)
        viewModel.escolherAcomodacao(Acomodacao.REDE)
        viewModel.escolherTipo(TipoPassagem.GRATUIDADE)
        viewModel.escolherGratuidade(TipoGratuidade.IDOSO)
        viewModel.avancar()
        viewModel.preencherPessoa(0, ana)
        viewModel.avancar()
        viewModel.avancar()
        advanceUntilIdle()

        val bloqueio = eventos.single() as EventoDeEmissao.Bloqueada
        assertEquals(ResultadoEmissao.CotaGratuidadeAtingida(TipoGratuidade.IDOSO), bloqueio.motivo)
        assertTrue(passagens.emitidas.isEmpty())
        coleta.cancel()
    }

    /** Cancelada não ocupa, então também não consome cota. */
    @Test
    fun `gratuidade cancelada nao consome cota`() = runTest {
        val passagens = FakePassagemRepository().apply {
            passagens = listOf(
                gratuidadeJaEmitida("p-1", TipoGratuidade.IDOSO),
                gratuidadeJaEmitida("p-2", TipoGratuidade.IDOSO, StatusPassagem.CANCELADA),
            )
        }
        val viewModel = vm(passagens)

        viewModel.escolherCategoria(CategoriaPassagem.PASSAGEIRO)
        viewModel.escolherAcomodacao(Acomodacao.REDE)
        viewModel.escolherTipo(TipoPassagem.GRATUIDADE)
        viewModel.escolherGratuidade(TipoGratuidade.IDOSO)
        viewModel.avancar()
        viewModel.preencherPessoa(0, ana)
        viewModel.avancar()
        viewModel.avancar()
        advanceUntilIdle()

        assertEquals(1, passagens.emitidas.size)
    }

    /** Outra categoria de gratuidade tem cota própria. */
    @Test
    fun `cota e por categoria de gratuidade`() = runTest {
        val passagens = FakePassagemRepository().apply {
            passagens = listOf(
                gratuidadeJaEmitida("p-1", TipoGratuidade.IDOSO),
                gratuidadeJaEmitida("p-2", TipoGratuidade.IDOSO),
            )
        }
        val viewModel = vm(passagens)

        viewModel.escolherCategoria(CategoriaPassagem.PASSAGEIRO)
        viewModel.escolherAcomodacao(Acomodacao.REDE)
        viewModel.escolherTipo(TipoPassagem.GRATUIDADE)
        viewModel.escolherGratuidade(TipoGratuidade.PCD)
        viewModel.avancar()
        viewModel.preencherPessoa(0, ana)
        viewModel.avancar()
        viewModel.avancar()
        advanceUntilIdle()

        assertEquals(1, passagens.emitidas.size)
    }

    // --- Tolerância a falha ---

    /** Sem vínculo não há agência a assinar nem dono a carimbar — e quem emite é da operação. */
    @Test
    fun `sem vinculo, a emissao falha e o atendimento fica intacto`() = runTest {
        val passagens = FakePassagemRepository()
        val viewModel = vm(passagens, sessao = FakeSessaoUsuario())
        val eventos = mutableListOf<EventoDeEmissao>()
        val coleta = launch { viewModel.eventos.collect { eventos += it } }

        viewModel.atePagamentoEmRede()
        viewModel.preencherPagamento(pagamentoEmDinheiro())
        viewModel.avancar()
        advanceUntilIdle()

        assertEquals(EventoDeEmissao.Falhou(MotivoDeFalha.SEM_VINCULO), eventos.single())
        assertTrue(passagens.emitidas.isEmpty())
        // O atendimento inteiro permanece: é o que "tolerar falha" significa (ADR-0028 D3).
        val pessoas = (viewModel.uiState.value.participante as ParticipanteEmEdicao.DePassageiro).pessoas
        assertEquals("Ana Ribeiro", pessoas.single().nome)
        assertEquals(PassoEmissao.PAGAMENTO, viewModel.uiState.value.passo)
        assertTrue(!viewModel.uiState.value.emitindo)
        coleta.cancel()
    }

    /**
     * O pool vem **antes** do número de propósito: falhar ao registrar o participante não pode ter consumido
     * um número da sequência daquela saída.
     */
    @Test
    fun `falha no pool nao consome numero`() = runTest {
        val passagens = FakePassagemRepository().apply { proximoNumero = 7 }
        val clientes = FakeClienteRepository().apply { falharAoCriar = true }
        val viewModel = vm(passagens, clientes)
        val eventos = mutableListOf<EventoDeEmissao>()
        val coleta = launch { viewModel.eventos.collect { eventos += it } }

        viewModel.atePagamentoEmRede()
        viewModel.preencherPagamento(pagamentoEmDinheiro())
        viewModel.avancar()
        advanceUntilIdle()

        assertEquals(EventoDeEmissao.Falhou(MotivoDeFalha.POOL_INDISPONIVEL), eventos.single())
        assertEquals(7, passagens.proximoNumero)
        assertTrue(passagens.emitidas.isEmpty())
        coleta.cancel()
    }

    private fun gratuidadeJaEmitida(
        id: String,
        gratuidade: TipoGratuidade,
        status: StatusPassagem = StatusPassagem.EMITIDA,
    ) = PassagemDePassageiro(
        id = id,
        numero = id,
        ocorrencia = ocorrencia,
        lancamentos = emptyList(),
        metadados = MetadadosPassagem(
            status = status,
            funcionarioId = "func-x",
            agenciaId = "empresa-9",
            criadoEm = "2026-08-13T08:00:00",
            alteradoEm = "2026-08-13T08:00:00",
        ),
        acomodacao = Acomodacao.REDE,
        tipo = TipoPassagem.GRATUIDADE,
        gratuidade = gratuidade,
        clientes = listOf("CPF:00000000000"),
    )
}