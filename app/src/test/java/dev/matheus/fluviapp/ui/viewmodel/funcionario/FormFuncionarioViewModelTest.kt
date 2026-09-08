package dev.matheus.fluviapp.ui.viewmodel.funcionario

import androidx.lifecycle.SavedStateHandle
import dev.matheus.fluviapp.domain.operacoes.Funcionario
import dev.matheus.fluviapp.domain.operacoes.Funcionario.Cargo
import dev.matheus.fluviapp.domain.operacoes.Vinculo
import dev.matheus.fluviapp.domain.viagem.Empresa
import dev.matheus.fluviapp.fakes.FakeEmpresaRepository
import dev.matheus.fluviapp.fakes.FakeFuncionarioRepository
import dev.matheus.fluviapp.fakes.FakeSessaoUsuario
import dev.matheus.fluviapp.services.repository.operacoes.SessaoUsuario
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
import dev.matheus.fluviapp.telemetry.registroCadastroDeTeste
import org.junit.Test

/**
 * O cadastro de membro depois que ele passou a editar o **vínculo** (F6.3, [ADR-0032] Q2).
 *
 * Os dois recortes do ADR-0015 §2.1/§8.5 continuam sendo o coração desta classe — o que mudou é a
 * coordenada: onde se lia "a agência dele", leia-se "a empresa em que ele é supervisor".
 *
 * **Três casos saíram** com a forma singular, e vale dizer quais: servir a duas empresas, adicionar duas
 * vezes a mesma empresa, e remover um vínculo. Os três descreviam a administração de uma coleção — e a
 * coleção era o que a D5 descartou. Não havia nada a preservar deles: um estado que não pode acontecer
 * não precisa de teste que o discipline.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FormFuncionarioViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    /** Do cadastro de empresa só importam id e nome aqui — o resto é preenchimento obrigatório. */
    private fun empresa(id: String, nome: String) =
        Empresa(id = id, nome = nome, razaoSocial = nome, cnpj = "", endereco = "", telefone1 = "", telefone2 = "")

    private fun empresasFake() = FakeEmpresaRepository().apply {
        empresas = listOf(empresa("empresa-1", "Navegação Norte"), empresa("empresa-2", "Rio Sul"))
    }

    private fun vm(
        repo: FakeFuncionarioRepository,
        sessao: SessaoUsuario = FakeSessaoUsuario.plataforma(),
        estado: SavedStateHandle = SavedStateHandle(),
    ) = FormFuncionarioViewModel(repo, empresasFake(), sessao, registroCadastroDeTeste(), estado)

    // --- Cadastro ---

    @Test
    fun `salvar invalido marca erros e nao persiste`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository()
        val vm = vm(fake)
        advanceUntilIdle()

        vm.salvar()

        val s = vm.uiState.value
        assertTrue(s.isNomeError)
        assertTrue(s.isEmailError)
        assertTrue(s.isEmpresaError)
        assertTrue(fake.salvos.isEmpty())
    }

    /** Sem vínculo, a pessoa não enxerga seção nenhuma: salvar assim seria fabricar quem "não é da casa". */
    @Test
    fun `nome e email preenchidos nao bastam — falta o vinculo`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository()
        val vm = vm(fake)
        advanceUntilIdle()

        vm.onNomeChange("Ana")
        vm.onEmailChange("ana@fluviapp.com.br")
        vm.salvar()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isEmpresaError)
        assertTrue(fake.salvos.isEmpty())
    }

    @Test
    fun `criar persiste o vinculo e emite sucesso`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository()
        val vm = vm(fake)
        advanceUntilIdle()
        val eventos = mutableListOf<Unit>()
        val job = launch { vm.sucesso.toList(eventos) }

        vm.onNomeChange("Ana")
        vm.onEmailChange("ana@fluviapp.com.br")
        vm.onEmpresaChange("Navegação Norte")
        vm.salvar()
        advanceUntilIdle()

        val salvo = fake.salvos.single()
        assertEquals("", salvo.id)
        assertEquals("Ana", salvo.descricaoNome)
        assertEquals(Vinculo("empresa-1", Cargo.AGENTE), salvo.vinculo)
        // Nasce no menor privilégio, mesmo cadastrado pela plataforma (ADR-0015 §8.5).
        assertEquals(Cargo.AGENTE.name, salvo.cargo)
        assertEquals(1, eventos.size)
        job.cancel()
    }

    /**
     * **Escolher a empresa é atribuir o vínculo**: não há gesto de acrescentar entre uma coisa e a outra.
     * É o que o `salvar` grava sem que nada precise ser confirmado antes.
     */
    @Test
    fun `escolher a empresa ja e o vinculo — sem gesto intermediario`() = runTest(mainRule.dispatcher) {
        val vm = vm(FakeFuncionarioRepository())
        advanceUntilIdle()

        vm.onEmpresaChange("Rio Sul")
        vm.onCargoChange(Cargo.SUPERVISOR.name)

        assertEquals(Vinculo("empresa-2", Cargo.SUPERVISOR), vm.uiState.value.vinculo)
    }

    /** Trocar a empresa **reatribui**: o vínculo é um, e o último escolhido é o que vale. */
    @Test
    fun `trocar a empresa reatribui em vez de acumular`() = runTest(mainRule.dispatcher) {
        val vm = vm(FakeFuncionarioRepository())
        advanceUntilIdle()

        vm.onEmpresaChange("Navegação Norte")
        vm.onEmpresaChange("Rio Sul")

        assertEquals(Vinculo("empresa-2", Cargo.AGENTE), vm.uiState.value.vinculo)
    }

    /**
     * O `cargo` do documento é o **último legado**, e continua sendo escrito derivado do vínculo — não
     * para o app, que lê o cargo do vínculo em vigor, mas para a regra de *passagem* no servidor
     * (`cargoDoAutor`), que a F9 reescreve.
     */
    @Test
    fun `o cargo legado espelha o vinculo`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository()
        val vm = vm(fake)
        advanceUntilIdle()

        vm.onNomeChange("Ana")
        vm.onEmailChange("ana@fluviapp.com.br")
        vm.onEmpresaChange("Rio Sul")
        vm.onCargoChange(Cargo.SUPERVISOR.name)
        vm.salvar()
        advanceUntilIdle()

        assertEquals(Cargo.SUPERVISOR.name, fake.salvos.single().cargo)
    }

    // --- Edição ---

    @Test
    fun `editar carrega o vinculo e preserva o id do persistido`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository().apply {
            funcionarios = listOf(
                Funcionario(
                    id = "a1",
                    descricaoNome = "Ana",
                    email = "ana@x.com",
                    vinculo = Vinculo("empresa-1", Cargo.SUPERVISOR),
                )
            )
        }
        val vm = vm(fake, estado = SavedStateHandle(mapOf("idFuncionario" to "a1")))
        advanceUntilIdle()

        // A empresa volta **por rótulo**, que é a forma que o seletor entende.
        assertEquals("Ana", vm.uiState.value.nome)
        assertEquals("Navegação Norte", vm.uiState.value.empresa)
        assertEquals(Cargo.SUPERVISOR.name, vm.uiState.value.cargo)

        vm.onNomeChange("Ana Maria")
        vm.salvar()
        advanceUntilIdle()

        val salvo = fake.salvos.single()
        assertEquals("a1", salvo.id)
        assertEquals("Ana Maria", salvo.descricaoNome)
        // O vínculo carregado volta como estava: editar o nome não é reatribuir ninguém.
        assertEquals(Vinculo("empresa-1", Cargo.SUPERVISOR), salvo.vinculo)
    }

    // --- Os dois recortes (ADR-0015 §2.1/§8.5) ---

    @Test
    fun `plataforma escolhe empresa e cargo`() = runTest(mainRule.dispatcher) {
        val vm = vm(FakeFuncionarioRepository(), FakeSessaoUsuario.plataforma())
        advanceUntilIdle()

        val s = vm.uiState.value
        assertTrue(s.podeEscolherEmpresa)
        assertTrue(s.podeDefinirCargo)
        assertEquals(listOf("Navegação Norte", "Rio Sul"), s.empresas.map { it.nome })
        assertEquals(Cargo.entries.map { it.name }, s.listaCargo)
    }

    @Test
    fun `supervisor cadastra na PROPRIA empresa, ja escolhida e sem alternativa`() =
        runTest(mainRule.dispatcher) {
            val vm = vm(FakeFuncionarioRepository(), FakeSessaoUsuario.supervisor(empresaId = "empresa-2"))
            advanceUntilIdle()

            val s = vm.uiState.value
            assertFalse(s.podeEscolherEmpresa)
            assertEquals(listOf("Rio Sul"), s.empresas.map { it.nome })
            // Uma lista de um item é uma pergunta sem alternativa: já vem escolhida.
            assertEquals("Rio Sul", s.empresa)
        }

    /**
     * **O supervisor gere a equipe dele por inteiro** (F6.7) — inclusive promovendo. O §8.5 reservava o
     * cargo à plataforma porque ele concede editar-qualquer-passagem; com o escopo por empresa, esse
     * poder passou a valer **dentro de uma empresa só**, e isso é decisão de negócio dela. O que
     * continua impossível é mexer nos próprios vínculos — e essa barreira é do servidor.
     */
    @Test
    fun `supervisor promove dentro da propria empresa`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository()
        val vm = vm(fake, FakeSessaoUsuario.supervisor(empresaId = "empresa-2"))
        advanceUntilIdle()

        vm.onNomeChange("Carla")
        vm.onEmailChange("carla@fluviapp.com.br")
        vm.onCargoChange(Cargo.SUPERVISOR.name)
        vm.salvar()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.podeDefinirCargo)
        assertEquals(Vinculo("empresa-2", Cargo.SUPERVISOR), fake.salvos.single().vinculo)
    }

    /** O supervisor não escapa do recorte trocando a empresa por outro caminho. */
    @Test
    fun `supervisor nao muda a empresa do vinculo`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository()
        val vm = vm(fake, FakeSessaoUsuario.supervisor(empresaId = "empresa-2"))
        advanceUntilIdle()

        vm.onEmpresaChange("Navegação Norte")

        assertEquals("Rio Sul", vm.uiState.value.empresa)
    }

    @Test
    fun `sem sessao o form nasce fechado — sem escolher empresa nem cargo`() = runTest(mainRule.dispatcher) {
        val vm = vm(FakeFuncionarioRepository(), FakeSessaoUsuario(contexto = null))
        advanceUntilIdle()

        val s = vm.uiState.value
        assertFalse(s.podeEscolherEmpresa)
        assertFalse(s.podeDefinirCargo)
        // Sem vínculo de quem cadastra, não há empresa a oferecer: nada a atribuir.
        assertTrue(s.empresas.isEmpty())
    }
}