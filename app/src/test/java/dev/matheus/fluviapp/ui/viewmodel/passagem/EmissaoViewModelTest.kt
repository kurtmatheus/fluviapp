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
import dev.matheus.fluviapp.fakes.FakeEmbarcacaoRepository
import dev.matheus.fluviapp.fakes.FakeLocalidadeRepository
import dev.matheus.fluviapp.fakes.FakePassagemRepository
import dev.matheus.fluviapp.fakes.FakePortoRepository
import dev.matheus.fluviapp.fakes.FakeRelogio
import dev.matheus.fluviapp.fakes.FakeRotaRepository
import dev.matheus.fluviapp.fakes.FakeSessaoUsuario
import dev.matheus.fluviapp.fakes.FakeVeiculoRepository
import dev.matheus.fluviapp.fakes.FakeViagemRepository
import dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.ColetorDeReferencias
import dev.matheus.fluviapp.revitalizacao.ForaDoEscopo
import dev.matheus.fluviapp.ui.states.passagem.CabecalhoDaViagem
import dev.matheus.fluviapp.ui.states.passagem.ClienteEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.ErroDeEmissao
import dev.matheus.fluviapp.ui.states.passagem.LancamentoEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.PagamentoEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.ParticipanteEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.PassoDaEmissao
import dev.matheus.fluviapp.ui.states.passagem.VeiculoEmEdicao
import dev.matheus.fluviapp.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
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
 * **A emissão como totem** ([ADR-0029]): passos pequenos, orientados a evento, e um roteiro que as escolhas
 * desenham.
 *
 * O que estes casos cobrem é a sequência que o ViewModel orquestra e, principalmente, **onde ela para**:
 * cada falha tem de deixar o atendimento intacto no passo em que está — digitar tudo de novo por causa de
 * rede é o que a tolerância a falha do [ADR-0028] D3 proíbe.
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
        // O coletor só resolve o **cabeçalho**; com repositórios vazios ele devolve tudo em branco, que é
        // exatamente o cenário de "vender mesmo sem conseguir resolver a saída" — a fila não para por isso.
        coletorDeReferencias = ColetorDeReferencias(
            clienteRepository = clientes,
            veiculoRepository = veiculos,
            viagemRepository = FakeViagemRepository(),
            rotaRepository = FakeRotaRepository(),
            portoRepository = FakePortoRepository(),
            localidadeRepository = FakeLocalidadeRepository(),
            embarcacaoRepository = FakeEmbarcacaoRepository(),
        ),
        sessaoUsuario = sessao,
        relogio = FakeRelogio(LocalDateTime.of(2026, 8, 13, 9, 30)),
    ).also { it.iniciar(ocorrencia, CabecalhoDaViagem(travessia = "A → B", partida = "Terça, 18/08 · 18:00")) }

    /**
     * No totem, um toque **escolhe e avança** — a tela chama os dois. Aqui isso fica explícito para que o
     * teste exercite a mesma sequência que o dedo do operador produz.
     */
    private fun TestScope.ateOPagamentoEmRede(viewModel: EmissaoViewModel, pessoa: ClienteEmEdicao = ana) {
        viewModel.escolherCategoria(CategoriaPassagem.PASSAGEIRO)
        viewModel.avancar()
        viewModel.escolherAcomodacao(Acomodacao.REDE)
        viewModel.avancar()
        viewModel.escolherTipo(TipoPassagem.INTEIRA)
        viewModel.avancar()
        viewModel.preencherPessoa(0, pessoa)
        viewModel.avancar()
        advanceUntilIdle()
    }

    private fun pagamentoEmDinheiro(valor: String = "150,00") =
        PagamentoEmEdicao(lancamentos = listOf(LancamentoEmEdicao(FormaPagamento.DINHEIRO, valor)))

    /**
     * O "Emitir" do passo 5 **abre a conferência**; quem emite é o gesto seguinte. Os dois estão juntos aqui
     * porque é o que o operador faz — mas separados no ViewModel, que é o que permite voltar e corrigir.
     */
    private fun TestScope.confirmarEEmitir(viewModel: EmissaoViewModel) {
        viewModel.avancar()
        advanceUntilIdle()
        viewModel.confirmarEmissao()
        advanceUntilIdle()
    }

    // --- O roteiro em movimento ---

    @Test
    fun `sem escolher acomodacao, o roteiro nao passa do passo dela`() = runTest {
        val viewModel = vm()

        viewModel.escolherCategoria(CategoriaPassagem.PASSAGEIRO)
        viewModel.avancar()
        viewModel.avancar()

        assertEquals(PassoDaEmissao.EscolhaDeAcomodacao, viewModel.uiState.value.passo)
    }

    @Test
    fun `o veiculo vai direto para a classe, sem acomodacao`() = runTest {
        val viewModel = vm()

        viewModel.escolherCategoria(CategoriaPassagem.VEICULO)
        viewModel.avancar()

        assertEquals(PassoDaEmissao.ClasseDoVeiculo, viewModel.uiState.value.passo)
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

    /** Fora da rede só existe inteira: manter "meia" escolhida antes seria guardar estado ilegal. */
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

    /** A quantidade do passo 3.2 **desenha o passo 4**: três pessoas são três formulários. */
    @Test
    fun `escolher tres pessoas acrescenta dois formularios ao roteiro`() = runTest {
        val viewModel = vm()

        viewModel.escolherCategoria(CategoriaPassagem.PASSAGEIRO)
        viewModel.escolherAcomodacao(Acomodacao.CAMAROTE)
        viewModel.escolherQuantidadeDePessoas(3)

        val formularios = viewModel.uiState.value.roteiro.filterIsInstance<PassoDaEmissao.DadosDoCliente>()
        assertEquals(3, formularios.size)
        assertEquals(6 + 2, viewModel.uiState.value.totalDePassos)
    }

    /** Reduzir descarta o que não cabe — melhor do que carregar em silêncio quem o bilhete não admite. */
    @Test
    fun `reduzir a quantidade descarta as pessoas que sobram`() = runTest {
        val viewModel = vm()

        viewModel.escolherCategoria(CategoriaPassagem.PASSAGEIRO)
        viewModel.escolherAcomodacao(Acomodacao.SUITE)
        viewModel.escolherQuantidadeDePessoas(2)
        viewModel.preencherPessoa(0, ana)
        viewModel.preencherPessoa(1, bruno)
        viewModel.escolherQuantidadeDePessoas(1)

        val pessoas = (viewModel.uiState.value.participante as ParticipanteEmEdicao.DePassageiro).pessoas
        assertEquals(listOf("Ana Ribeiro"), pessoas.map { it.nome })
    }

    @Test
    fun `sem titular completo, o passo do cliente nao avanca`() = runTest {
        val viewModel = vm()

        viewModel.escolherCategoria(CategoriaPassagem.PASSAGEIRO)
        viewModel.avancar()
        viewModel.escolherAcomodacao(Acomodacao.REDE)
        viewModel.avancar()
        viewModel.escolherTipo(TipoPassagem.INTEIRA)
        viewModel.avancar()
        viewModel.avancar()
        advanceUntilIdle()

        assertEquals(PassoDaEmissao.DadosDoCliente(0), viewModel.uiState.value.passo)
        assertEquals(setOf(ErroDeEmissao.TITULAR_INCOMPLETO), viewModel.uiState.value.erros)
    }

    /** Quem volta está corrigindo: cobrar o passo na saída seria prendê-lo nele. */
    @Test
    fun `voltar nao valida`() = runTest {
        val viewModel = vm()

        viewModel.escolherCategoria(CategoriaPassagem.PASSAGEIRO)
        viewModel.avancar()
        viewModel.escolherAcomodacao(Acomodacao.REDE)
        viewModel.avancar()
        viewModel.voltar()

        assertEquals(PassoDaEmissao.EscolhaDeAcomodacao, viewModel.uiState.value.passo)
        assertTrue(viewModel.uiState.value.erros.isEmpty())
    }

    // --- O cliente é salvo no passo dele (ADR-0029 D4) ---

    @Test
    fun `cada pessoa entra no pool no passo dela`() = runTest {
        val clientes = FakeClienteRepository()
        val viewModel = vm(clientes = clientes)

        ateOPagamentoEmRede(viewModel)

        assertEquals(1, clientes.criados.size)
        assertEquals(PassoDaEmissao.Pagamento, viewModel.uiState.value.passo)
        val pessoas = (viewModel.uiState.value.participante as ParticipanteEmEdicao.DePassageiro).pessoas
        // O id volta para o formulário: reentrar no passo assina em vez de criar (idempotente).
        assertEquals("CPF:52998224725", pessoas.single().idExistente)
    }

    /**
     * A falha da operação que exige rede acontece **no passo da pessoa**, e não no fim: o operador vê qual
     * não subiu, com o que acabou de digitar na tela.
     */
    @Test
    fun `falha do pool interrompe no passo do cliente, com o formulario intacto`() = runTest {
        val clientes = FakeClienteRepository().apply { falharAoCriar = true }
        val passagens = FakePassagemRepository().apply { proximoNumero = 7 }
        val viewModel = vm(passagens, clientes)
        val eventos = mutableListOf<EventoDeEmissao>()
        val coleta = launch { viewModel.eventos.collect { eventos += it } }

        ateOPagamentoEmRede(viewModel)

        assertEquals(EventoDeEmissao.Falhou(MotivoDeFalha.POOL_INDISPONIVEL), eventos.single())
        assertEquals(PassoDaEmissao.DadosDoCliente(0), viewModel.uiState.value.passo)
        val pessoas = (viewModel.uiState.value.participante as ParticipanteEmEdicao.DePassageiro).pessoas
        assertEquals("Ana Ribeiro", pessoas.single().nome)
        // Nada além do pool aconteceu: o número da saída continua intocado.
        assertEquals(7, passagens.proximoNumero)
        coleta.cancel()
    }

    // --- O detalhamento que precede a emissão (conferência) ---

    /** O "Emitir" do passo 5 **não emite**: abre a conferência. Quem emite é o gesto seguinte. */
    @Test
    fun `o pagamento abre a conferencia em vez de emitir`() = runTest {
        val passagens = FakePassagemRepository()
        val viewModel = vm(passagens)

        ateOPagamentoEmRede(viewModel)
        viewModel.preencherPagamento(pagamentoEmDinheiro())
        viewModel.avancar()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.confirmacao != null)
        assertTrue(passagens.emitidas.isEmpty())
        // A conferência não é passo: o roteiro não andou, e a trilha continua no pagamento.
        assertEquals(PassoDaEmissao.Pagamento, viewModel.uiState.value.passo)
    }

    @Test
    fun `a conferencia mostra o que foi digitado`() = runTest {
        val viewModel = vm()

        ateOPagamentoEmRede(viewModel)
        viewModel.preencherPagamento(pagamentoEmDinheiro())
        viewModel.avancar()
        advanceUntilIdle()

        val confirmacao = viewModel.uiState.value.confirmacao!!
        assertEquals("Rede", confirmacao.bilhete)
        assertEquals("Ana Ribeiro", confirmacao.pessoas.single().nome)
        // O documento sai **formatado pelo tipo**: é assim que a pessoa o lê no cartão que tem na mão.
        assertEquals("CPF 529.982.247-25", confirmacao.pessoas.single().documento)
        assertEquals("Dinheiro", confirmacao.lancamentos.single().forma)
        assertTrue(confirmacao.total.contains("150"))
    }

    /** Gratuidade não tem lançamento, e a **ausência precisa dizer isso** — não parecer pagamento perdido. */
    @Test
    fun `a conferencia de gratuidade nao tem lancamento`() = runTest {
        val viewModel = vm()

        viewModel.escolherCategoria(CategoriaPassagem.PASSAGEIRO)
        viewModel.avancar()
        viewModel.escolherAcomodacao(Acomodacao.REDE)
        viewModel.avancar()
        viewModel.escolherTipo(TipoPassagem.GRATUIDADE)
        viewModel.avancar()
        viewModel.escolherGratuidade(TipoGratuidade.IDOSO)
        viewModel.avancar()
        viewModel.preencherPessoa(0, ana)
        viewModel.avancar()
        advanceUntilIdle()
        viewModel.avancar()
        advanceUntilIdle()

        val confirmacao = viewModel.uiState.value.confirmacao!!
        assertTrue(confirmacao.lancamentos.isEmpty())
        assertEquals("Rede · Gratuidade · Idoso", confirmacao.bilhete)
    }

    /** Corrigir fecha a conferência **sem emitir**, e o atendimento continua inteiro onde estava. */
    @Test
    fun `corrigir volta para o pagamento sem emitir`() = runTest {
        val passagens = FakePassagemRepository()
        val viewModel = vm(passagens)

        ateOPagamentoEmRede(viewModel)
        viewModel.preencherPagamento(pagamentoEmDinheiro())
        viewModel.avancar()
        advanceUntilIdle()
        viewModel.revisar()

        assertNull(viewModel.uiState.value.confirmacao)
        assertTrue(passagens.emitidas.isEmpty())
        assertEquals(PassoDaEmissao.Pagamento, viewModel.uiState.value.passo)
        assertEquals("150,00", viewModel.uiState.value.pagamento.lancamentos.single().valor)
    }

    // --- A emissão ---

    @Test
    fun `emitir reserva o numero, cria a passagem e leva ao desfecho`() = runTest {
        val passagens = FakePassagemRepository().apply { proximoNumero = 41 }
        val viewModel = vm(passagens)
        val eventos = mutableListOf<EventoDeEmissao>()
        val coleta = launch { viewModel.eventos.collect { eventos += it } }

        ateOPagamentoEmRede(viewModel)
        viewModel.preencherPagamento(pagamentoEmDinheiro())
        confirmarEEmitir(viewModel)

        val emitida = passagens.emitidas.single() as PassagemDePassageiro
        assertEquals("41", emitida.numero)
        assertEquals(StatusPassagem.EMITIDA, emitida.metadados.status)
        assertEquals(ocorrencia, emitida.ocorrencia)
        assertEquals(BigDecimal("150.00"), emitida.lancamentos.single().valor)
        assertEquals(listOf("CPF:52998224725"), emitida.clientes)
        assertTrue(eventos.single() is EventoDeEmissao.Emitida)
        // O roteiro anda para o desfecho, que é onde o bilhete se entrega.
        assertEquals(PassoDaEmissao.Desfecho, viewModel.uiState.value.passo)
        assertEquals(emitida.id, viewModel.uiState.value.idEmitida)
        coleta.cancel()
    }

    @Test
    fun `veiculo emite com o responsavel pulado`() = runTest {
        val passagens = FakePassagemRepository()
        val veiculos = FakeVeiculoRepository()
        val clientes = FakeClienteRepository()
        val viewModel = vm(passagens, clientes, veiculos)

        viewModel.escolherCategoria(CategoriaPassagem.VEICULO)
        viewModel.avancar()
        viewModel.escolherClasseDeVeiculo(ClasseVeiculo.MOTO)
        viewModel.avancar()
        viewModel.preencherVeiculo(
            VeiculoEmEdicao(placa = "ABC1D23", classe = ClasseVeiculo.MOTO, modelo = "Fan", cilindrada = "150"),
        )
        viewModel.avancar()
        // O passo 4 é o responsável, e ele é opcional: bilhete de veículo sem ninguém nomeado é o normal.
        viewModel.pular()
        viewModel.preencherPagamento(pagamentoEmDinheiro("80,00"))
        confirmarEEmitir(viewModel)

        val emitida = passagens.emitidas.single() as PassagemDeVeiculo
        assertEquals("ABC1D23", emitida.veiculoId)
        assertNull(emitida.responsavelRetirada)
        assertEquals(1, veiculos.criados.size)
        assertTrue(clientes.criados.isEmpty())
    }

    /** Gratuidade é tarifa zero por lei, não pagamento de zero: o passo 5 não cobra dinheiro dela. */
    @Test
    fun `gratuidade emite sem lancamento nenhum`() = runTest {
        val passagens = FakePassagemRepository()
        val viewModel = vm(passagens)

        viewModel.escolherCategoria(CategoriaPassagem.PASSAGEIRO)
        viewModel.avancar()
        viewModel.escolherAcomodacao(Acomodacao.REDE)
        viewModel.avancar()
        viewModel.escolherTipo(TipoPassagem.GRATUIDADE)
        viewModel.avancar()
        viewModel.escolherGratuidade(TipoGratuidade.IDOSO)
        viewModel.avancar()
        viewModel.preencherPessoa(0, ana)
        viewModel.avancar()
        advanceUntilIdle()
        confirmarEEmitir(viewModel)

        val emitida = passagens.emitidas.single() as PassagemDePassageiro
        assertTrue(emitida.lancamentos.isEmpty())
        assertEquals(TipoGratuidade.IDOSO, emitida.gratuidade)
    }

    // --- As guardas ---

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

        emitirGratuidade(viewModel, TipoGratuidade.IDOSO)

        val bloqueio = eventos.single() as EventoDeEmissao.Bloqueada
        assertEquals(ResultadoEmissao.CotaGratuidadeAtingida(TipoGratuidade.IDOSO), bloqueio.motivo)
        assertTrue(passagens.emitidas.isEmpty())
        // Bloqueado não é emitido: o roteiro não anda para o desfecho.
        assertEquals(PassoDaEmissao.Pagamento, viewModel.uiState.value.passo)
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

        emitirGratuidade(viewModel, TipoGratuidade.IDOSO)

        assertEquals(1, passagens.emitidas.size)
    }

    @Test
    fun `cota e por categoria de gratuidade`() = runTest {
        val passagens = FakePassagemRepository().apply {
            passagens = listOf(
                gratuidadeJaEmitida("p-1", TipoGratuidade.IDOSO),
                gratuidadeJaEmitida("p-2", TipoGratuidade.IDOSO),
            )
        }
        val viewModel = vm(passagens)

        emitirGratuidade(viewModel, TipoGratuidade.PCD)

        assertEquals(1, passagens.emitidas.size)
    }

    // --- Tolerância a falha ---

    @Test
    fun `sem vinculo, a emissao falha e o atendimento fica intacto`() = runTest {
        val passagens = FakePassagemRepository()
        val viewModel = vm(passagens, sessao = FakeSessaoUsuario())
        val eventos = mutableListOf<EventoDeEmissao>()
        val coleta = launch { viewModel.eventos.collect { eventos += it } }

        ateOPagamentoEmRede(viewModel)

        // Falha já no passo do cliente, porque é lá que o vínculo passou a ser exigido.
        assertEquals(EventoDeEmissao.Falhou(MotivoDeFalha.SEM_VINCULO), eventos.single())
        assertTrue(passagens.emitidas.isEmpty())
        val pessoas = (viewModel.uiState.value.participante as ParticipanteEmEdicao.DePassageiro).pessoas
        assertEquals("Ana Ribeiro", pessoas.single().nome)
        assertTrue(!viewModel.uiState.value.emitindo)
        coleta.cancel()
    }

    private fun TestScope.emitirGratuidade(viewModel: EmissaoViewModel, gratuidade: TipoGratuidade) {
        viewModel.escolherCategoria(CategoriaPassagem.PASSAGEIRO)
        viewModel.avancar()
        viewModel.escolherAcomodacao(Acomodacao.REDE)
        viewModel.avancar()
        viewModel.escolherTipo(TipoPassagem.GRATUIDADE)
        viewModel.avancar()
        viewModel.escolherGratuidade(gratuidade)
        viewModel.avancar()
        viewModel.preencherPessoa(0, ana)
        viewModel.avancar()
        advanceUntilIdle()
        confirmarEEmitir(viewModel)
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