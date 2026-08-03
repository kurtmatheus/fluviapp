package dev.matheus.fluviapp.ui.viewmodel.empresa

import dev.matheus.fluviapp.fakes.FakeEmpresaRepository
import dev.matheus.fluviapp.domain.viagem.Empresa
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
class PesquisaEmpresaViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private fun empresa(id: String, nome: String) = Empresa(id, nome, "$nome LTDA", id, "end", "1", "2")

    private val amostra = listOf(
        empresa("1", "NAVEGA MODELO"),
        empresa("2", "TRANSPORTE ILHA"),
        empresa("3", "NAVEGA SUL"),
    )

    @Test
    fun `carrega todas as empresas`() = runTest(mainRule.dispatcher) {
        val fake = FakeEmpresaRepository().apply { empresas = amostra }
        val vm = PesquisaEmpresaViewModel(fake)
        advanceUntilIdle()

        assertEquals(3, vm.uiState.value.resultados.size)
    }

    @Test
    fun `filtra por nome com startsWith ignore case`() = runTest(mainRule.dispatcher) {
        val fake = FakeEmpresaRepository().apply { empresas = amostra }
        val vm = PesquisaEmpresaViewModel(fake)
        advanceUntilIdle()

        vm.onNomeChange("nav") // minúsculo, prefixo

        assertEquals(setOf("NAVEGA MODELO", "NAVEGA SUL"), vm.uiState.value.resultados.map { it.nome }.toSet())
    }

    @Test
    fun `deletar remove a empresa e a lista encolhe pelo snapshot`() = runTest(mainRule.dispatcher) {
        val fake = FakeEmpresaRepository().apply { empresas = amostra }
        val vm = PesquisaEmpresaViewModel(fake)
        advanceUntilIdle()

        vm.onDeletar("1")
        advanceUntilIdle()

        assertTrue(fake.deletados.contains("1"))
        assertEquals(2, vm.uiState.value.resultados.size)
        assertNull(vm.uiState.value.resultados.find { it.id == "1" })
    }

    /**
     * O que a fonte reativa compra (ADR-0017 D1): dado novo chegando do listener aparece na tela sem que
     * ninguém peça. É o caso que o `obterTodas()` de uma vez não atendia — voltar de um cadastro deixava
     * a lista velha até alguém recarregar.
     */
    @Test
    fun `empresa cadastrada depois aparece sem recarregar`() = runTest(mainRule.dispatcher) {
        val fake = FakeEmpresaRepository().apply { empresas = amostra }
        val vm = PesquisaEmpresaViewModel(fake)
        advanceUntilIdle()

        fake.empresas = amostra + empresa("4", "NAVEGA NORTE")
        advanceUntilIdle()

        assertEquals(4, vm.uiState.value.resultados.size)
    }

    /** O filtro digitado sobrevive ao snapshot: chegada de dado não desfaz o que o usuário buscou. */
    @Test
    fun `snapshot novo respeita o filtro em vigor`() = runTest(mainRule.dispatcher) {
        val fake = FakeEmpresaRepository().apply { empresas = amostra }
        val vm = PesquisaEmpresaViewModel(fake)
        advanceUntilIdle()
        vm.onNomeChange("nav")

        fake.empresas = amostra + empresa("4", "TRANSPORTE NORTE")
        advanceUntilIdle()

        assertEquals("nav", vm.uiState.value.nome)
        assertEquals(
            setOf("NAVEGA MODELO", "NAVEGA SUL"),
            vm.uiState.value.resultados.map { it.nome }.toSet(),
        )
    }

    /** Sem ligar o listener, o StateFlow fica vazio para sempre — o `obterTodas()` fazia isso de carona. */
    @Test
    fun `a busca liga a sincronizacao da colecao`() = runTest(mainRule.dispatcher) {
        val fake = FakeEmpresaRepository()

        PesquisaEmpresaViewModel(fake)
        advanceUntilIdle()

        assertTrue(fake.sincronizou)
    }
}
