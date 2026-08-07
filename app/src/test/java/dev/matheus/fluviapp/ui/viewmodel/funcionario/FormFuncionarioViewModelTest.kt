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
import org.junit.Test

/**
 * O cadastro de membro depois que ele passou a editar **vínculos** (F6.3).
 *
 * Os dois recortes do ADR-0015 §2.1/§8.5 continuam sendo o coração desta classe — o que mudou é a
 * coordenada: onde se lia "a agência dele", leia-se "a empresa em que ele é supervisor".
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
    ) = FormFuncionarioViewModel(repo, empresasFake(), sessao, estado)

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
        assertTrue(s.isVinculosError)
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

        assertTrue(vm.uiState.value.isVinculosError)
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
        vm.onAdicionarVinculo()
        vm.salvar()
        advanceUntilIdle()

        val salvo = fake.salvos.single()
        assertEquals("", salvo.id)
        assertEquals("Ana", salvo.descricaoNome)
        assertEquals(listOf(Vinculo("empresa-1", Cargo.AGENTE)), salvo.vinculos)
        // Nasce no menor privilégio, mesmo cadastrado pela plataforma (ADR-0015 §8.5).
        assertEquals(Cargo.AGENTE.name, salvo.cargo)
        assertEquals(1, eventos.size)
        job.cancel()
    }

    /** A ponte para o bilhete (F6.5): o legado deixa de ser texto livre e espelha a empresa do vínculo. */
    @Test
    fun `o campo legado agencia passa a espelhar o nome da empresa do primeiro vinculo`() =
        runTest(mainRule.dispatcher) {
            val fake = FakeFuncionarioRepository()
            val vm = vm(fake)
            advanceUntilIdle()

            vm.onNomeChange("Ana")
            vm.onEmailChange("ana@fluviapp.com.br")
            vm.onEmpresaChange("Rio Sul")
            vm.onAdicionarVinculo()
            vm.salvar()
            advanceUntilIdle()

            assertEquals("Rio Sul", fake.salvos.single().agencia)
        }

    @Test
    fun `a pessoa pode servir a duas empresas, com cargos diferentes`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository()
        val vm = vm(fake)
        advanceUntilIdle()

        vm.onNomeChange("Ana")
        vm.onEmailChange("ana@fluviapp.com.br")
        vm.onEmpresaChange("Navegação Norte")
        vm.onCargoChange(Cargo.SUPERVISOR.name)
        vm.onAdicionarVinculo()
        vm.onEmpresaChange("Rio Sul")
        vm.onCargoChange(Cargo.AGENTE.name)
        vm.onAdicionarVinculo()
        vm.salvar()
        advanceUntilIdle()

        assertEquals(
            listOf(Vinculo("empresa-1", Cargo.SUPERVISOR), Vinculo("empresa-2", Cargo.AGENTE)),
            fake.salvos.single().vinculos,
        )
    }

    /**
     * Dois vínculos na mesma empresa não significam nada — o segundo só poderia contradizer o cargo do
     * primeiro. Reatribuir é o gesto que a pessoa tem em mente ao escolher de novo.
     */
    @Test
    fun `adicionar de novo a mesma empresa substitui o cargo, nao duplica`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository()
        val vm = vm(fake)
        advanceUntilIdle()

        vm.onEmpresaChange("Navegação Norte")
        vm.onCargoChange(Cargo.AGENTE.name)
        vm.onAdicionarVinculo()
        vm.onCargoChange(Cargo.SUPERVISOR.name)
        vm.onAdicionarVinculo()

        assertEquals(listOf(Vinculo("empresa-1", Cargo.SUPERVISOR)), vm.uiState.value.vinculos)
    }

    @Test
    fun `remover tira o vinculo daquela empresa`() = runTest(mainRule.dispatcher) {
        val vm = vm(FakeFuncionarioRepository())
        advanceUntilIdle()

        vm.onEmpresaChange("Navegação Norte")
        vm.onAdicionarVinculo()
        vm.onEmpresaChange("Rio Sul")
        vm.onAdicionarVinculo()
        vm.onRemoverVinculo("empresa-1")

        assertEquals(listOf(Vinculo("empresa-2", Cargo.AGENTE)), vm.uiState.value.vinculos)
    }

    // --- Edição ---

    @Test
    fun `editar carrega os vinculos e preserva o id do persistido`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository().apply {
            funcionarios = listOf(
                Funcionario(
                    id = "a1",
                    descricaoNome = "Ana",
                    agencia = "Navegação Norte",
                    email = "ana@x.com",
                    vinculos = listOf(Vinculo("empresa-1", Cargo.SUPERVISOR)),
                )
            )
        }
        val vm = vm(fake, estado = SavedStateHandle(mapOf("idFuncionario" to "a1")))
        advanceUntilIdle()

        assertEquals("Ana", vm.uiState.value.nome)
        assertEquals(listOf("Navegação Norte · SUPERVISOR"), vm.uiState.value.vinculosNaTela.map { "${it.empresa} · ${it.cargo}" })

        vm.onNomeChange("Ana Maria")
        vm.salvar()
        advanceUntilIdle()

        val salvo = fake.salvos.single()
        assertEquals("a1", salvo.id)
        assertEquals("Ana Maria", salvo.descricaoNome)
        // O vínculo carregado volta como estava: editar o nome não é reatribuir ninguém.
        assertEquals(listOf(Vinculo("empresa-1", Cargo.SUPERVISOR)), salvo.vinculos)
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
            assertEquals("Rio Sul", s.empresaEmEdicao)
        }

    @Test
    fun `supervisor nao define cargo — o membro nasce AGENTE mesmo se o evento for disparado`() =
        runTest(mainRule.dispatcher) {
            val fake = FakeFuncionarioRepository()
            val vm = vm(fake, FakeSessaoUsuario.supervisor(empresaId = "empresa-2"))
            advanceUntilIdle()

            vm.onNomeChange("Carla")
            vm.onEmailChange("carla@fluviapp.com.br")
            // A tela nem desenha o seletor; o VM ignora o evento se ele vier por outro caminho.
            vm.onCargoChange(Cargo.SUPERVISOR.name)
            vm.onAdicionarVinculo()
            vm.salvar()
            advanceUntilIdle()

            assertFalse(vm.uiState.value.podeDefinirCargo)
            assertEquals(listOf(Vinculo("empresa-2", Cargo.AGENTE)), fake.salvos.single().vinculos)
        }

    /** O supervisor não escapa do recorte trocando a empresa por outro caminho. */
    @Test
    fun `supervisor nao muda a empresa do vinculo`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository()
        val vm = vm(fake, FakeSessaoUsuario.supervisor(empresaId = "empresa-2"))
        advanceUntilIdle()

        vm.onEmpresaChange("Navegação Norte")

        assertEquals("Rio Sul", vm.uiState.value.empresaEmEdicao)
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