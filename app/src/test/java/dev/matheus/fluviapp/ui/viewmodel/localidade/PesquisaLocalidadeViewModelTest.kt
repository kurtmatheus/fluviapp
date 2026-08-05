package dev.matheus.fluviapp.ui.viewmodel.localidade

import dev.matheus.fluviapp.domain.localidade.Localidade
import dev.matheus.fluviapp.domain.localidade.Uf
import dev.matheus.fluviapp.fakes.FakeLocalidadeRepository
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
class PesquisaLocalidadeViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val belem = Localidade("l1", "Belém", Uf.PA, "1501402")
    private val parintins = Localidade("l2", "Parintins", Uf.AM, "1303205")
    private val belaVista = Localidade("l3", "Bela Vista", Uf.MS, "5001904")

    private fun fake() = FakeLocalidadeRepository().apply {
        localidades = listOf(belem, parintins, belaVista)
    }

    @Test
    fun `carrega as localidades com o rotulo pronto`() = runTest(mainRule.dispatcher) {
        val vm = PesquisaLocalidadeViewModel(fake())
        advanceUntilIdle()

        assertEquals(3, vm.uiState.value.resultados.size)
        assertEquals("Belém/PA", vm.uiState.value.resultados.first { it.id == "l1" }.rotulo)
        assertEquals("1501402", vm.uiState.value.resultados.first { it.id == "l1" }.codigoIbge)
    }

    @Test
    fun `liga o listener — sem isso o StateFlow do repositorio real fica vazio`() = runTest(mainRule.dispatcher) {
        val fake = fake()
        PesquisaLocalidadeViewModel(fake)
        advanceUntilIdle()

        assertTrue(fake.sincronizou)
    }

    @Test
    fun `filtra por inicio do nome do municipio, ignorando caixa`() = runTest(mainRule.dispatcher) {
        val vm = PesquisaLocalidadeViewModel(fake())
        advanceUntilIdle()

        vm.onMunicipioChange("bel")

        assertEquals(setOf("l1", "l3"), vm.uiState.value.resultados.map { it.id }.toSet())
    }

    /**
     * **O delete lógico visto de fora**: excluir tira da lista, e o documento continua lá. A asserção
     * dupla é o ponto — se um dia o repositório voltar a apagar de verdade, o primeiro `assert` continuaria
     * passando e só o segundo acusaria.
     */
    @Test
    fun `excluir some da lista mas nao apaga o documento`() = runTest(mainRule.dispatcher) {
        val fake = fake()
        val vm = PesquisaLocalidadeViewModel(fake)
        advanceUntilIdle()

        vm.onDeletar("l1")
        advanceUntilIdle()

        assertNull(vm.uiState.value.resultados.find { it.id == "l1" })
        assertEquals(2, vm.uiState.value.resultados.size)

        val guardada = fake.obterPorId("l1")
        assertEquals("Belém", guardada?.municipio)
        assertEquals(false, guardada?.ativo)
    }

    /** Quem chega pela lista nunca vê inativa — nem quando ela já vem inativa do servidor. */
    @Test
    fun `localidade inativa nao entra na lista`() = runTest(mainRule.dispatcher) {
        val fake = FakeLocalidadeRepository().apply {
            localidades = listOf(belem, parintins.copy(ativo = false))
        }
        val vm = PesquisaLocalidadeViewModel(fake)
        advanceUntilIdle()

        assertEquals(listOf("l1"), vm.uiState.value.resultados.map { it.id })
    }
}