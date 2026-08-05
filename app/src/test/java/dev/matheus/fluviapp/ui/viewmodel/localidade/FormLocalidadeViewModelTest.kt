package dev.matheus.fluviapp.ui.viewmodel.localidade

import androidx.lifecycle.SavedStateHandle
import dev.matheus.fluviapp.domain.localidade.Localidade
import dev.matheus.fluviapp.domain.localidade.Uf
import dev.matheus.fluviapp.fakes.FakeConsultaMunicipioIbge
import dev.matheus.fluviapp.fakes.FakeLocalidadeRepository
import dev.matheus.fluviapp.services.ibge.ResultadoConsultaIbge
import dev.matheus.fluviapp.ui.states.BuscaIbge
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
class FormLocalidadeViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private fun viewModel(
        repositorio: FakeLocalidadeRepository = FakeLocalidadeRepository(),
        ibge: FakeConsultaMunicipioIbge = FakeConsultaMunicipioIbge(),
        idLocalidade: String? = null,
    ) = FormLocalidadeViewModel(
        repositorio,
        ibge,
        if (idLocalidade == null) SavedStateHandle() else SavedStateHandle(mapOf("idLocalidade" to idLocalidade)),
    )

    // --- Cadastro ---

    @Test
    fun `salvar invalido marca erros e nao persiste`() = runTest(mainRule.dispatcher) {
        val fake = FakeLocalidadeRepository()
        val vm = viewModel(fake)

        vm.salvar()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertTrue(s.isMunicipioError)
        assertTrue(s.isUfError)
        assertTrue(s.isCodigoIbgeError)
        assertTrue(fake.salvos.isEmpty())
    }

    @Test
    fun `salvar valido persiste e emite sucesso`() = runTest(mainRule.dispatcher) {
        val fake = FakeLocalidadeRepository()
        val vm = viewModel(fake)
        val eventos = mutableListOf<Unit>()
        val job = launch { vm.sucesso.toList(eventos) }

        vm.onCodigoIbgeChange("1501402")
        vm.onMunicipioChange("Belém")
        vm.onUfChange(Uf.PA.rotulo())
        vm.salvar()
        advanceUntilIdle()

        val salva = fake.salvos.single()
        assertEquals("Belém", salva.municipio)
        assertEquals(Uf.PA, salva.uf)
        assertEquals("1501402", salva.codigoIbge)
        assertTrue("toda gravação do form é de localidade em uso", salva.ativo)
        assertEquals(1, eventos.size)
        job.cancel()
    }

    /** O campo aceita só dígito e para nos sete: colar "15.014-02 " não estraga a chave. */
    @Test
    fun `o codigo guarda so digitos e no maximo sete`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()

        vm.onCodigoIbgeChange("15.014-02 xyz9")

        assertEquals("1501402", vm.uiState.value.codigoIbge)
    }

    @Test
    fun `a uf entra pelo rotulo do dropdown`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()

        vm.onUfChange(Uf.AM.rotulo())

        assertEquals(Uf.AM, vm.uiState.value.uf)
    }

    @Test
    fun `edicao carrega a localidade existente`() = runTest(mainRule.dispatcher) {
        val fake = FakeLocalidadeRepository().apply {
            localidades = listOf(Localidade("loc-1", "Parintins", Uf.AM, "1303205"))
        }
        val vm = viewModel(fake, idLocalidade = "loc-1")
        advanceUntilIdle()

        val s = vm.uiState.value
        assertEquals("Parintins", s.municipio)
        assertEquals(Uf.AM, s.uf)
        assertEquals("1303205", s.codigoIbge)
    }

    // --- A ajuda de digitação (IBGE) ---

    @Test
    fun `consulta preenche municipio e uf`() = runTest(mainRule.dispatcher) {
        val ibge = FakeConsultaMunicipioIbge().apply {
            resultado = ResultadoConsultaIbge.Encontrado("Belém", Uf.PA)
        }
        val vm = viewModel(ibge = ibge)

        vm.onCodigoIbgeChange("1501402")
        vm.consultarIbge()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertEquals("Belém", s.municipio)
        assertEquals(Uf.PA, s.uf)
        assertEquals(BuscaIbge.Ociosa, s.buscaIbge)
        assertEquals(listOf("1501402"), ibge.consultados)
    }

    /** Código pela metade não vale consulta: gastaria rede para receber "não encontrado" e assustar. */
    @Test
    fun `nao consulta com codigo incompleto`() = runTest(mainRule.dispatcher) {
        val ibge = FakeConsultaMunicipioIbge()
        val vm = viewModel(ibge = ibge)

        vm.onCodigoIbgeChange("150")
        vm.consultarIbge()
        advanceUntilIdle()

        assertTrue(ibge.consultados.isEmpty())
        assertFalse(vm.uiState.value.podeConsultarIbge)
    }

    /**
     * A distinção que dá nome aos dois estados: **rede caída não apaga o que a pessoa escreveu** e não
     * acusa o código. Se os dois desfechos fossem um `null`, a tela diria "código inválido" para um
     * problema que é de conexão.
     */
    @Test
    fun `indisponivel preserva o que ja foi digitado`() = runTest(mainRule.dispatcher) {
        val ibge = FakeConsultaMunicipioIbge().apply { resultado = ResultadoConsultaIbge.Indisponivel }
        val vm = viewModel(ibge = ibge)

        vm.onCodigoIbgeChange("1501402")
        vm.onMunicipioChange("Belem digitado à mão")
        vm.consultarIbge()
        advanceUntilIdle()

        assertEquals("Belem digitado à mão", vm.uiState.value.municipio)
        assertEquals(BuscaIbge.Indisponivel, vm.uiState.value.buscaIbge)
    }

    @Test
    fun `nao encontrado fala do codigo, e so dele`() = runTest(mainRule.dispatcher) {
        val ibge = FakeConsultaMunicipioIbge().apply { resultado = ResultadoConsultaIbge.NaoEncontrado }
        val vm = viewModel(ibge = ibge)

        vm.onCodigoIbgeChange("1501402")
        vm.consultarIbge()
        advanceUntilIdle()

        assertEquals(BuscaIbge.NaoEncontrado, vm.uiState.value.buscaIbge)
        assertEquals("", vm.uiState.value.municipio)
    }

    /** Digitar de novo apaga o veredito anterior — senão ele acusaria um código que já não está na tela. */
    @Test
    fun `editar o codigo descarta o resultado anterior`() = runTest(mainRule.dispatcher) {
        val ibge = FakeConsultaMunicipioIbge().apply { resultado = ResultadoConsultaIbge.NaoEncontrado }
        val vm = viewModel(ibge = ibge)

        vm.onCodigoIbgeChange("1501402")
        vm.consultarIbge()
        advanceUntilIdle()
        assertEquals(BuscaIbge.NaoEncontrado, vm.uiState.value.buscaIbge)

        vm.onCodigoIbgeChange("1501403")

        assertEquals(BuscaIbge.Ociosa, vm.uiState.value.buscaIbge)
    }

    /** O IBGE fora do ar **não impede cadastrar**: é preenchimento, não porteiro. */
    @Test
    fun `salva mesmo com o ibge indisponivel`() = runTest(mainRule.dispatcher) {
        val fake = FakeLocalidadeRepository()
        val vm = viewModel(fake, FakeConsultaMunicipioIbge())

        vm.onCodigoIbgeChange("1501402")
        vm.consultarIbge()
        advanceUntilIdle()

        vm.onMunicipioChange("Belém")
        vm.onUfChange("PA")
        vm.salvar()
        advanceUntilIdle()

        assertEquals(1, fake.salvos.size)
    }

    @Test
    fun `falha ao salvar nao emite sucesso e libera processamento`() = runTest(mainRule.dispatcher) {
        val fake = FakeLocalidadeRepository().apply { falharAoSalvar = true }
        val vm = viewModel(fake)
        val eventos = mutableListOf<Unit>()
        val job = launch { vm.sucesso.toList(eventos) }

        vm.onCodigoIbgeChange("1501402")
        vm.onMunicipioChange("Belém")
        vm.onUfChange("PA")
        vm.salvar()
        advanceUntilIdle()

        assertTrue(eventos.isEmpty())
        assertFalse(vm.uiState.value.isProcessing)
        job.cancel()
    }
}