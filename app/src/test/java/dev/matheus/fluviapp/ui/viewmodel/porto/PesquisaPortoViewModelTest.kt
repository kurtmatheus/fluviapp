package dev.matheus.fluviapp.ui.viewmodel.porto

import dev.matheus.fluviapp.domain.localidade.Localidade
import dev.matheus.fluviapp.domain.localidade.Uf
import dev.matheus.fluviapp.domain.porto.Porto
import dev.matheus.fluviapp.fakes.FakeLocalidadeRepository
import dev.matheus.fluviapp.fakes.FakePortoRepository
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
class PesquisaPortoViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val belem = Localidade("belem", "Belém", Uf.PA, "1501402")
    private val parintins = Localidade("parintins", "Parintins", Uf.AM, "1303205")

    private val valDeCaes = Porto("p1", "Porto de Val-de-Cães", "belem")
    private val portoDeParintins = Porto("p2", "Porto de Parintins", "parintins")
    private val portoCentral = Porto("p3", "Porto Central", "belem")

    private fun portos() = FakePortoRepository().apply {
        portos = listOf(valDeCaes, portoDeParintins, portoCentral)
    }

    private fun locais() = FakeLocalidadeRepository().apply {
        localidades = listOf(belem, parintins)
    }

    @Test
    fun `junta as duas colecoes e entrega o rotulo pronto`() = runTest(mainRule.dispatcher) {
        val vm = PesquisaPortoViewModel(portos(), locais())
        advanceUntilIdle()

        val resultados = vm.uiState.value.resultados
        assertEquals(3, resultados.size)
        assertEquals("Belém/PA", resultados.first { it.id == "p1" }.rotuloLocalidade)
        assertEquals("Parintins/AM", resultados.first { it.id == "p2" }.rotuloLocalidade)
    }

    @Test
    fun `liga os dois listeners — sem isso os StateFlows reais ficam vazios`() = runTest(mainRule.dispatcher) {
        val portos = portos()
        val locais = locais()
        PesquisaPortoViewModel(portos, locais)
        advanceUntilIdle()

        assertTrue(portos.sincronizou)
        assertTrue(locais.sincronizou)
    }

    @Test
    fun `filtra por inicio do nome do porto, ignorando caixa`() = runTest(mainRule.dispatcher) {
        val vm = PesquisaPortoViewModel(portos(), locais())
        advanceUntilIdle()

        vm.onNomeChange("porto de")

        assertEquals(setOf("p1", "p2"), vm.uiState.value.resultados.map { it.id }.toSet())
    }

    /** O delete lógico visto de fora: some da lista, e o documento continua lá. */
    @Test
    fun `excluir some da lista mas nao apaga o documento`() = runTest(mainRule.dispatcher) {
        val portos = portos()
        val vm = PesquisaPortoViewModel(portos, locais())
        advanceUntilIdle()

        vm.onDeletar("p1")
        advanceUntilIdle()

        assertNull(vm.uiState.value.resultados.find { it.id == "p1" })
        assertEquals(2, vm.uiState.value.resultados.size)

        val guardado = portos.obterPorId("p1")
        assertEquals("Porto de Val-de-Cães", guardado?.nome)
        assertEquals(false, guardado?.ativo)
    }

    @Test
    fun `porto inativo nao entra na lista`() = runTest(mainRule.dispatcher) {
        val portos = FakePortoRepository().apply {
            portos = listOf(valDeCaes, portoDeParintins.copy(ativo = false))
        }
        val vm = PesquisaPortoViewModel(portos, locais())
        advanceUntilIdle()

        assertEquals(listOf("p1"), vm.uiState.value.resultados.map { it.id })
    }

    /**
     * **Localidade inativa continua sendo o lugar do porto.** Aqui se resolve por id, e a regra da porta
     * diz que quem resolve por id não filtra — senão desativar um município apagaria da tela o lugar dos
     * portos que continuam operando.
     */
    @Test
    fun `localidade inativa ainda rotula os portos dela`() = runTest(mainRule.dispatcher) {
        val locais = FakeLocalidadeRepository().apply {
            localidades = listOf(belem, parintins.copy(ativo = false))
        }
        val vm = PesquisaPortoViewModel(portos(), locais)
        advanceUntilIdle()

        assertEquals("Parintins/AM", vm.uiState.value.resultados.first { it.id == "p2" }.rotuloLocalidade)
    }

    /** Referência que não resolve deixa a linha **sem lugar** — nunca com um lugar inventado. */
    @Test
    fun `porto de localidade desconhecida aparece sem rotulo`() = runTest(mainRule.dispatcher) {
        val portos = FakePortoRepository().apply {
            portos = listOf(Porto("p9", "Porto Órfão", "nao-existe"))
        }
        val vm = PesquisaPortoViewModel(portos, locais())
        advanceUntilIdle()

        assertEquals("", vm.uiState.value.resultados.single().rotuloLocalidade)
    }

    /**
     * A junção é reativa dos **dois** lados: corrigir a grafia do município repinta a lista de portos
     * sem que ninguém recarregue nada. É o ganho que a cópia embutida no porto não teria.
     */
    @Test
    fun `corrigir a localidade repinta a lista de portos`() = runTest(mainRule.dispatcher) {
        val locais = locais()
        val vm = PesquisaPortoViewModel(portos(), locais)
        advanceUntilIdle()

        locais.localidades = listOf(belem.copy(municipio = "Belém do Pará"), parintins)
        advanceUntilIdle()

        assertEquals("Belém do Pará/PA", vm.uiState.value.resultados.first { it.id == "p1" }.rotuloLocalidade)
    }
}