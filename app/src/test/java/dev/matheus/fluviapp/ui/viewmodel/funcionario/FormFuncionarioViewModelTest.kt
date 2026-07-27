package dev.matheus.fluviapp.ui.viewmodel.funcionario

import androidx.lifecycle.SavedStateHandle
import dev.matheus.fluviapp.fakes.FakeFuncionarioRepository
import dev.matheus.fluviapp.fakes.FakeConstanteRepository
import dev.matheus.fluviapp.fakes.FakeSessaoUsuario
import dev.matheus.fluviapp.model.cadastro.constantes.Constante
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Categoria.MUNICIPIO
import dev.matheus.fluviapp.model.operacoes.Funcionario
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

@OptIn(ExperimentalCoroutinesApi::class)
class FormFuncionarioViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private fun constanteFake() = FakeConstanteRepository().apply {
        constantes = listOf(Constante("1", "PORTO NORTE", MUNICIPIO.name))
    }

    private fun vm(
        repo: FakeFuncionarioRepository,
        sessao: SessaoUsuario = FakeSessaoUsuario.plataforma(),
        estado: SavedStateHandle = SavedStateHandle(),
    ) = FormFuncionarioViewModel(repo, constanteFake(), sessao, estado)

    @Test
    fun `salvar invalido marca erros e nao persiste`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository()
        val vm = vm(fake)
        advanceUntilIdle()

        vm.salvar()

        val s = vm.uiState.value
        assertTrue(s.isAgenciaError)
        assertTrue(s.isFuncionarioError)
        assertTrue(s.isEmailError)
        assertTrue(s.isLotacaoError)
        assertTrue(fake.salvos.isEmpty())
    }

    @Test
    fun `criar persiste funcionario novo e emite sucesso`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository()
        val vm = vm(fake)
        advanceUntilIdle()
        val eventos = mutableListOf<Unit>()
        val job = launch { vm.sucesso.toList(eventos) }

        vm.onAgenciaChange("NOVA")
        vm.onFuncionarioChange("Ana")
        vm.onEmailChange("ana@fluviapp.com.br")
        vm.onLotacaoChange("PORTO NORTE")
        vm.salvar()
        advanceUntilIdle()

        assertEquals(1, fake.salvos.size)
        assertEquals("", fake.salvos.first().id)
        assertEquals("Ana", fake.salvos.first().descricaoNome)
        assertEquals("ana@fluviapp.com.br", fake.salvos.first().email)
        // Nasce no menor privilégio, mesmo cadastrado pela plataforma (ADR-0015 §8.5).
        assertEquals(Funcionario.Cargo.AGENTE.name, fake.salvos.first().cargo)
        assertEquals(1, eventos.size)
        job.cancel()
    }

    @Test
    fun `editar atualiza campos e preserva o id do funcionario persistido`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository().apply {
            funcionarios = listOf(Funcionario("a1", "Ana", "MATRIZ", "PORTO NORTE", email = "ana@x.com"))
        }
        val vm = vm(fake, estado = SavedStateHandle(mapOf("idFuncionario" to "a1")))
        advanceUntilIdle()
        assertEquals("Ana", vm.uiState.value.funcionario)
        assertEquals("ana@x.com", vm.uiState.value.email)

        vm.onFuncionarioChange("Ana Maria")
        vm.salvar()
        advanceUntilIdle()

        val salvo = fake.salvos.first()
        assertEquals("a1", salvo.id)
        assertEquals("Ana Maria", salvo.descricaoNome)
        // Campos carregados e não mexidos voltam como estavam (o form parte do persistido).
        assertEquals("MATRIZ", salvo.agencia)
    }

    // --- Os dois recortes do cadastro (ADR-0015 §2.1/§8.5) ---

    @Test
    fun `plataforma escolhe agencia e cargo`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository().apply {
            funcionarios = listOf(Funcionario("a1", "Ana", "MATRIZ", "PORTO NORTE"))
        }
        val vm = vm(fake, FakeSessaoUsuario.plataforma())
        advanceUntilIdle()

        val s = vm.uiState.value
        assertTrue(s.podeEscolherAgencia)
        assertTrue(s.podeDefinirCargo)
        assertEquals(listOf("MATRIZ"), s.listaAgencia)
        assertEquals(Funcionario.Cargo.entries.map { it.name }, s.listaCargo)
    }

    @Test
    fun `plataforma promove o membro a SUPERVISOR`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository()
        val vm = vm(fake, FakeSessaoUsuario.plataforma())
        advanceUntilIdle()

        vm.onAgenciaChange("MATRIZ")
        vm.onFuncionarioChange("Bruno")
        vm.onEmailChange("bruno@fluviapp.com.br")
        vm.onLotacaoChange("PORTO NORTE")
        vm.onCargoChange(Funcionario.Cargo.SUPERVISOR.name)
        vm.salvar()
        advanceUntilIdle()

        assertEquals(Funcionario.Cargo.SUPERVISOR.name, fake.salvos.first().cargo)
    }

    @Test
    fun `supervisor cadastra na PROPRIA agencia, sem seletor`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository().apply {
            funcionarios = listOf(Funcionario("a1", "Ana", "AGENCIA MARE", "PORTO NORTE"))
        }
        val vm = vm(fake, FakeSessaoUsuario.supervisor(agencia = "AGENCIA LITORAL"))
        advanceUntilIdle()

        val s = vm.uiState.value
        assertFalse(s.podeEscolherAgencia)
        assertEquals("AGENCIA LITORAL", s.agencia)
        // Sem seletor não há lista para oferecer — nem a das outras agências.
        assertTrue(s.listaAgencia.isEmpty())
    }

    @Test
    fun `supervisor nao define cargo — o membro nasce AGENTE mesmo se o evento for disparado`() =
        runTest(mainRule.dispatcher) {
            val fake = FakeFuncionarioRepository()
            val vm = vm(fake, FakeSessaoUsuario.supervisor(agencia = "AGENCIA LITORAL"))
            advanceUntilIdle()

            vm.onFuncionarioChange("Carla")
            vm.onEmailChange("carla@fluviapp.com.br")
            vm.onLotacaoChange("PORTO NORTE")
            // A tela nem desenha o seletor; o VM ignora o evento se ele vier por outro caminho.
            vm.onCargoChange(Funcionario.Cargo.SUPERVISOR.name)
            vm.salvar()
            advanceUntilIdle()

            assertFalse(vm.uiState.value.podeDefinirCargo)
            assertEquals(Funcionario.Cargo.AGENTE.name, fake.salvos.first().cargo)
            assertEquals("AGENCIA LITORAL", fake.salvos.first().agencia)
        }

    @Test
    fun `supervisor editando membro nao rebaixa o cargo gravado`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository().apply {
            funcionarios = listOf(
                Funcionario("a1", "Ana", "AGENCIA LITORAL", "PORTO NORTE", Funcionario.Cargo.SUPERVISOR.name, "ana@x.com")
            )
        }
        val vm = vm(fake, FakeSessaoUsuario.supervisor(agencia = "AGENCIA LITORAL"), SavedStateHandle(mapOf("idFuncionario" to "a1")))
        advanceUntilIdle()

        vm.onLotacaoChange("ILHA CENTRAL")
        vm.salvar()
        advanceUntilIdle()

        // Editar lotação não pode virar rebaixamento silencioso: o cargo do persistido é preservado.
        assertEquals(Funcionario.Cargo.SUPERVISOR.name, fake.salvos.first().cargo)
        assertEquals("ILHA CENTRAL", fake.salvos.first().lotacao)
    }

    @Test
    fun `sem sessao o form nasce fechado — sem escolher agencia nem cargo`() = runTest(mainRule.dispatcher) {
        val vm = vm(FakeFuncionarioRepository(), FakeSessaoUsuario(contexto = null))
        advanceUntilIdle()

        val s = vm.uiState.value
        assertFalse(s.podeEscolherAgencia)
        assertFalse(s.podeDefinirCargo)
    }
}