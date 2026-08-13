package dev.matheus.fluviapp.ui.viewmodel.passagem

import dev.matheus.fluviapp.domain.cliente.Cliente
import dev.matheus.fluviapp.domain.documento.TipoDocumento
import dev.matheus.fluviapp.domain.localidade.Localidade
import dev.matheus.fluviapp.domain.localidade.Uf
import dev.matheus.fluviapp.domain.passagem.Acomodacao
import dev.matheus.fluviapp.domain.passagem.MetadadosPassagem
import dev.matheus.fluviapp.domain.passagem.PassagemDePassageiro
import dev.matheus.fluviapp.domain.passagem.ResultadoEmbarque
import dev.matheus.fluviapp.domain.passagem.StatusPassagem
import dev.matheus.fluviapp.domain.passagem.TipoPassagem
import dev.matheus.fluviapp.domain.porto.Porto
import dev.matheus.fluviapp.domain.rota.Rota
import dev.matheus.fluviapp.domain.viagem.OcorrenciaViagem
import dev.matheus.fluviapp.domain.viagem.Viagem
import dev.matheus.fluviapp.fakes.FakeClienteRepository
import dev.matheus.fluviapp.fakes.FakeEmbarcacaoRepository
import dev.matheus.fluviapp.fakes.FakeLocalidadeRepository
import dev.matheus.fluviapp.fakes.FakePassagemRepository
import dev.matheus.fluviapp.fakes.FakePortoRepository
import dev.matheus.fluviapp.fakes.FakeRotaRepository
import dev.matheus.fluviapp.fakes.FakeSessaoUsuario
import dev.matheus.fluviapp.fakes.FakeVeiculoRepository
import dev.matheus.fluviapp.fakes.FakeViagemRepository
import dev.matheus.fluviapp.revitalizacao.ForaDoEscopo
import dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.ColetorDeReferencias
import dev.matheus.fluviapp.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * **O primeiro teste de ViewModel de passagem** ([ADR-0025] D1) — e o fato de ele ser o primeiro é o achado
 * que o estudo da camada registrou: a passagem era a única entidade sem porta, com a classe concreta injetada
 * em dez lugares, e sem porta não há fake. A ausência de teste não era desleixo; era consequência de forma.
 *
 * O que estes casos cobrem é o fluxo inteiro do embarque em JVM: ler o QR, **coletar** as referências,
 * **traduzir** para a projeção, confirmar (ou ser barrado pela FSM) e reiniciar.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Category(ForaDoEscopo::class)
class EmbarqueViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val ocorrencia = OcorrenciaViagem(viagemId = "viagem-1", data = LocalDate.of(2026, 8, 18))

    private val ana = Cliente(
        id = "CPF:52998224725",
        nome = "Ana Ribeiro",
        tipoDocumento = TipoDocumento.CPF,
        numeroDocumento = "52998224725",
        dataNascimento = LocalDate.of(1996, 1, 30),
        agenciaIds = setOf("empresa-1"),
    )

    private fun passagem(
        id: String = "pas-1",
        status: StatusPassagem = StatusPassagem.EMITIDA,
        clientes: List<String> = listOf(ana.id),
    ) = PassagemDePassageiro(
        id = id,
        numero = "12",
        ocorrencia = ocorrencia,
        lancamentos = emptyList(),
        metadados = MetadadosPassagem(
            status = status,
            funcionarioId = "func-1",
            agenciaId = "empresa-1",
            criadoEm = "2026-08-13T09:00:00",
            alteradoEm = "2026-08-13T09:00:00",
        ),
        acomodacao = Acomodacao.SUITE,
        tipo = TipoPassagem.INTEIRA,
        clientes = clientes,
    )

    /** O cenário completo: a viagem existe, a rota existe e os dois portos resolvem. */
    private fun coletor(
        clientes: FakeClienteRepository = FakeClienteRepository().apply { this.clientes = listOf(ana) },
        comViagem: Boolean = true,
    ) = ColetorDeReferencias(
        clienteRepository = clientes,
        veiculoRepository = FakeVeiculoRepository(),
        viagemRepository = FakeViagemRepository().apply {
            if (comViagem) {
                viagens = listOf(
                    Viagem(
                        id = "viagem-1",
                        rotaId = "rota-1",
                        embarcacaoId = "emb-1",
                        diaSemana = DayOfWeek.TUESDAY,
                        horaMin = 1080,
                    ),
                )
            }
        },
        rotaRepository = FakeRotaRepository().apply {
            rotas = listOf(
                Rota(
                    id = "rota-1",
                    portoOrigemId = "porto-1",
                    portoDestinoId = "porto-2",
                    distanciaMn = 420.0,
                    tempoMedioH = 30.0,
                ),
            )
        },
        portoRepository = FakePortoRepository().apply {
            portos = listOf(
                Porto(id = "porto-1", nome = "Porto de Val-de-Cães", localidadeId = "loc-1"),
                Porto(id = "porto-2", nome = "Porto de Parintins", localidadeId = "loc-2"),
            )
        },
        localidadeRepository = FakeLocalidadeRepository().apply {
            localidades = listOf(
                Localidade(id = "loc-1", municipio = "Belém", uf = Uf.PA, codigoIbge = "1501402"),
                Localidade(id = "loc-2", municipio = "Parintins", uf = Uf.AM, codigoIbge = "1303205"),
            )
        },
        // A embarcação só o **cabeçalho da emissão** usa; a conferência de embarque não a mostra.
        embarcacaoRepository = FakeEmbarcacaoRepository(),
    )

    private fun vm(
        repo: FakePassagemRepository,
        coletor: ColetorDeReferencias = coletor(),
        sessao: FakeSessaoUsuario = FakeSessaoUsuario.supervisor(),
    ) = EmbarqueViewModel(repo, coletor, sessao)

    // --- Ler o QR: coletar e traduzir ---

    @Test
    fun `ao ler o QR, a conferencia chega com a travessia resolvida`() = runTest {
        val repo = FakePassagemRepository().apply { passagens = listOf(passagem()) }
        val viewModel = vm(repo)

        viewModel.aoLerQr("pas-1")
        advanceUntilIdle()

        val conferencia = viewModel.uiState.value.conferencia!!
        assertEquals("#12", conferencia.numero)
        assertEquals("Suíte", conferencia.bilhete)
        assertEquals("Porto de Val-de-Cães · Belém/PA → Porto de Parintins · Parintins/AM", conferencia.travessia)
        assertEquals("Terça-feira, 18/08 · 18:00", conferencia.partida)
        assertEquals("EMITIDA", conferencia.status)
    }

    /**
     * **O embarque confere bilhete e não pessoa** (analista, 2026-08-13), e este caso é o que trava a
     * decisão: o fluxo não pode nem *tentar* ler o pool. Se um dia alguém trocar `daTravessia` por
     * `completas`, este teste cai — e é o que se quer, porque a mudança devolveria ao embarque uma leitura
     * de dado pessoal que ele não precisa fazer.
     */
    @Test
    fun `o embarque nao le o pool de clientes`() = runTest {
        val pool = FakeClienteRepository().apply { clientes = listOf(ana) }
        val repo = FakePassagemRepository().apply { passagens = listOf(passagem()) }
        val viewModel = vm(repo, coletor(clientes = pool))

        viewModel.aoLerQr("pas-1")
        advanceUntilIdle()

        assertEquals(0, pool.leiturasPorIds)
        assertEquals("Suíte", viewModel.uiState.value.conferencia?.bilhete)
    }

    /** Meia e gratuidade aparecem porque é o que se confere; "Inteira" seria ruído em todo bilhete. */
    @Test
    fun `o tipo tarifario so aparece quando nao e inteira`() = runTest {
        val repo = FakePassagemRepository().apply {
            passagens = listOf(passagem().copy(acomodacao = Acomodacao.REDE, tipo = TipoPassagem.MEIA))
        }
        val viewModel = vm(repo)

        viewModel.aoLerQr("pas-1")
        advanceUntilIdle()

        assertEquals("Rede · Meia", viewModel.uiState.value.conferencia?.bilhete)
    }

    /** Viagem inativada continua tendo bilhete apontando para ela: degradação, não erro. */
    @Test
    fun `sem a viagem, a partida fica so com a data e a travessia vazia`() = runTest {
        val repo = FakePassagemRepository().apply { passagens = listOf(passagem()) }
        val viewModel = vm(repo, coletor(comViagem = false))

        viewModel.aoLerQr("pas-1")
        advanceUntilIdle()

        val conferencia = viewModel.uiState.value.conferencia!!
        assertEquals("Terça-feira, 18/08", conferencia.partida)
        assertEquals("", conferencia.travessia)
        assertEquals("Suíte", conferencia.bilhete)
    }

    @Test
    fun `QR de bilhete inexistente vira NaoEncontrada`() = runTest {
        val viewModel = vm(FakePassagemRepository())

        viewModel.aoLerQr("nao-existe")
        advanceUntilIdle()

        assertEquals(ResultadoEmbarque.NaoEncontrada, viewModel.uiState.value.resultado)
        assertNull(viewModel.uiState.value.conferencia)
    }

    @Test
    fun `falha de rede ao resolver o QR nao derruba a tela`() = runTest {
        val repo = FakePassagemRepository().apply {
            passagens = listOf(passagem())
            falharAoLerDoServidor = true
        }
        val viewModel = vm(repo)

        viewModel.aoLerQr("pas-1")
        advanceUntilIdle()

        assertEquals(ResultadoEmbarque.NaoEncontrada, viewModel.uiState.value.resultado)
    }

    /** A câmera dispara o mesmo QR em vários frames: só o primeiro conta. */
    @Test
    fun `leitura repetida enquanto processa e ignorada`() = runTest {
        val repo = FakePassagemRepository().apply { passagens = listOf(passagem()) }
        val viewModel = vm(repo)

        viewModel.aoLerQr("pas-1")
        viewModel.aoLerQr("pas-1")
        advanceUntilIdle()

        assertEquals("Suíte", viewModel.uiState.value.conferencia?.bilhete)
    }

    // --- Confirmar: a FSM e o carimbo ---

    @Test
    fun `confirmar embarca e carimba o uid de quem leu o QR`() = runTest {
        val repo = FakePassagemRepository().apply { passagens = listOf(passagem()) }
        val sessao = FakeSessaoUsuario.supervisor()
        val viewModel = vm(repo, sessao = sessao)

        viewModel.aoLerQr("pas-1")
        advanceUntilIdle()
        viewModel.confirmarEmbarque()
        advanceUntilIdle()

        val resultado = viewModel.uiState.value.resultado
        assertTrue(resultado is ResultadoEmbarque.Confirmada)
        val embarcada = (resultado as ResultadoEmbarque.Confirmada).passagem
        assertEquals(StatusPassagem.EMBARCADA, embarcada.metadados.status)
        assertEquals(sessao.contexto!!.usuario.id, embarcada.metadados.embarque?.porId)
    }

    /** Reuso barrado: o bilhete já embarcado não embarca de novo, e o carimbo antigo é o que se mostra. */
    @Test
    fun `bilhete ja embarcado volta como JaEmbarcada`() = runTest {
        val repo = FakePassagemRepository().apply { passagens = listOf(passagem()) }
        val viewModel = vm(repo)

        viewModel.aoLerQr("pas-1")
        advanceUntilIdle()
        viewModel.confirmarEmbarque()
        advanceUntilIdle()
        viewModel.reiniciar()

        viewModel.aoLerQr("pas-1")
        advanceUntilIdle()
        viewModel.confirmarEmbarque()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.resultado is ResultadoEmbarque.JaEmbarcada)
    }

    @Test
    fun `bilhete ainda A_EMITIR nao embarca`() = runTest {
        val repo = FakePassagemRepository().apply {
            passagens = listOf(passagem(status = StatusPassagem.A_EMITIR))
        }
        val viewModel = vm(repo)

        viewModel.aoLerQr("pas-1")
        advanceUntilIdle()
        viewModel.confirmarEmbarque()
        advanceUntilIdle()

        assertEquals(ResultadoEmbarque.NaoEmitida, viewModel.uiState.value.resultado)
    }

    /** Cancelada é terminal: não há aresta para EMBARCADA. */
    @Test
    fun `bilhete cancelado nao embarca`() = runTest {
        val repo = FakePassagemRepository().apply {
            passagens = listOf(passagem(status = StatusPassagem.CANCELADA))
        }
        val viewModel = vm(repo)

        viewModel.aoLerQr("pas-1")
        advanceUntilIdle()
        viewModel.confirmarEmbarque()
        advanceUntilIdle()

        assertEquals(ResultadoEmbarque.NaoEmitida, viewModel.uiState.value.resultado)
    }

    /** Sem sessão não há uid a carimbar — e carimbo é auditoria, então não se inventa autor. */
    @Test
    fun `sem contexto de usuario, o embarque nao acontece`() = runTest {
        val repo = FakePassagemRepository().apply { passagens = listOf(passagem()) }
        val viewModel = vm(repo, sessao = FakeSessaoUsuario())

        viewModel.aoLerQr("pas-1")
        advanceUntilIdle()
        viewModel.confirmarEmbarque()
        advanceUntilIdle()

        assertEquals(ResultadoEmbarque.NaoEncontrada, viewModel.uiState.value.resultado)
        assertEquals(StatusPassagem.EMITIDA, repo.passagens.single().metadados.status)
    }

    @Test
    fun `reiniciar volta para o escaneamento`() = runTest {
        val repo = FakePassagemRepository().apply { passagens = listOf(passagem()) }
        val viewModel = vm(repo)

        viewModel.aoLerQr("pas-1")
        advanceUntilIdle()
        viewModel.reiniciar()

        assertTrue(viewModel.uiState.value.escaneando)
        assertNull(viewModel.uiState.value.conferencia)
        assertNull(viewModel.uiState.value.passagem)
    }
}