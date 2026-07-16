package dev.matheus.fluviapp.ui.viewmodel.navio

import dev.matheus.fluviapp.fakes.FakeEmpresaRepository
import dev.matheus.fluviapp.fakes.FakeNavioRepository
import dev.matheus.fluviapp.model.viagem.Empresa
import dev.matheus.fluviapp.model.viagem.Navio
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
class PesquisaNavioViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val empresas = listOf(
        Empresa("e1", "NAVEGA MODELO", "Navega LTDA", "1", "end", "1", "2"),
        Empresa("e2", "TRANSPORTE ILHA", "Ilha SA", "2", "end", "1", "2"),
    )
    private val navios = listOf(
        Navio("n1", "F/B A", 10, 2, 2, 2, "e1"),
        Navio("n2", "F/B B", 5, 1, 1, 1, "e2"),
        Navio("n3", "F/B C", 8, 1, 1, 1, "e1"),
    )

    private fun vm() = PesquisaNavioViewModel(
        FakeNavioRepository().apply { this.navios = this@PesquisaNavioViewModelTest.navios },
        FakeEmpresaRepository().apply { this.empresas = this@PesquisaNavioViewModelTest.empresas },
    )

    @Test
    fun `carrega navios com nome da empresa resolvido pelo id`() = runTest(mainRule.dispatcher) {
        val vm = vm()
        advanceUntilIdle()

        assertEquals(3, vm.uiState.value.resultados.size)
        assertEquals(2, vm.uiState.value.listaEmpresas.size)
        // n1 tem empresaId e1 → nome resolvido "NAVEGA MODELO".
        assertEquals("NAVEGA MODELO", vm.uiState.value.resultados.first { it.id == "n1" }.empresaNome)
    }

    @Test
    fun `filtra por empresa`() = runTest(mainRule.dispatcher) {
        val vm = vm()
        advanceUntilIdle()

        vm.onEmpresaChange("NAVEGA MODELO")

        assertEquals(setOf("n1", "n3"), vm.uiState.value.resultados.map { it.id }.toSet())
    }

    @Test
    fun `deletar remove o navio e recarrega`() = runTest(mainRule.dispatcher) {
        val fakeNavio = FakeNavioRepository().apply { navios = this@PesquisaNavioViewModelTest.navios }
        val vm = PesquisaNavioViewModel(
            fakeNavio,
            FakeEmpresaRepository().apply { empresas = this@PesquisaNavioViewModelTest.empresas },
        )
        advanceUntilIdle()

        vm.onDeletar("n1")
        advanceUntilIdle()

        assertTrue(fakeNavio.deletados.contains("n1"))
        assertEquals(2, vm.uiState.value.resultados.size)
        assertNull(vm.uiState.value.resultados.find { it.id == "n1" })
    }
}
