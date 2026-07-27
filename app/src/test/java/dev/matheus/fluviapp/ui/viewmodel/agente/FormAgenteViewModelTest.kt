package dev.matheus.fluviapp.ui.viewmodel.agente

import androidx.lifecycle.SavedStateHandle
import dev.matheus.fluviapp.fakes.FakeFuncionarioRepository
import dev.matheus.fluviapp.fakes.FakeConstanteRepository
import dev.matheus.fluviapp.model.cadastro.constantes.Constante
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Categoria.MUNICIPIO
import dev.matheus.fluviapp.model.operacoes.Funcionario
import dev.matheus.fluviapp.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FormAgenteViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private fun constanteFake() = FakeConstanteRepository().apply {
        constantes = listOf(Constante("1", "PORTO NORTE", MUNICIPIO.name))
    }

    @Test
    fun `salvar invalido marca erros e nao persiste`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository()
        val vm = FormAgenteViewModel(fake, constanteFake(), SavedStateHandle())
        advanceUntilIdle()

        vm.salvar()

        val s = vm.uiState.value
        assertTrue(s.isAgenciaError)
        assertTrue(s.isAgenteError)
        assertTrue(s.isLotacaoError)
        assertTrue(fake.salvos.isEmpty())
    }

    @Test
    fun `criar persiste agente novo e emite sucesso`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository()
        val vm = FormAgenteViewModel(fake, constanteFake(), SavedStateHandle())
        advanceUntilIdle()
        val eventos = mutableListOf<Unit>()
        val job = launch { vm.sucesso.toList(eventos) }

        vm.onAgenciaChange("NOVA")
        vm.onAgenteChange("Ana")
        vm.onLotacaoChange("PORTO NORTE")
        vm.salvar()
        advanceUntilIdle()

        assertEquals(1, fake.salvos.size)
        assertEquals("", fake.salvos.first().id)
        assertEquals("Ana", fake.salvos.first().descricaoNome)
        assertEquals(1, eventos.size)
        job.cancel()
    }

    @Test
    fun `editar atualiza campos e preserva o id do agente persistido`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository().apply {
            agentes = listOf(Funcionario("a1", "Ana", "MATRIZ", "PORTO NORTE"))
        }
        val vm = FormAgenteViewModel(fake, constanteFake(), SavedStateHandle(mapOf("idAgente" to "a1")))
        advanceUntilIdle()
        assertEquals("Ana", vm.uiState.value.agente)

        vm.onAgenteChange("Ana Maria")
        vm.salvar()
        advanceUntilIdle()

        val salvo = fake.salvos.first()
        assertEquals("a1", salvo.id)
        assertEquals("Ana Maria", salvo.descricaoNome)
        // Campos carregados e não mexidos voltam como estavam (o form parte do persistido).
        assertEquals("MATRIZ", salvo.agencia)
    }
}
