package dev.matheus.fluviapp.ui.viewmodel.viagem

import androidx.lifecycle.SavedStateHandle
import dev.matheus.fluviapp.fakes.FakeConstanteRepository
import dev.matheus.fluviapp.fakes.FakeEmpresaRepository
import dev.matheus.fluviapp.fakes.FakeNavioRepository
import dev.matheus.fluviapp.fakes.FakeViagemRepository
import dev.matheus.fluviapp.model.cadastro.constantes.Constante
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Categoria.MUNICIPIO
import dev.matheus.fluviapp.model.mappers.ViagemDadosViagemMapper
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
class FormViagemViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val fakeEmpresa = FakeEmpresaRepository().apply {
        empresas = listOf(Empresa("e1", "ACME", "ACME LTDA", "1", "end", "1", "2"))
    }
    private val fakeNavio = FakeNavioRepository().apply {
        navios = listOf(
            Navio("n1", "F/B", 10, 2, 2, 2, "e1"),
            Navio("n2", "Outro", 5, 1, 1, 1, "e2"),
        )
    }
    private val fakeConstante = FakeConstanteRepository().apply {
        constantes = listOf(
            Constante("1", "Porto Norte", MUNICIPIO.name),
            Constante("2", "Ilha Central", MUNICIPIO.name),
        )
    }
    private val fakeViagem = FakeViagemRepository()
    private val mapper = ViagemDadosViagemMapper(fakeEmpresa, fakeNavio, fakeConstante)

    private fun viewModel() =
        FormViagemViewModel(fakeEmpresa, fakeNavio, fakeConstante, fakeViagem, mapper, SavedStateHandle())

    @Test
    fun `carrega empresas e municipios`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.listaEmpresas.size)
        assertEquals(2, vm.uiState.value.listaMunicipios.size)
    }

    @Test
    fun `onEmpresaChange carrega navios da empresa e habilita navio`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEmpresaChange("ACME")
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.listaNavios.size)
        assertEquals("F/B", vm.uiState.value.listaNavios.first().descricaoNome)
        assertFalse(vm.uiState.value.navioDesabilitado)
    }

    @Test
    fun `onEmpresaChange com empresa fora da lista nao lista navios`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEmpresaChange("FANTASMA") // não existe em listaEmpresas → sem id
        advanceUntilIdle()

        assertTrue(vm.uiState.value.listaNavios.isEmpty())
    }

    @Test
    fun `salvar invalido valida empresa`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.salvar()

        assertTrue(vm.uiState.value.isEmpresaError)
        assertTrue(vm.uiState.value.isNavioError)
        assertTrue(fakeViagem.salvos.isEmpty())
    }

    @Test
    fun `salvar valido persiste e emite sucesso`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        val eventos = mutableListOf<Unit>()
        val job = launch { vm.sucesso.toList(eventos) }

        vm.onEmpresaChange("ACME")
        advanceUntilIdle()
        vm.onNavioChange("F/B")
        vm.onTrechoOrigemChange("Porto Norte")
        vm.onTrechoDestinoChange("Ilha Central")
        vm.salvar()
        advanceUntilIdle()

        assertEquals(1, fakeViagem.salvos.size)
        val salvo = fakeViagem.salvos.first()
        assertEquals("", salvo.id) // criação → id vazio; auto-id acontece na impl real
        assertEquals("Porto Norte", salvo.origem)
        assertEquals("Ilha Central", salvo.destino)
        assertEquals("e1", salvo.empresaId) // ADR-0008 Fase 3: vínculo só por id (nomes não persistem)
        assertEquals("n1", salvo.navioId)
        assertEquals(1, eventos.size)
        job.cancel()
    }
}
