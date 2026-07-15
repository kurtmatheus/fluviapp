package dev.matheus.fluviapp.ui.viewmodel.navio

import androidx.lifecycle.SavedStateHandle
import dev.matheus.fluviapp.fakes.FakeEmpresaRepository
import dev.matheus.fluviapp.fakes.FakeNavioRepository
import dev.matheus.fluviapp.model.viagem.Empresa
import dev.matheus.fluviapp.model.viagem.Navio
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
class FormNavioViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private fun empresaFake() = FakeEmpresaRepository().apply {
        empresas = listOf(Empresa("e1", "ACME", "ACME LTDA", "11222333000181", "Rua 1", "111", "222"))
    }

    @Test
    fun `salvar invalido marca erros e nao persiste`() = runTest(mainRule.dispatcher) {
        val navioFake = FakeNavioRepository()
        val vm = FormNavioViewModel(navioFake, empresaFake(), SavedStateHandle())

        vm.salvar()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertTrue(s.isNomeError)
        assertTrue(s.isEmpresaError)
        assertTrue(navioFake.salvos.isEmpty())
    }

    @Test
    fun `salvar valido persiste com capacidades parseadas e emite sucesso`() = runTest(mainRule.dispatcher) {
        val navioFake = FakeNavioRepository()
        val vm = FormNavioViewModel(navioFake, empresaFake(), SavedStateHandle())
        val eventos = mutableListOf<Unit>()
        val job = launch { vm.sucesso.toList(eventos) }

        vm.onNomeChange("FLUVI I")
        vm.onEmpresaChange("ACME")
        vm.onCapacidadeVeiculoChange("12")
        vm.onCapacidadeCamaroteChange("abc8") // filtra dígitos -> "8"
        vm.salvar()
        advanceUntilIdle()

        assertEquals(1, navioFake.salvos.size)
        val salvo = navioFake.salvos.first()
        assertEquals("FLUVI I", salvo.descricaoNome)
        assertEquals("e1", salvo.empresaId) // ADR-0008: link resolvido do nome selecionado
        assertEquals(12, salvo.capacidadeVeiculo)
        assertEquals(8, salvo.capacidadeCamarote)
        assertEquals(0, salvo.capacidadeSuite2) // em branco -> 0
        assertEquals(1, eventos.size)
        job.cancel()
    }

    @Test
    fun `empresa sem match na lista salva com empresaId vazio (dormente, nao quebra)`() = runTest(mainRule.dispatcher) {
        val navioFake = FakeNavioRepository()
        val vm = FormNavioViewModel(navioFake, empresaFake(), SavedStateHandle())

        vm.onNomeChange("FLUVI I")
        vm.onEmpresaChange("EMPRESA FANTASMA") // não existe em listaEmpresas
        vm.salvar()
        advanceUntilIdle()

        assertEquals(1, navioFake.salvos.size)
        assertEquals("", navioFake.salvos.first().empresaId)
    }

    @Test
    fun `falha ao salvar nao emite sucesso e libera processamento`() = runTest(mainRule.dispatcher) {
        val navioFake = FakeNavioRepository().apply { falharAoSalvar = true }
        val vm = FormNavioViewModel(navioFake, empresaFake(), SavedStateHandle())
        val eventos = mutableListOf<Unit>()
        val job = launch { vm.sucesso.toList(eventos) }

        vm.onNomeChange("FLUVI I")
        vm.onEmpresaChange("ACME")
        vm.salvar()
        advanceUntilIdle()

        assertTrue(eventos.isEmpty())
        assertFalse(vm.uiState.value.isProcessing)
        job.cancel()
    }

    @Test
    fun `edicao carrega navio existente`() = runTest(mainRule.dispatcher) {
        val navioFake = FakeNavioRepository().apply {
            navios = listOf(Navio("n1", "FLUVI I", 10, 20, 30, 5, "e1")) // vínculo por id
        }
        val vm = FormNavioViewModel(navioFake, empresaFake(), SavedStateHandle(mapOf("idNavio" to "n1")))
        advanceUntilIdle()

        val s = vm.uiState.value
        assertEquals("FLUVI I", s.nome)
        assertEquals("ACME", s.empresa) // ADR-0008: nome resolvido de volta do empresaId "e1"
        assertEquals("10", s.capacidadeVeiculo)
        assertEquals("5", s.capacidadeCamarote)
    }
}
