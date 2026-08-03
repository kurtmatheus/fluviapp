package dev.matheus.fluviapp.ui.viewmodel.viagem

import dev.matheus.fluviapp.fakes.FakeConstanteRepository
import dev.matheus.fluviapp.fakes.FakeEmpresaRepository
import dev.matheus.fluviapp.fakes.FakeNavioRepository
import dev.matheus.fluviapp.fakes.FakeViagemRepository
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Categoria.MUNICIPIO
import dev.matheus.fluviapp.domain.mappers.ViagemDadosViagemMapper
import dev.matheus.fluviapp.domain.viagem.Empresa
import dev.matheus.fluviapp.domain.viagem.Navio
import dev.matheus.fluviapp.domain.viagem.Viagem
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
class PesquisarViagemViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val fakeEmpresa = FakeEmpresaRepository().apply {
        empresas = listOf(Empresa("e1", "ACME", "ACME LTDA", "1", "end", "1", "2"))
    }
    private val fakeNavio = FakeNavioRepository().apply {
        navios = listOf(Navio("n1", "F/B", 10, 2, 2, 2, "e1"))
    }
    private val fakeConstante = FakeConstanteRepository().apply {
        constantes = listOf(
            Constante("1", "Porto Norte", MUNICIPIO.name),
            Constante("2", "Ilha Central", MUNICIPIO.name),
        )
    }
    private val fakeViagem = FakeViagemRepository().apply {
        // Viagem relaciona por id (ADR-0008 Fase 3): empresaId "e1" (ACME), navioId "n1" (F/B).
        viagens = listOf(Viagem("v1", "COD", "Porto Norte", "Ilha Central", "e1", "n1"))
    }
    private val mapper = ViagemDadosViagemMapper(fakeEmpresa, fakeNavio, fakeConstante)

    private fun viewModel() =
        PesquisarViagemViewModel(fakeEmpresa, fakeNavio, fakeConstante, fakeViagem, mapper)

    @Test
    fun `carrega fontes de filtro`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.listaEmpresas.size)
        assertEquals(1, vm.uiState.value.listaNavios.size)
        assertEquals(2, vm.uiState.value.listaMunicipios.size)
    }

    @Test
    fun `filtro marcado sem valor invalida e nao navega`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        val eventos = mutableListOf<Unit>()
        val job = launch { vm.irParaResultados.toList(eventos) }

        vm.onCheckEmpresa() // marca empresa, mas empresa está em branco
        vm.pesquisar()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isEmpresaError)
        assertTrue(eventos.isEmpty())
        job.cancel()
    }

    @Test
    fun `pesquisa valida filtra e emite ir-para-resultados`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        val eventos = mutableListOf<Unit>()
        val job = launch { vm.irParaResultados.toList(eventos) }

        vm.onCheckEmpresa()
        vm.onEmpresaChange("ACME")
        vm.pesquisar()
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.listaResultadoViagens.size)
        assertEquals("ACME", vm.uiState.value.listaResultadoViagens.first().empresa)
        assertEquals(1, eventos.size)
        job.cancel()
    }

    @Test
    fun `deletar com sucesso emite true e fecha o dialogo`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.exibirConfirmDeleteDialog() // abre
        val resultados = mutableListOf<Boolean>()
        val job = launch { vm.exclusao.toList(resultados) }

        vm.deletarViagem("v1")
        advanceUntilIdle()

        assertEquals(listOf(true), resultados)
        assertFalse(vm.uiState.value.isShowDeleteDialog)
        assertTrue(fakeViagem.deletados.contains("v1"))
        job.cancel()
    }

    @Test
    fun `deletar remove a viagem da lista de resultados`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onCheckEmpresa()
        vm.onEmpresaChange("ACME")
        vm.pesquisar()
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.listaResultadoViagens.size)

        vm.deletarViagem("v1")
        advanceUntilIdle()

        // reload pós-deleção reaplica o filtro sobre a lista já sem a viagem removida.
        assertTrue(vm.uiState.value.listaResultadoViagens.isEmpty())
        assertTrue(fakeViagem.deletados.contains("v1"))
    }

    @Test
    fun `deletar com falha emite false e nao reporta sucesso`() = runTest(mainRule.dispatcher) {
        fakeViagem.falharAoDeletar = true
        val vm = viewModel()
        advanceUntilIdle()
        val resultados = mutableListOf<Boolean>()
        val job = launch { vm.exclusao.toList(resultados) }

        vm.deletarViagem("v1")
        advanceUntilIdle()

        assertEquals(listOf(false), resultados)
        job.cancel()
    }
}
