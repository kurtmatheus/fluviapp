package dev.matheus.fluviapp.ui.viewmodel.empresa

import androidx.lifecycle.SavedStateHandle
import dev.matheus.fluviapp.fakes.FakeEmbarcacaoRepository
import dev.matheus.fluviapp.fakes.FakeEmpresaRepository
import dev.matheus.fluviapp.fakes.FakeLocalidadeRepository
import dev.matheus.fluviapp.fakes.FakePortoRepository
import dev.matheus.fluviapp.domain.localidade.Localidade
import dev.matheus.fluviapp.domain.localidade.Uf
import dev.matheus.fluviapp.domain.porto.Porto
import dev.matheus.fluviapp.domain.operacoes.Atuacao
import dev.matheus.fluviapp.domain.viagem.AtuacaoDaEmpresa
import dev.matheus.fluviapp.domain.viagem.Embarcacao
import dev.matheus.fluviapp.domain.viagem.Empresa
import dev.matheus.fluviapp.domain.viagem.TipoEmbarcacao
import dev.matheus.fluviapp.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FormEmpresaViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun `salvar invalido marca erros e nao persiste`() = runTest(mainRule.dispatcher) {
        val fake = FakeEmpresaRepository()
        val vm = viewModel(fake)

        vm.salvar()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertTrue(s.isNomeError)
        assertTrue(s.isRazaoSocialError)
        assertTrue(s.isCnpjError)
        assertTrue(fake.salvos.isEmpty())
    }

    @Test
    fun `salvar valido persiste cnpj em digitos e emite sucesso`() = runTest(mainRule.dispatcher) {
        val fake = FakeEmpresaRepository()
        val vm = viewModel(fake)
        val eventos = mutableListOf<Unit>()
        val job = launch { vm.sucesso.toList(eventos) }

        vm.onNomeChange("ACME")
        vm.onRazaoSocialChange("ACME LTDA")
        vm.onCnpjChange("11.222.333/0001-81") // com máscara → guarda só dígitos
        vm.onAtuacaoToggle(Atuacao.AGENCIAMENTO) // obrigatória (domínio §3.1)
        vm.salvar()
        advanceUntilIdle()

        assertEquals(1, fake.salvos.size)
        assertEquals("11222333000181", fake.salvos.first().cnpj)
        assertEquals(1, eventos.size)
        job.cancel()
    }

    @Test
    fun `falha ao salvar nao emite sucesso e libera processamento`() = runTest(mainRule.dispatcher) {
        val fake = FakeEmpresaRepository().apply { falharAoSalvar = true }
        val vm = viewModel(fake)
        val eventos = mutableListOf<Unit>()
        val job = launch { vm.sucesso.toList(eventos) }

        vm.onNomeChange("ACME")
        vm.onRazaoSocialChange("ACME LTDA")
        vm.onCnpjChange("11222333000181")
        vm.salvar()
        advanceUntilIdle()

        assertTrue(eventos.isEmpty())
        assertFalse(vm.uiState.value.isProcessing)
        job.cancel()
    }

    @Test
    fun `edicao carrega empresa existente`() = runTest(mainRule.dispatcher) {
        val fake = FakeEmpresaRepository().apply {
            empresas = listOf(Empresa("e1", "ACME", "ACME LTDA", "11222333000181", "Rua 1", "111", "222"))
        }
        val vm = viewModel(fake, idEmpresa = "e1")
        advanceUntilIdle()

        val s = vm.uiState.value
        assertEquals("ACME", s.nome)
        assertEquals("ACME LTDA", s.razaoSocial)
        assertEquals("11222333000181", s.cnpj)
    }

    // --- Atuações: a subcoleção `empresas/{id}/atuacoes` (ADR-0016 §4, ADR-0020 F5c) ---

    @Test
    fun `atuacao marcada e salva na subcolecao, pendurada no id gerado`() = runTest(mainRule.dispatcher) {
        val fake = FakeEmpresaRepository()
        val vm = viewModel(fake)

        preencherObrigatorios(vm)
        vm.onAtuacaoToggle(Atuacao.AGENCIAMENTO)
        vm.salvar()
        advanceUntilIdle()

        val id = fake.salvos.single().id.ifBlank { "id-gerado-1" }
        assertEquals(
            listOf(AtuacaoDaEmpresa(Atuacao.AGENCIAMENTO)),
            fake.atuacoesPorEmpresa[id],
        )
    }

    @Test
    fun `o toggle liga e desliga — desmarcar deixa a parte sem aquela atuacao`() {
        val vm = viewModel(FakeEmpresaRepository())

        vm.onAtuacaoToggle(Atuacao.TRANSPORTE)
        assertEquals(setOf(Atuacao.TRANSPORTE), vm.uiState.value.atuacoes)

        vm.onAtuacaoToggle(Atuacao.TRANSPORTE)
        assertTrue(vm.uiState.value.atuacoes.isEmpty())
    }

    @Test
    fun `uma parte exerce varias atuacoes ao mesmo tempo`() {
        val vm = viewModel(FakeEmpresaRepository())

        vm.onAtuacaoToggle(Atuacao.AGENCIAMENTO)
        vm.onAtuacaoToggle(Atuacao.TRANSPORTE)

        assertEquals(setOf(Atuacao.AGENCIAMENTO, Atuacao.TRANSPORTE), vm.uiState.value.atuacoes)
    }

    // --- Concessão: o que esta parte pode vender (ADR-0016 §7.1) ---

    /**
     * Editar a empresa sem tocar na concessão não pode apagá-la. Antes isto se garantia **preservando** o
     * que estava gravado (o form não editava concessão); agora se garante por **ida e volta**: a edição
     * carrega os ids para o estado e o salvar devolve os mesmos. O resultado é o mesmo, o caminho não —
     * e é o caminho que este teste vigia, porque é ele que pode quebrar.
     */
    @Test
    fun `editar a empresa sem mexer na concessao devolve os mesmos ids`() = runTest(mainRule.dispatcher) {
        val fake = FakeEmpresaRepository()
        fake.empresas = listOf(empresaValida("e1"))
        fake.atuacoesPorEmpresa["e1"] =
            listOf(AtuacaoDaEmpresa(Atuacao.AGENCIAMENTO, embarcacaoIds = setOf("embarcacao-7")))
        val vm = viewModel(fake, frota = listOf(embarcacao("embarcacao-7", "F/B SETE")), idEmpresa = "e1")
        advanceUntilIdle()

        assertEquals(setOf("embarcacao-7"), vm.uiState.value.embarcacoesConcedidas)

        vm.onNomeChange("OUTRO NOME")
        vm.salvar()
        advanceUntilIdle()

        assertEquals(
            setOf("embarcacao-7"),
            fake.atuacoesPorEmpresa["e1"]?.single()?.embarcacaoIds,
        )
    }

    /** A frota inteira é candidata: agenciar é vender o que é dos outros (7ª rodada do §7). */
    @Test
    fun `o formulario oferece toda a frota, e nao so a da propria parte`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(
            FakeEmpresaRepository(),
            frota = listOf(embarcacao("n1", "F/B UM"), embarcacao("n2", "F/B DOIS")),
        )
        advanceUntilIdle()

        assertEquals(listOf("n1", "n2"), vm.uiState.value.embarcacoes.map { it.id })
    }

    @Test
    fun `conceder e revogar sao o mesmo gesto`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(FakeEmpresaRepository(), frota = listOf(embarcacao("n1", "F/B UM")))
        advanceUntilIdle()

        vm.onEmbarcacaoToggle("n1")
        assertEquals(setOf("n1"), vm.uiState.value.embarcacoesConcedidas)

        vm.onEmbarcacaoToggle("n1")
        assertTrue(vm.uiState.value.embarcacoesConcedidas.isEmpty())
    }

    @Test
    fun `a concessao escolhida e gravada na atuacao de agenciamento`() = runTest(mainRule.dispatcher) {
        val fake = FakeEmpresaRepository()
        val vm = viewModel(fake, frota = listOf(embarcacao("n1", "F/B UM"), embarcacao("n2", "F/B DOIS")))
        advanceUntilIdle()

        preencherObrigatorios(vm)
        vm.onAtuacaoToggle(Atuacao.AGENCIAMENTO)
        vm.onEmbarcacaoToggle("n2")
        vm.salvar()
        advanceUntilIdle()

        val id = fake.salvos.single().id.ifBlank { "id-gerado-1" }
        assertEquals(
            listOf(AtuacaoDaEmpresa(Atuacao.AGENCIAMENTO, embarcacaoIds = setOf("n2"))),
            fake.atuacoesPorEmpresa[id],
        )
    }

    /**
     * Revogar tem de chegar ao Firestore. Se o salvar apenas acrescentasse — preservando o gravado como
     * antes —, tirar a marca da tela não tiraria a permissão: a agência continuaria podendo vender.
     */
    @Test
    fun `revogar na tela apaga a concessao gravada`() = runTest(mainRule.dispatcher) {
        val fake = FakeEmpresaRepository()
        fake.empresas = listOf(empresaValida("e1"))
        fake.atuacoesPorEmpresa["e1"] =
            listOf(AtuacaoDaEmpresa(Atuacao.AGENCIAMENTO, embarcacaoIds = setOf("n1", "n2")))
        val vm = viewModel(
            fake,
            frota = listOf(embarcacao("n1", "F/B UM"), embarcacao("n2", "F/B DOIS")),
            idEmpresa = "e1",
        )
        advanceUntilIdle()

        vm.onEmbarcacaoToggle("n1")
        vm.salvar()
        advanceUntilIdle()

        assertEquals(setOf("n2"), fake.atuacoesPorEmpresa["e1"]?.single()?.embarcacaoIds)
    }

    /** Deixar de agenciar leva a concessão junto: a permissão não sobrevive à atuação que a justificava. */
    @Test
    fun `desmarcar agenciamento limpa a concessao do estado`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(FakeEmpresaRepository(), frota = listOf(embarcacao("n1", "F/B UM")))
        advanceUntilIdle()

        vm.onAtuacaoToggle(Atuacao.AGENCIAMENTO)
        vm.onEmbarcacaoToggle("n1")
        assertTrue(vm.uiState.value.concedeEmbarcacoes)

        vm.onAtuacaoToggle(Atuacao.AGENCIAMENTO)

        assertFalse(vm.uiState.value.concedeEmbarcacoes)
        assertTrue(vm.uiState.value.embarcacoesConcedidas.isEmpty())
    }

    /**
     * A atuação sem editor continua **preservada**: `TRANSPORTE` não tem tela de concessão, e salvar a
     * empresa não pode apagar em silêncio o que o formulário não mostra.
     */
    @Test
    fun `atuacao sem editor conserva o que estava gravado`() = runTest(mainRule.dispatcher) {
        val fake = FakeEmpresaRepository()
        fake.empresas = listOf(empresaValida("e1"))
        fake.atuacoesPorEmpresa["e1"] =
            listOf(AtuacaoDaEmpresa(Atuacao.TRANSPORTE, embarcacaoIds = setOf("herdada")))
        val vm = viewModel(fake, idEmpresa = "e1")
        advanceUntilIdle()

        vm.salvar()
        advanceUntilIdle()

        assertEquals(setOf("herdada"), fake.atuacoesPorEmpresa["e1"]?.single()?.embarcacaoIds)
    }

    @Test
    fun `edicao carrega as atuacoes ja gravadas`() = runTest(mainRule.dispatcher) {
        val fake = FakeEmpresaRepository()
        fake.empresas = listOf(empresaValida("e1"))
        fake.atuacoesPorEmpresa["e1"] = listOf(AtuacaoDaEmpresa(Atuacao.TRANSPORTE))
        val vm = viewModel(fake, idEmpresa = "e1")

        advanceUntilIdle()

        assertEquals(setOf(Atuacao.TRANSPORTE), vm.uiState.value.atuacoes)
    }

    @Test
    fun `parte sem atuacao nenhuma NAO e salva — o que a empresa faz vive nas atuacoes`() =
        runTest(mainRule.dispatcher) {
            // Domínio §3.1: a empresa não tem campo de segmento nem de tipo. Sem atuação ela não pode
            // ser escolhida em lugar nenhum — não tem cargo, não abre seção, não recebe concessão.
            val fake = FakeEmpresaRepository()
            val vm = viewModel(fake)

            preencherObrigatorios(vm)
            vm.salvar()
            advanceUntilIdle()

            assertTrue(vm.uiState.value.isAtuacoesError)
            assertTrue(fake.salvos.isEmpty())
        }

    // --- a outra metade da concessão: ONDE (F7, §7.1) ----------------------------------------------

    /**
     * O rótulo do porto traz a cidade porque é ela que distingue homônimos: "Porto Central" em Belém e
     * "Porto Central" em Manaus são duas concessões diferentes, e sem a cidade a escolha seria no escuro.
     */
    @Test
    fun `o porto e oferecido com a cidade no rotulo`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(
            FakeEmpresaRepository(),
            portos = listOf(porto("p1", "PORTO CENTRAL", localidadeId = "loc-1")),
            localidades = listOf(localidade("loc-1", "MANAUS")),
        )
        advanceUntilIdle()

        assertEquals(listOf("PORTO CENTRAL · MANAUS/AM"), vm.uiState.value.portos.map { it.rotulo })
    }

    /**
     * Porto inativo não é candidato — e é aí que ele difere da atuação dormente, que aparece desabilitada:
     * atuação dormente é vocabulário que a plataforma ainda não usa; porto inativo é registro aposentado.
     * Conceder onde ninguém mais opera seria escrever uma permissão nascida morta.
     */
    @Test
    fun `porto inativo nao entra na lista de candidatos`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(
            FakeEmpresaRepository(),
            portos = listOf(porto("p1", "ATIVO"), porto("p2", "APOSENTADO", ativo = false)),
            localidades = listOf(localidade("loc-1", "MANAUS")),
        )
        advanceUntilIdle()

        assertEquals(listOf("p1"), vm.uiState.value.portos.map { it.id })
    }

    @Test
    fun `conceder e revogar porto sao o mesmo gesto`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(FakeEmpresaRepository(), portos = listOf(porto("p1", "PORTO UM")))
        advanceUntilIdle()

        vm.onPortoToggle("p1")
        assertEquals(setOf("p1"), vm.uiState.value.portosConcedidos)

        vm.onPortoToggle("p1")
        assertTrue(vm.uiState.value.portosConcedidos.isEmpty())
    }

    /** As duas dimensões vivem no mesmo documento da atuação, e uma escrita grava as duas. */
    @Test
    fun `as duas dimensoes da concessao sao gravadas juntas`() = runTest(mainRule.dispatcher) {
        val fake = FakeEmpresaRepository()
        val vm = viewModel(
            fake,
            frota = listOf(embarcacao("n1", "F/B UM")),
            portos = listOf(porto("p1", "PORTO UM"), porto("p2", "PORTO DOIS")),
        )
        advanceUntilIdle()

        preencherObrigatorios(vm)
        vm.onAtuacaoToggle(Atuacao.AGENCIAMENTO)
        vm.onEmbarcacaoToggle("n1")
        vm.onPortoToggle("p1")
        vm.onPortoToggle("p2")
        vm.salvar()
        advanceUntilIdle()

        val id = fake.salvos.single().id.ifBlank { "id-gerado-1" }
        assertEquals(
            listOf(
                AtuacaoDaEmpresa(
                    Atuacao.AGENCIAMENTO,
                    embarcacaoIds = setOf("n1"),
                    portoIds = setOf("p1", "p2"),
                )
            ),
            fake.atuacoesPorEmpresa[id],
        )
    }

    @Test
    fun `a edicao traz de volta os portos ja concedidos`() = runTest(mainRule.dispatcher) {
        val fake = FakeEmpresaRepository()
        fake.empresas = listOf(empresaValida("e1"))
        fake.atuacoesPorEmpresa["e1"] =
            listOf(AtuacaoDaEmpresa(Atuacao.AGENCIAMENTO, portoIds = setOf("p1")))
        val vm = viewModel(fake, portos = listOf(porto("p1", "PORTO UM")), idEmpresa = "e1")
        advanceUntilIdle()

        assertEquals(setOf("p1"), vm.uiState.value.portosConcedidos)
    }

    /** Mesma razão da embarcação: a permissão não sobrevive à atuação que a justificava. */
    @Test
    fun `desmarcar agenciamento limpa tambem os portos`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(FakeEmpresaRepository(), portos = listOf(porto("p1", "PORTO UM")))
        advanceUntilIdle()

        vm.onAtuacaoToggle(Atuacao.AGENCIAMENTO)
        vm.onPortoToggle("p1")
        assertTrue(vm.uiState.value.concedePortos)

        vm.onAtuacaoToggle(Atuacao.AGENCIAMENTO)

        assertFalse(vm.uiState.value.concedePortos)
        assertTrue(vm.uiState.value.portosConcedidos.isEmpty())
    }

    /**
     * O VM conhece quatro repositórios: a Empresa que ele cadastra e três que ele só **lê** para oferecer
     * candidatos — a frota e os portos da concessão (ADR-0016 §7.1), e a localidade, que só entra para
     * dar nome de cidade ao rótulo do porto. Listas vazias são o caso comum: quem não testa concessão não
     * precisa declarar nem embarcação nem porto.
     */
    private fun viewModel(
        empresaFake: FakeEmpresaRepository,
        frota: List<Embarcacao> = emptyList(),
        portos: List<Porto> = emptyList(),
        localidades: List<Localidade> = emptyList(),
        idEmpresa: String? = null,
    ) = FormEmpresaViewModel(
        empresaFake,
        FakeEmbarcacaoRepository().apply { embarcacoes = frota },
        FakePortoRepository().apply { this.portos = portos },
        FakeLocalidadeRepository().apply { this.localidades = localidades },
        if (idEmpresa == null) SavedStateHandle() else SavedStateHandle(mapOf("idEmpresa" to idEmpresa)),
    )

    private fun embarcacao(id: String, nome: String) =
        Embarcacao(id, nome, TipoEmbarcacao.FERRY_BOAT, 10, 2, 2, 2, "outra-empresa")

    private fun porto(id: String, nome: String, localidadeId: String = "loc-1", ativo: Boolean = true) =
        Porto(id = id, nome = nome, localidadeId = localidadeId, ativo = ativo)

    private fun localidade(id: String, municipio: String) =
        Localidade(id = id, municipio = municipio, uf = Uf.AM, codigoIbge = "", ativo = true)

    private fun preencherObrigatorios(vm: FormEmpresaViewModel) {
        vm.onNomeChange("EMPRESA MODELO")
        vm.onRazaoSocialChange("EMPRESA MODELO LTDA")
        vm.onCnpjChange("11222333000181")
    }

    private fun empresaValida(id: String) = Empresa(
        id = id,
        nome = "EMPRESA MODELO",
        razaoSocial = "EMPRESA MODELO LTDA",
        cnpj = "11222333000181",
        endereco = "",
        telefone1 = "",
        telefone2 = "",
    )
}
