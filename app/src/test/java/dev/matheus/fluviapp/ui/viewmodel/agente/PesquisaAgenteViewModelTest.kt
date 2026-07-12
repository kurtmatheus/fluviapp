package dev.matheus.fluviapp.ui.viewmodel.agente

import dev.matheus.fluviapp.fakes.FakeAgenteRepository
import dev.matheus.fluviapp.model.cadastro.passagem.Agente
import dev.matheus.fluviapp.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PesquisaAgenteViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val amostra = listOf(
        Agente("1", "Ana", "AGENCIA LITORAL", "PORTO NORTE"),
        Agente("2", "Bruno", "AGENCIA MARE", "ILHA CENTRAL"),
        Agente("3", "Carla", "AGENCIA LITORAL", "PORTO NORTE"),
    )

    @Test
    fun `carrega todos os agentes e as agencias`() = runTest(mainRule.dispatcher) {
        val fake = FakeAgenteRepository().apply { agentes = amostra }

        val vm = PesquisaAgenteViewModel(fake)
        advanceUntilIdle()

        assertEquals(3, vm.uiState.value.resultados.size)
        assertEquals(2, vm.uiState.value.listaAgencia.size) // distinct
    }

    @Test
    fun `filtra resultados por agencia no VM`() = runTest(mainRule.dispatcher) {
        val fake = FakeAgenteRepository().apply { agentes = amostra }
        val vm = PesquisaAgenteViewModel(fake)
        advanceUntilIdle()

        vm.onAgenciaChange("AGENCIA LIT")

        assertEquals(2, vm.uiState.value.resultados.size)
        assertEquals(setOf("Ana", "Carla"), vm.uiState.value.resultados.map { it.descricaoNome }.toSet())
    }
}
