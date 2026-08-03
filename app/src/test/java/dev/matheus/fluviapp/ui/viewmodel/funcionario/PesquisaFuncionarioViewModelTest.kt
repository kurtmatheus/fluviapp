package dev.matheus.fluviapp.ui.viewmodel.funcionario

import dev.matheus.fluviapp.fakes.FakeFuncionarioRepository
import dev.matheus.fluviapp.fakes.FakeSessaoUsuario
import dev.matheus.fluviapp.domain.operacoes.Funcionario
import dev.matheus.fluviapp.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import dev.matheus.fluviapp.services.repository.operacoes.SessaoUsuario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PesquisaFuncionarioViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private fun vm(repo: FakeFuncionarioRepository, sessao: SessaoUsuario = FakeSessaoUsuario.plataforma()) =
        PesquisaFuncionarioViewModel(repo, sessao)

    private val amostra = listOf(
        Funcionario("1", "Ana", "AGENCIA LITORAL", "PORTO NORTE"),
        Funcionario("2", "Bruno", "AGENCIA MARE", "ILHA CENTRAL"),
        Funcionario("3", "Carla", "AGENCIA LITORAL", "PORTO NORTE"),
    )

    @Test
    fun `carrega todos os funcionarios e as agencias`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository().apply { funcionarios = amostra }

        val vm = vm(fake)
        advanceUntilIdle()

        assertEquals(3, vm.uiState.value.resultados.size)
        assertEquals(2, vm.uiState.value.listaAgencia.size) // distinct
    }

    @Test
    fun `filtra resultados por agencia no VM`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository().apply { funcionarios = amostra }
        val vm = vm(fake)
        advanceUntilIdle()

        vm.onAgenciaChange("AGENCIA LIT")

        assertEquals(2, vm.uiState.value.resultados.size)
        assertEquals(setOf("Ana", "Carla"), vm.uiState.value.resultados.map { it.descricaoNome }.toSet())
    }

    @Test
    fun `carrega lotacoes distintas`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository().apply { funcionarios = amostra }
        val vm = vm(fake)
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.listaLotacao.size) // PORTO NORTE, ILHA CENTRAL
    }

    @Test
    fun `filtra por lotacao (dropdown, match exato)`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository().apply { funcionarios = amostra }
        val vm = vm(fake)
        advanceUntilIdle()

        vm.onLotacaoChange("PORTO NORTE")

        assertEquals(setOf("Ana", "Carla"), vm.uiState.value.resultados.map { it.descricaoNome }.toSet())
    }

    @Test
    fun `combina filtro de agencia e lotacao`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository().apply { funcionarios = amostra }
        val vm = vm(fake)
        advanceUntilIdle()

        vm.onAgenciaChange("AGENCIA MARE")
        vm.onLotacaoChange("PORTO NORTE")

        // Bruno é MARE mas lotação ILHA CENTRAL → nenhum casa.
        assertEquals(0, vm.uiState.value.resultados.size)
    }

    @Test
    fun `deletar remove o funcionario e recarrega os resultados`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository().apply { funcionarios = amostra }
        val vm = vm(fake)
        advanceUntilIdle()

        vm.onDeletar("1")
        advanceUntilIdle()

        assertTrue(fake.deletados.contains("1"))
        assertEquals(2, vm.uiState.value.resultados.size)
        assertNull(vm.uiState.value.resultados.find { it.id == "1" })
    }

    // --- Recorte por cargo na listagem (ADR-0015 §2.2) ---

    @Test
    fun `supervisor ve apenas a propria agencia e nao filtra por agencia`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository().apply { funcionarios = amostra }

        val vm = vm(fake, FakeSessaoUsuario.supervisor(agencia = "AGENCIA LITORAL"))
        advanceUntilIdle()

        val s = vm.uiState.value
        assertFalse(s.podeFiltrarPorAgencia)
        assertEquals(setOf("Ana", "Carla"), s.resultados.map { it.descricaoNome }.toSet())
        // Sem filtro de agência, também não se oferece a lista das outras.
        assertTrue(s.listaAgencia.isEmpty())
    }

    @Test
    fun `supervisor nao escapa do recorte tentando filtrar por outra agencia`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository().apply { funcionarios = amostra }
        val vm = vm(fake, FakeSessaoUsuario.supervisor(agencia = "AGENCIA LITORAL"))
        advanceUntilIdle()

        vm.onAgenciaChange("AGENCIA MARE")

        // O recorte é do universo, não do filtro: o evento é ignorado e a lista continua sendo a dele.
        assertEquals(setOf("Ana", "Carla"), vm.uiState.value.resultados.map { it.descricaoNome }.toSet())
    }

    @Test
    fun `supervisor nao deleta membro`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository().apply { funcionarios = amostra }
        val vm = vm(fake, FakeSessaoUsuario.supervisor(agencia = "AGENCIA LITORAL"))
        advanceUntilIdle()

        vm.onDeletar("1")
        advanceUntilIdle()

        assertFalse(vm.uiState.value.podeDeletar)
        assertTrue(fake.deletados.isEmpty())
    }
}
