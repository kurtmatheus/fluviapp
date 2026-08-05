package dev.matheus.fluviapp.ui.viewmodel.embarcacao

import androidx.lifecycle.SavedStateHandle
import dev.matheus.fluviapp.fakes.FakeEmpresaRepository
import dev.matheus.fluviapp.fakes.FakeEmbarcacaoRepository
import dev.matheus.fluviapp.domain.viagem.Empresa
import dev.matheus.fluviapp.domain.viagem.Embarcacao
import dev.matheus.fluviapp.domain.viagem.TipoEmbarcacao
import dev.matheus.fluviapp.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FormEmbarcacaoViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private fun empresaFake() = FakeEmpresaRepository().apply {
        empresas = listOf(Empresa("e1", "ACME", "ACME LTDA", "11222333000181", "Rua 1", "111", "222"))
    }

    @Test
    fun `salvar invalido marca erros e nao persiste`() = runTest(mainRule.dispatcher) {
        val embarcacaoFake = FakeEmbarcacaoRepository()
        val vm = FormEmbarcacaoViewModel(embarcacaoFake, empresaFake(), SavedStateHandle())

        vm.salvar()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertTrue(s.isNomeError)
        assertTrue(s.isEmpresaError)
        assertTrue(s.isTipoError)
        assertTrue(embarcacaoFake.salvos.isEmpty())
    }

    @Test
    fun `salvar valido persiste com capacidades parseadas e emite sucesso`() = runTest(mainRule.dispatcher) {
        val embarcacaoFake = FakeEmbarcacaoRepository()
        val vm = FormEmbarcacaoViewModel(embarcacaoFake, empresaFake(), SavedStateHandle())
        val eventos = mutableListOf<Unit>()
        val job = launch { vm.sucesso.toList(eventos) }

        vm.onNomeChange("FLUVI I")
        vm.onTipoChange("Ferry Boat")
        vm.onEmpresaChange("ACME")
        vm.onCapacidadeVeiculoChange("12")
        vm.onCapacidadeCamaroteChange("abc8") // filtra dígitos -> "8"
        vm.salvar()
        advanceUntilIdle()

        assertEquals(1, embarcacaoFake.salvos.size)
        val salvo = embarcacaoFake.salvos.first()
        assertEquals("FLUVI I", salvo.descricaoNome)
        assertEquals(TipoEmbarcacao.FERRY_BOAT, salvo.tipo) // rótulo escolhido, tipo persistido
        assertEquals("e1", salvo.empresaId) // ADR-0008: link resolvido do nome selecionado
        assertEquals(12, salvo.capacidadeVeiculo)
        assertEquals(8, salvo.capacidadeCamarote)
        assertEquals(0, salvo.capacidadeSuite2) // em branco -> 0
        assertEquals(1, eventos.size)
        job.cancel()
    }

    /** A tela devolve rótulo; o domínio recebe tipo. Rótulo que não existe não vira tipo nenhum. */
    @Test
    fun `o tipo entra pelo rotulo e rotulo desconhecido nao seleciona nada`() = runTest(mainRule.dispatcher) {
        val vm = FormEmbarcacaoViewModel(FakeEmbarcacaoRepository(), empresaFake(), SavedStateHandle())

        vm.onTipoChange("Lancha")
        assertEquals(TipoEmbarcacao.LANCHA, vm.uiState.value.tipo)

        vm.onTipoChange("Catamarã")
        assertNull(vm.uiState.value.tipo)
    }

    /**
     * A contradição não sobrevive à troca de tipo: quem digitou vagas de carro e depois escolheu a lancha
     * não fica com o número guardado num campo que a tela não mostra mais.
     */
    @Test
    fun `trocar para um tipo sem veiculo apaga a capacidade de veiculo`() = runTest(mainRule.dispatcher) {
        val embarcacaoFake = FakeEmbarcacaoRepository()
        val vm = FormEmbarcacaoViewModel(embarcacaoFake, empresaFake(), SavedStateHandle())

        vm.onNomeChange("LANCHA VELOZ")
        vm.onEmpresaChange("ACME")
        vm.onTipoChange("Ferry Boat")
        vm.onCapacidadeVeiculoChange("12")
        vm.onTipoChange("Lancha")

        assertEquals("", vm.uiState.value.capacidadeVeiculo)
        assertFalse(vm.uiState.value.perguntaCapacidadeVeiculo)

        vm.salvar()
        advanceUntilIdle()

        assertEquals(0, embarcacaoFake.salvos.first().capacidadeVeiculo)
    }

    /** Trocar entre dois tipos que levam veículo preserva o que já foi digitado. */
    @Test
    fun `trocar entre tipos que levam veiculo preserva a capacidade`() = runTest(mainRule.dispatcher) {
        val vm = FormEmbarcacaoViewModel(FakeEmbarcacaoRepository(), empresaFake(), SavedStateHandle())

        vm.onTipoChange("Ferry Boat")
        vm.onCapacidadeVeiculoChange("12")
        vm.onTipoChange("Navio")

        assertEquals("12", vm.uiState.value.capacidadeVeiculo)
    }

    @Test
    fun `empresa sem match na lista salva com empresaId vazio (dormente, nao quebra)`() = runTest(mainRule.dispatcher) {
        val embarcacaoFake = FakeEmbarcacaoRepository()
        val vm = FormEmbarcacaoViewModel(embarcacaoFake, empresaFake(), SavedStateHandle())

        vm.onNomeChange("FLUVI I")
        vm.onTipoChange("Ferry Boat")
        vm.onEmpresaChange("EMPRESA FANTASMA") // não existe em listaEmpresas
        vm.salvar()
        advanceUntilIdle()

        assertEquals(1, embarcacaoFake.salvos.size)
        assertEquals("", embarcacaoFake.salvos.first().empresaId)
    }

    @Test
    fun `falha ao salvar nao emite sucesso e libera processamento`() = runTest(mainRule.dispatcher) {
        val embarcacaoFake = FakeEmbarcacaoRepository().apply { falharAoSalvar = true }
        val vm = FormEmbarcacaoViewModel(embarcacaoFake, empresaFake(), SavedStateHandle())
        val eventos = mutableListOf<Unit>()
        val job = launch { vm.sucesso.toList(eventos) }

        vm.onNomeChange("FLUVI I")
        vm.onTipoChange("Ferry Boat")
        vm.onEmpresaChange("ACME")
        vm.salvar()
        advanceUntilIdle()

        assertTrue(eventos.isEmpty())
        assertFalse(vm.uiState.value.isProcessing)
        job.cancel()
    }

    @Test
    fun `edicao carrega embarcacao existente`() = runTest(mainRule.dispatcher) {
        val embarcacaoFake = FakeEmbarcacaoRepository().apply {
            // vínculo por id
            embarcacoes = listOf(Embarcacao("n1", "FLUVI I", TipoEmbarcacao.FERRY_BOAT, 10, 20, 30, 5, "e1"))
        }
        val vm = FormEmbarcacaoViewModel(embarcacaoFake, empresaFake(), SavedStateHandle(mapOf("idEmbarcacao" to "n1")))
        advanceUntilIdle()

        val s = vm.uiState.value
        assertEquals("FLUVI I", s.nome)
        assertEquals(TipoEmbarcacao.FERRY_BOAT, s.tipo)
        assertEquals("ACME", s.empresa) // ADR-0008: nome resolvido de volta do empresaId "e1"
        assertEquals("10", s.capacidadeVeiculo)
        assertEquals("5", s.capacidadeCamarote)
    }

    /** Editar uma lancha não mostra — nem oferece — a capacidade de veículos. */
    @Test
    fun `edicao de lancha nao pergunta capacidade de veiculo`() = runTest(mainRule.dispatcher) {
        val embarcacaoFake = FakeEmbarcacaoRepository().apply {
            embarcacoes = listOf(Embarcacao("n2", "LANCHA VELOZ", TipoEmbarcacao.LANCHA, 0, 0, 0, 0, "e1"))
        }
        val vm = FormEmbarcacaoViewModel(embarcacaoFake, empresaFake(), SavedStateHandle(mapOf("idEmbarcacao" to "n2")))
        advanceUntilIdle()

        assertEquals(TipoEmbarcacao.LANCHA, vm.uiState.value.tipo)
        assertFalse(vm.uiState.value.perguntaCapacidadeVeiculo)
    }
}
