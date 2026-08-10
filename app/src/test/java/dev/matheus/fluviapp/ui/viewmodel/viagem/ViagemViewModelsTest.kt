package dev.matheus.fluviapp.ui.viewmodel.viagem

import dev.matheus.fluviapp.domain.localidade.Localidade
import dev.matheus.fluviapp.domain.localidade.Uf
import dev.matheus.fluviapp.domain.porto.Porto
import dev.matheus.fluviapp.domain.rota.Rota
import dev.matheus.fluviapp.domain.viagem.Embarcacao
import dev.matheus.fluviapp.domain.viagem.TipoEmbarcacao
import dev.matheus.fluviapp.domain.viagem.Viagem
import dev.matheus.fluviapp.domain.viagem.rotulo
import dev.matheus.fluviapp.fakes.FakeEmbarcacaoRepository
import dev.matheus.fluviapp.fakes.FakeEscopoDaSessao
import dev.matheus.fluviapp.fakes.FakeLocalidadeRepository
import dev.matheus.fluviapp.fakes.FakePortoRepository
import dev.matheus.fluviapp.fakes.FakeRotaRepository
import dev.matheus.fluviapp.fakes.FakeSessaoUsuario
import dev.matheus.fluviapp.fakes.FakeViagemRepository
import dev.matheus.fluviapp.ui.states.ErroHoraViagem
import dev.matheus.fluviapp.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.DayOfWeek

