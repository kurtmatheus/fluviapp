package dev.matheus.fluviapp.ui.viewmodel.agente

import dev.matheus.fluviapp.fakes.FakeFuncionarioRepository
import dev.matheus.fluviapp.model.operacoes.Funcionario
import dev.matheus.fluviapp.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PesquisaAgenteViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val amostra = listOf(
        Funcionario("1", "Ana", "AGENCIA LITORAL", "PORTO NORTE"),
        Funcionario("2", "Bruno", "AGENCIA MARE", "ILHA CENTRAL"),
        Funcionario("3", "Carla", "AGENCIA LITORAL", "PORTO NORTE"),
    )

    @Test
    fun `carrega todos os agentes e as agencias`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository().apply { agentes = amostra }

        val vm = PesquisaAgenteViewModel(fake)
        advanceUntilIdle()

        assertEquals(3, vm.uiState.value.resultados.size)
        assertEquals(2, vm.uiState.value.listaAgencia.size) // distinct
    }

    @Test
    fun `filtra resultados por agencia no VM`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository().apply { agentes = amostra }
        val vm = PesquisaAgenteViewModel(fake)
        advanceUntilIdle()

        vm.onAgenciaChange("AGENCIA LIT")

        assertEquals(2, vm.uiState.value.resultados.size)
        assertEquals(setOf("Ana", "Carla"), vm.uiState.value.resultados.map { it.descricaoNome }.toSet())
    }

    @Test
    fun `carrega lotacoes distintas`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository().apply { agentes = amostra }
        val vm = PesquisaAgenteViewModel(fake)
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.listaLotacao.size) // PORTO NORTE, ILHA CENTRAL
    }

    @Test
    fun `filtra por lotacao (dropdown, match exato)`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository().apply { agentes = amostra }
        val vm = PesquisaAgenteViewModel(fake)
        advanceUntilIdle()

        vm.onLotacaoChange("PORTO NORTE")

        assertEquals(setOf("Ana", "Carla"), vm.uiState.value.resultados.map { it.descricaoNome }.toSet())
    }

    @Test
    fun `combina filtro de agencia e lotacao`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository().apply { agentes = amostra }
        val vm = PesquisaAgenteViewModel(fake)
        advanceUntilIdle()

        vm.onAgenciaChange("AGENCIA MARE")
        vm.onLotacaoChange("PORTO NORTE")

        // Bruno é MARE mas lotação ILHA CENTRAL → nenhum casa.
        assertEquals(0, vm.uiState.value.resultados.size)
    }

    @Test
    fun `deletar remove o agente e recarrega os resultados`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository().apply { agentes = amostra }
        val vm = PesquisaAgenteViewModel(fake)
        advanceUntilIdle()

        vm.onDeletar("1")
        advanceUntilIdle()

        assertTrue(fake.deletados.contains("1"))
        assertEquals(2, vm.uiState.value.resultados.size)
        assertNull(vm.uiState.value.resultados.find { it.id == "1" })
    }
}
