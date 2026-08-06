package dev.matheus.fluviapp.ui.viewmodel.porto

import androidx.lifecycle.SavedStateHandle
import dev.matheus.fluviapp.domain.localidade.Localidade
import dev.matheus.fluviapp.domain.localidade.Uf
import dev.matheus.fluviapp.domain.porto.Porto
import dev.matheus.fluviapp.fakes.FakeLocalidadeRepository
import dev.matheus.fluviapp.fakes.FakePortoRepository
import dev.matheus.fluviapp.ui.states.ErroNomePorto
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
class FormPortoViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val belem = Localidade("belem", "Belém", Uf.PA, "1501402")
    private val parintins = Localidade("parintins", "Parintins", Uf.AM, "1303205")

    /** Sem argumentos, as duas de sempre — é o cenário de quase todos os casos. */
    private fun localidades(vararg lista: Localidade) = FakeLocalidadeRepository().apply {
        localidades = if (lista.isEmpty()) listOf(belem, parintins) else lista.toList()
    }

    private fun viewModel(
        portos: FakePortoRepository = FakePortoRepository(),
        locais: FakeLocalidadeRepository = localidades(),
        idPorto: String? = null,
    ) = FormPortoViewModel(
        portos,
        locais,
        if (idPorto == null) SavedStateHandle() else SavedStateHandle(mapOf("idPorto" to idPorto)),
    )

    // --- As fontes ---

    @Test
    fun `oferece as localidades ativas, em ordem de rotulo`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(listOf("Belém/PA", "Parintins/AM"), vm.uiState.value.localidades.map { it.rotulo })
    }

    /** Inativar um município é dizer "não escolham mais este" — e a lista de escolha é onde isso vale. */
    @Test
    fun `localidade inativa nao entra no dropdown`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(locais = localidades(belem, parintins.copy(ativo = false)))
        advanceUntilIdle()

        assertEquals(listOf("Belém/PA"), vm.uiState.value.localidades.map { it.rotulo })
    }

    // --- Cadastro ---

    @Test
    fun `salvar invalido marca erros e nao persiste`() = runTest(mainRule.dispatcher) {
        val fake = FakePortoRepository()
        val vm = viewModel(fake)
        advanceUntilIdle()

        vm.salvar()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertEquals(ErroNomePorto.OBRIGATORIO, s.erroNome)
        assertTrue(s.isLocalidadeError)
        assertTrue(fake.salvos.isEmpty())
    }

    @Test
    fun `salvar valido persiste a localidade por id e emite sucesso`() = runTest(mainRule.dispatcher) {
        val fake = FakePortoRepository()
        val vm = viewModel(fake)
        val eventos = mutableListOf<Unit>()
        val job = launch { vm.sucesso.toList(eventos) }
        advanceUntilIdle()

        vm.onNomeChange("  Porto de Val-de-Cães  ")
        vm.onLocalidadeChange("Belém/PA")
        vm.salvar()
        advanceUntilIdle()

        val salvo = fake.salvos.single()
        assertEquals("Porto de Val-de-Cães", salvo.nome)
        assertEquals("belem", salvo.localidadeId)
        assertTrue(salvo.ativo)
        assertEquals(1, eventos.size)
        job.cancel()
    }

    /** Rótulo que não casa com opção nenhuma não vira texto guardado: vira "sem localidade". */
    @Test
    fun `rotulo desconhecido nao seleciona nada`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onLocalidadeChange("Santarém/PA")

        assertEquals("", vm.uiState.value.localidadeId)
        assertEquals("", vm.uiState.value.rotuloLocalidade)
    }

    // --- A unicidade (nome, localidade) ---

    @Test
    fun `recusa homonimo na mesma localidade`() = runTest(mainRule.dispatcher) {
        val fake = FakePortoRepository().apply {
            portos = listOf(Porto("p1", "Porto Central", "belem"))
        }
        val vm = viewModel(fake)
        advanceUntilIdle()

        vm.onNomeChange("porto central")
        vm.onLocalidadeChange("Belém/PA")
        vm.salvar()
        advanceUntilIdle()

        assertEquals(ErroNomePorto.DUPLICADO, vm.uiState.value.erroNome)
        assertTrue(fake.salvos.isEmpty())
    }

    /** O erro é do par: trocar de cidade resolve, e a queixa some antes de a pessoa tocar em salvar. */
    @Test
    fun `trocar de localidade limpa a queixa de duplicidade`() = runTest(mainRule.dispatcher) {
        val fake = FakePortoRepository().apply {
            portos = listOf(Porto("p1", "Porto Central", "belem"))
        }
        val vm = viewModel(fake)
        advanceUntilIdle()

        vm.onNomeChange("Porto Central")
        vm.onLocalidadeChange("Belém/PA")
        vm.salvar()
        advanceUntilIdle()
        assertEquals(ErroNomePorto.DUPLICADO, vm.uiState.value.erroNome)

        vm.onLocalidadeChange("Parintins/AM")

        assertEquals(ErroNomePorto.NENHUM, vm.uiState.value.erroNome)
        vm.salvar()
        advanceUntilIdle()
        assertEquals("parintins", fake.salvos.single().localidadeId)
    }

    /** Sem isto, salvar um porto sem renomeá-lo o acusaria de ser duplicata de si mesmo. */
    @Test
    fun `editar sem renomear nao acusa duplicidade de si mesmo`() = runTest(mainRule.dispatcher) {
        val fake = FakePortoRepository().apply {
            portos = listOf(Porto("p1", "Porto Central", "belem"))
        }
        val vm = viewModel(fake, idPorto = "p1")
        advanceUntilIdle()

        vm.salvar()
        advanceUntilIdle()

        assertEquals(ErroNomePorto.NENHUM, vm.uiState.value.erroNome)
        assertEquals("p1", fake.salvos.single().id)
    }

    // --- Edição ---

    @Test
    fun `edicao carrega o porto e resolve o rotulo da localidade`() = runTest(mainRule.dispatcher) {
        val fake = FakePortoRepository().apply {
            portos = listOf(Porto("p1", "Porto de Parintins", "parintins"))
        }
        val vm = viewModel(fake, idPorto = "p1")
        advanceUntilIdle()

        val s = vm.uiState.value
        assertEquals("Porto de Parintins", s.nome)
        assertEquals("parintins", s.localidadeId)
        assertEquals("Parintins/AM", s.rotuloLocalidade)
    }

    /**
     * **O outro lado da regra da porta**: quem lista filtra `ativo`, quem resolve por id não. Sem isto,
     * editar um porto cuja cidade saiu de uso abriria o formulário com o campo vazio — e salvar em
     * seguida trocaria silenciosamente a localidade dele por nenhuma.
     */
    @Test
    fun `edicao mostra a localidade inativa deste porto`() = runTest(mainRule.dispatcher) {
        val fake = FakePortoRepository().apply {
            portos = listOf(Porto("p1", "Porto de Parintins", "parintins"))
        }
        val vm = viewModel(fake, locais = localidades(belem, parintins.copy(ativo = false)), idPorto = "p1")
        advanceUntilIdle()

        val s = vm.uiState.value
        assertEquals("Parintins/AM", s.rotuloLocalidade)
        assertTrue("a inativa deste porto tinha de estar entre as opções", "Parintins/AM" in s.localidades.map { it.rotulo })

        vm.salvar()
        advanceUntilIdle()
        assertEquals("parintins", fake.salvos.single().localidadeId)
    }

    // --- Falha de escrita ---

    @Test
    fun `falha ao salvar destrava o botao e nao emite sucesso`() = runTest(mainRule.dispatcher) {
        val fake = FakePortoRepository().apply { falharAoSalvar = true }
        val vm = viewModel(fake)
        val eventos = mutableListOf<Unit>()
        val job = launch { vm.sucesso.toList(eventos) }
        advanceUntilIdle()

        vm.onNomeChange("Porto Central")
        vm.onLocalidadeChange("Belém/PA")
        vm.salvar()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isProcessing)
        assertTrue(eventos.isEmpty())
        job.cancel()
    }
}