/**
 * As duas telas da Viagem (F8.2).
 *
 * O que elas têm de próprio no app é o **recorte por concessão** (decisão do analista, 2026-08-10): o
 * painel da empresa mostra o que é da empresa, e o pool sem dono não é exceção a isso. Metade destes
 * casos existe para travar essa fronteira nas duas direções — o que a empresa vê e o que a plataforma
 * continua vendo.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ViagemViewModelsTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val belem = Localidade("loc-belem", "Belém", Uf.PA, "1501402")
    private val parintins = Localidade("loc-parintins", "Parintins", Uf.AM, "1303205")
    private val manaus = Localidade("loc-manaus", "Manaus", Uf.AM, "1302603")

    private val portoBelem = Porto("porto-a", "Porto de Val-de-Cães", "loc-belem")
    private val portoParintins = Porto("porto-b", "Porto de Parintins", "loc-parintins")
    private val portoManaus = Porto("porto-c", "Porto de Manaus", "loc-manaus")

    private val rotaConcedida = Rota("r1", "porto-a", "porto-b", distanciaMn = 420.0, tempoMedioH = 30.0)
    private val rotaDeFora = Rota("r2", "porto-c", "porto-b", distanciaMn = 100.0, tempoMedioH = 6.0)

    private val embarcacaoConcedida = embarcacao("e1", "F/B Modelo")
    private val embarcacaoDeFora = embarcacao("e2", "F/B Alheio")

    private val rotuloRotaConcedida =
        "Porto de Val-de-Cães · Belém/PA → Porto de Parintins · Parintins/AM"

    private fun embarcacao(id: String, nome: String) = Embarcacao(
        id = id,
        descricaoNome = nome,
        tipo = TipoEmbarcacao.FERRY_BOAT,
        capacidadeVeiculo = 60,
        capacidadeSuite2 = 4,
        capacidadeSuite3 = 5,
        capacidadeCamarote = 4,
        empresaId = "empresa-1",
    )

    private fun viagem(
        id: String = "v1",
        rotaId: String = "r1",
        embarcacaoId: String = "e1",
        dia: DayOfWeek = DayOfWeek.TUESDAY,
        horaMin: Int = 18 * 60,
        ativo: Boolean = true,
    ) = Viagem(id, rotaId, embarcacaoId, dia, horaMin, ativo = ativo)

    private fun locais() = FakeLocalidadeRepository().apply {
        localidades = listOf(belem, parintins, manaus)
    }

    private fun portos() = FakePortoRepository().apply {
        portos = listOf(portoBelem, portoParintins, portoManaus)
    }

    private fun rotas(lista: List<Rota> = listOf(rotaConcedida, rotaDeFora)) =
        FakeRotaRepository().apply { rotas = lista }

    private fun embarcacoes(lista: List<Embarcacao> = listOf(embarcacaoConcedida, embarcacaoDeFora)) =
        FakeEmbarcacaoRepository().apply { embarcacoes = lista }

    /** A empresa que recebeu Belém, Parintins e o "F/B Modelo" — e nada mais. */
    private fun concedido() = FakeEscopoDaSessao.concedido(
        portoIds = setOf("porto-a", "porto-b"),
        embarcacaoIds = setOf("e1"),
    )

    private fun formVm(
        viagens: FakeViagemRepository = FakeViagemRepository(),
        rotas: FakeRotaRepository = rotas(),
        embarcacoes: FakeEmbarcacaoRepository = embarcacoes(),
        escopo: FakeEscopoDaSessao = concedido(),
        sessao: FakeSessaoUsuario = FakeSessaoUsuario.supervisor(),
    ) = FormViagemViewModel(viagens, rotas, embarcacoes, portos(), locais(), escopo, sessao)

    private fun buscaVm(
        viagens: FakeViagemRepository,
        rotas: FakeRotaRepository = rotas(),
        escopo: FakeEscopoDaSessao = concedido(),
        sessao: FakeSessaoUsuario = FakeSessaoUsuario.plataforma(),
    ) = PesquisaViagemViewModel(viagens, rotas, embarcacoes(), portos(), locais(), escopo, sessao)

    // --- O formulário: o que ele oferece ---

    /**
     * O núcleo da decisão do analista: **não é validação, é o que existe no dropdown**. A rota de Manaus
     * e o navio alheio não aparecem, então o erro não chega a poder ser cometido.
     */
    @Test
    fun `o formulario so oferece rota e embarcacao concedidas`() = runTest(mainRule.dispatcher) {
        val vm = formVm()
        advanceUntilIdle()

        assertEquals(listOf(rotuloRotaConcedida), vm.uiState.value.rotas.map { it.rotulo })
        assertEquals(listOf("F/B Modelo"), vm.uiState.value.embarcacoes.map { it.rotulo })
    }

    /** Quem cura o pool precisa enxergá-lo inteiro — o que ela não vê, não conserta. */
    @Test
    fun `a plataforma monta viagem sobre o pool inteiro`() = runTest(mainRule.dispatcher) {
        val vm = formVm(escopo = FakeEscopoDaSessao.plataforma())
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.rotas.size)
        assertEquals(2, vm.uiState.value.embarcacoes.size)
        assertFalse(vm.uiState.value.semConcessao)
    }

    /** Rota inativada é registro do passado: criar partida sobre ela seria nascer sobre o encerrado. */
    @Test
    fun `rota inativa nao entra no seletor`() = runTest(mainRule.dispatcher) {
        val vm = formVm(rotas = rotas(listOf(rotaConcedida.copy(ativo = false))))
        advanceUntilIdle()

        assertTrue(vm.uiState.value.rotas.isEmpty())
    }

    /**
     * O preço declarado da decisão: sem concessão, tela vazia — e ela **diz isso**, em vez de mostrar
     * dois dropdowns vazios que pareceriam defeito.
     */
    @Test
    fun `empresa sem concessao ve a mensagem, nao dropdowns vazios`() = runTest(mainRule.dispatcher) {
        val vm = formVm(escopo = FakeEscopoDaSessao.semNada())
        advanceUntilIdle()

        assertTrue(vm.uiState.value.semConcessao)
        assertTrue(vm.uiState.value.rotas.isEmpty())
        assertTrue(vm.uiState.value.embarcacoes.isEmpty())
    }

    /** Um lado vazio já basta: sem navio não há partida, mesmo com a rota concedida. */
    @Test
    fun `rota concedida sem embarcacao concedida tambem e sem concessao`() =
        runTest(mainRule.dispatcher) {
            val vm = formVm(
                escopo = FakeEscopoDaSessao.concedido(
                    portoIds = setOf("porto-a", "porto-b"),
                    embarcacaoIds = emptySet(),
                ),
            )
            advanceUntilIdle()

            assertTrue(vm.uiState.value.semConcessao)
        }

    @Test
    fun `os sete dias sao oferecidos, comecando na segunda`() = runTest(mainRule.dispatcher) {
        val vm = formVm()
        advanceUntilIdle()

        assertEquals(7, vm.uiState.value.diasDaSemana.size)
        assertEquals(DayOfWeek.MONDAY.rotulo, vm.uiState.value.diasDaSemana.first())
        assertEquals(DayOfWeek.SUNDAY.rotulo, vm.uiState.value.diasDaSemana.last())
    }

    // --- O formulário: o que ele grava ---

    @Test
    fun `criar grava ids, dia e minuto — nao rotulos`() = runTest(mainRule.dispatcher) {
        val viagens = FakeViagemRepository()
        val vm = formVm(viagens = viagens)
        advanceUntilIdle()

        vm.onRotaChange(rotuloRotaConcedida)
        vm.onEmbarcacaoChange("F/B Modelo")
        vm.onDiaSemanaChange(DayOfWeek.TUESDAY.rotulo)
        vm.onHoraChange("1800")
        vm.salvar()
        advanceUntilIdle()

        val criada = viagens.criadas.single()
        assertEquals("r1", criada.rotaId)
        assertEquals("e1", criada.embarcacaoId)
        assertEquals(DayOfWeek.TUESDAY, criada.diaSemana)
        assertEquals(18 * 60, criada.horaMin)
    }

    /** Num pool sem dono, a assinatura é o que resta de responsabilidade. */
    @Test
    fun `a viagem nasce assinada por quem a criou`() = runTest(mainRule.dispatcher) {
        val viagens = FakeViagemRepository()
        val vm = formVm(viagens = viagens, sessao = FakeSessaoUsuario.supervisor())
        advanceUntilIdle()

        vm.onRotaChange(rotuloRotaConcedida)
        vm.onEmbarcacaoChange("F/B Modelo")
        vm.onDiaSemanaChange(DayOfWeek.TUESDAY.rotulo)
        vm.onHoraChange("1800")
        vm.salvar()
        advanceUntilIdle()

        assertTrue(viagens.criadas.single().criadoPor.isNotBlank())
        assertTrue(viagens.criadas.single().criadoEm.isNotBlank())
    }

    /** Papel puro de plataforma assina **vazio**: não há funcionário, e inventar um id seria autoria falsa. */
    @Test
    fun `a plataforma assina vazio`() = runTest(mainRule.dispatcher) {
        val viagens = FakeViagemRepository()
        val vm = formVm(
            viagens = viagens,
            escopo = FakeEscopoDaSessao.plataforma(),
            sessao = FakeSessaoUsuario.plataforma(),
        )
        advanceUntilIdle()

        vm.onRotaChange(rotuloRotaConcedida)
        vm.onEmbarcacaoChange("F/B Modelo")
        vm.onDiaSemanaChange(DayOfWeek.TUESDAY.rotulo)
        vm.onHoraChange("1800")
        vm.salvar()
        advanceUntilIdle()

        assertEquals("", viagens.criadas.single().criadoPor)
    }

    @Test
    fun `formulario incompleto nao grava e acusa os campos`() = runTest(mainRule.dispatcher) {
        val viagens = FakeViagemRepository()
        val vm = formVm(viagens = viagens)
        advanceUntilIdle()

        vm.salvar()
        advanceUntilIdle()

        assertTrue(viagens.criadas.isEmpty())
        assertTrue(vm.uiState.value.isRotaError)
        assertTrue(vm.uiState.value.isEmbarcacaoError)
        assertTrue(vm.uiState.value.isDiaSemanaError)
        assertEquals(ErroHoraViagem.OBRIGATORIA, vm.uiState.value.erroHora)
    }

    @Test
    fun `saida ja existente nao grava e acusa a hora`() = runTest(mainRule.dispatcher) {
        val viagens = FakeViagemRepository().apply { this.viagens = listOf(viagem()) }
        val vm = formVm(viagens = viagens)
        advanceUntilIdle()

        vm.onRotaChange(rotuloRotaConcedida)
        vm.onEmbarcacaoChange("F/B Modelo")
        vm.onDiaSemanaChange(DayOfWeek.TUESDAY.rotulo)
        vm.onHoraChange("1800")
        vm.salvar()
        advanceUntilIdle()

        assertTrue(viagens.criadas.isEmpty())
        assertEquals(ErroHoraViagem.DUPLICADA, vm.uiState.value.erroHora)
    }

    /**
     * **O teclado numérico do Android não tem `:`** (achado em homologação, 2026-08-10), e o campo pedia
     * `HH:mm` — não era atrito, era um campo que não se conseguia preencher.
     *
     * O estado guarda **só os dígitos**: o separador é desenhado. A primeira correção o guardava no
     * valor, e foi o que quebrou o cursor — inserir caractere no meio faz o Compose recalcular a seleção
     * sobre o texto anterior.
     */
    @Test
    fun `o estado guarda digito, nao o separador`() = runTest(mainRule.dispatcher) {
        val vm = formVm()
        advanceUntilIdle()

        vm.onHoraChange("1")
        assertEquals("1", vm.uiState.value.horaDigitada)

        vm.onHoraChange("183")
        assertEquals("183", vm.uiState.value.horaDigitada)

        vm.onHoraChange("1830")
        assertEquals("1830", vm.uiState.value.horaDigitada)
    }

    /**
     * **O caso do cursor, visto de onde dá para vê-lo em JVM.** Digitar em duas levas só termina em
     * `1830` se cada dígito entrar no fim; com o separador guardado no valor, o terceiro caía antes do
     * `:` e a hora saía embaralhada.
     */
    @Test
    fun `digitar em duas levas termina no fim, nao no meio`() = runTest(mainRule.dispatcher) {
        val vm = formVm()
        advanceUntilIdle()

        vm.onHoraChange("18")
        vm.onHoraChange(vm.uiState.value.horaDigitada + "30")

        assertEquals("1830", vm.uiState.value.horaDigitada)
    }

    /** E o que o campo guardou é o que a gravação lê — a tradução acontece num lugar só. */
    @Test
    fun `os digitos chegam ao documento como minuto`() = runTest(mainRule.dispatcher) {
        val viagens = FakeViagemRepository()
        val vm = formVm(viagens = viagens)
        advanceUntilIdle()

        vm.onRotaChange(rotuloRotaConcedida)
        vm.onEmbarcacaoChange("F/B Modelo")
        vm.onDiaSemanaChange(DayOfWeek.TUESDAY.rotulo)
        vm.onHoraChange("0605")
        vm.salvar()
        advanceUntilIdle()

        assertEquals("0605", vm.uiState.value.horaDigitada)
        assertEquals(6 * 60 + 5, viagens.criadas.single().horaMin)
    }

    /**
     * Parar em três dígitos **não grava** 18:03 por engano — a hora fica incompleta e a tela acusa.
     * É o par da decisão de `minutosDaHora` exigir dois dígitos de cada lado.
     */
    @Test
    fun `hora incompleta nao grava`() = runTest(mainRule.dispatcher) {
        val viagens = FakeViagemRepository()
        val vm = formVm(viagens = viagens)
        advanceUntilIdle()

        vm.onRotaChange(rotuloRotaConcedida)
        vm.onEmbarcacaoChange("F/B Modelo")
        vm.onDiaSemanaChange(DayOfWeek.TUESDAY.rotulo)
        vm.onHoraChange("183")
        vm.salvar()
        advanceUntilIdle()

        assertTrue(viagens.criadas.isEmpty())
        assertEquals(ErroHoraViagem.INVALIDA, vm.uiState.value.erroHora)
    }

    // --- A busca ---

    @Test
    fun `a empresa so ve as viagens que pode ofertar`() = runTest(mainRule.dispatcher) {
        val viagens = FakeViagemRepository().apply {
            this.viagens = listOf(
                viagem(id = "minha"),
                viagem(id = "rota-alheia", rotaId = "r2"),
                viagem(id = "navio-alheio", embarcacaoId = "e2"),
            )
        }
        val vm = buscaVm(viagens, escopo = concedido())
        advanceUntilIdle()

        assertEquals(listOf("minha"), vm.uiState.value.resultados.map { it.id })
    }

    @Test
    fun `a plataforma ve o pool inteiro`() = runTest(mainRule.dispatcher) {
        val viagens = FakeViagemRepository().apply {
            this.viagens = listOf(viagem(id = "minha"), viagem(id = "alheia", rotaId = "r2"))
        }
        val vm = buscaVm(viagens, escopo = FakeEscopoDaSessao.plataforma())
        advanceUntilIdle()

        assertEquals(listOf("minha", "alheia"), vm.uiState.value.resultados.map { it.id })
    }

    /** O descartado é registro: some-lo esconderia por que um bilhete antigo aponta para onde aponta. */
    @Test
    fun `a inativa fica na lista, marcada`() = runTest(mainRule.dispatcher) {
        val viagens = FakeViagemRepository().apply {
            this.viagens = listOf(viagem(id = "v1", ativo = false))
        }
        val vm = buscaVm(viagens)
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.resultados.size)
        assertFalse(vm.uiState.value.resultados.single().ativa)
    }

    @Test
    fun `a partida traz dia e hora, e a rota traz as duas cidades`() = runTest(mainRule.dispatcher) {
        val viagens = FakeViagemRepository().apply { this.viagens = listOf(viagem()) }
        val vm = buscaVm(viagens)
        advanceUntilIdle()

        val resultado = vm.uiState.value.resultados.single()
        assertEquals("Terça-feira · 18:00", resultado.partida)
        assertEquals(rotuloRotaConcedida, resultado.rota)
        assertEquals("F/B Modelo", resultado.embarcacao)
    }

    /**
     * A travessia de 30h saindo terça 18h chega **quinta** 00:00 — e a lista diz o dia, porque "chega às
     * 00:00" sem ele parece a mesma madrugada da saída.
     */
    @Test
    fun `a chegada mostra o dia quando a travessia o atravessa`() = runTest(mainRule.dispatcher) {
        val viagens = FakeViagemRepository().apply { this.viagens = listOf(viagem()) }
        val vm = buscaVm(viagens)
        advanceUntilIdle()

        assertEquals("Qui 00:00", vm.uiState.value.resultados.single().chegada)
    }

    /** No mesmo dia, o dia seria ruído: só a hora. */
    @Test
    fun `a chegada no mesmo dia mostra so a hora`() = runTest(mainRule.dispatcher) {
        val viagens = FakeViagemRepository().apply {
            this.viagens = listOf(viagem(rotaId = "r2", embarcacaoId = "e1", horaMin = 6 * 60))
        }
        val vm = buscaVm(viagens, escopo = FakeEscopoDaSessao.plataforma())
        advanceUntilIdle()

        assertEquals("12:00", vm.uiState.value.resultados.single().chegada)
    }

    @Test
    fun `o filtro casa contra a rota e contra a embarcacao`() = runTest(mainRule.dispatcher) {
        val viagens = FakeViagemRepository().apply { this.viagens = listOf(viagem()) }
        val vm = buscaVm(viagens)
        advanceUntilIdle()

        vm.onFiltroChange("Parintins")
        assertEquals(1, vm.uiState.value.resultados.size)

        vm.onFiltroChange("Modelo")
        assertEquals(1, vm.uiState.value.resultados.size)

        vm.onFiltroChange("Santarém")
        assertTrue(vm.uiState.value.resultados.isEmpty())
    }

    // --- Inativar ---

    @Test
    fun `a plataforma inativa, e a lista reflete`() = runTest(mainRule.dispatcher) {
        val viagens = FakeViagemRepository().apply { this.viagens = listOf(viagem(id = "v1")) }
        val vm = buscaVm(viagens, sessao = FakeSessaoUsuario.plataforma())
        advanceUntilIdle()

        assertTrue(vm.uiState.value.podeInativar)
        vm.onInativar("v1")
        advanceUntilIdle()

        assertFalse(viagens.viagens.single().ativo)
    }

    /**
     * Segunda barreira do mesmo recorte: a tela esconde o botão, e o **ViewModel também recusa**. Tirar
     * do ar uma saída atinge bilhetes de quem nem sabe que ela existe.
     */
    @Test
    fun `o supervisor nao inativa — nem pelo VM`() = runTest(mainRule.dispatcher) {
        val viagens = FakeViagemRepository().apply { this.viagens = listOf(viagem(id = "v1")) }
        val vm = buscaVm(viagens, sessao = FakeSessaoUsuario.supervisor())
        advanceUntilIdle()

        assertFalse(vm.uiState.value.podeInativar)
        vm.onInativar("v1")
        advanceUntilIdle()

        assertTrue(viagens.viagens.single().ativo)
    }

    // --- Os dois vazios ---

    /**
     * *"Não recebeu nada"* e *"não há viagem no que recebeu"* pareceriam a mesma tela e não são: a
     * primeira se resolve com a plataforma, a segunda com o botão de criar.
     */
    @Test
    fun `sem concessao e distinto de pool vazio`() = runTest(mainRule.dispatcher) {
        val semConcessao = buscaVm(FakeViagemRepository(), escopo = FakeEscopoDaSessao.semNada())
        advanceUntilIdle()
        assertTrue(semConcessao.uiState.value.semConcessao)

        val poolVazio = buscaVm(FakeViagemRepository(), escopo = concedido())
        advanceUntilIdle()
        assertFalse(poolVazio.uiState.value.semConcessao)
    }

    /** E a plataforma **nunca** está sem concessão: lista vazia para ela é pool vazio mesmo. */
    @Test
    fun `a plataforma nunca esta sem concessao`() = runTest(mainRule.dispatcher) {
        val vm = buscaVm(
            FakeViagemRepository(),
            rotas = rotas(emptyList()),
            escopo = FakeEscopoDaSessao.plataforma(),
        )
        advanceUntilIdle()

        assertFalse(vm.uiState.value.semConcessao)
    }
}