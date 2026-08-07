package dev.matheus.fluviapp.ui.viewmodel

import dev.matheus.fluviapp.domain.viagem.Empresa
import dev.matheus.fluviapp.fakes.FakeEmpresaRepository
import dev.matheus.fluviapp.fakes.FakeSessaoUsuario
import dev.matheus.fluviapp.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

/**
 * A seleção de contexto (ADR-0016 §6, F6.4): a tela que pergunta **em nome de qual empresa** se opera.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SelecaoVinculoViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private fun empresa(id: String, nome: String) =
        Empresa(id = id, nome = nome, razaoSocial = nome, cnpj = "", endereco = "", telefone1 = "", telefone2 = "")

    private fun empresasFake() = FakeEmpresaRepository().apply {
        empresas = listOf(empresa("empresa-1", "Navegação Norte"), empresa("empresa-2", "Rio Sul"))
    }

    @Test
    fun `oferece uma opcao por vinculo, com empresa e cargo`() = runTest(mainRule.dispatcher) {
        val vm = SelecaoVinculoViewModel(FakeSessaoUsuario.comDoisVinculos(), empresasFake())
        advanceUntilIdle()

        val s = vm.uiState.value
        assertFalse(s.carregando)
        assertEquals(
            listOf("Navegação Norte · SUPERVISOR", "Rio Sul · AGENTE"),
            s.opcoes.map { "${it.empresa} · ${it.cargo}" },
        )
    }

    /** O cargo aparece porque é ele que muda o que a pessoa poderá fazer — escolher sem ver seria no escuro. */
    @Test
    fun `exibe o nome de quem esta entrando`() = runTest(mainRule.dispatcher) {
        val vm = SelecaoVinculoViewModel(FakeSessaoUsuario.comDoisVinculos(), empresasFake())
        advanceUntilIdle()

        assertEquals("Operador", vm.uiState.value.nome)
    }

    @Test
    fun `escolher guarda a empresa e avisa uma vez`() = runTest(mainRule.dispatcher) {
        val sessao = FakeSessaoUsuario.comDoisVinculos()
        val vm = SelecaoVinculoViewModel(sessao, empresasFake())
        val eventos = mutableListOf<Unit>()
        val job = launch { vm.escolhido.toList(eventos) }
        advanceUntilIdle()

        vm.escolher("empresa-2")
        advanceUntilIdle()

        assertEquals("empresa-2", sessao.empresaEscolhida)
        assertEquals("empresa-2", sessao.atual()?.vinculoAtivo?.empresaId)
        assertEquals(1, eventos.size)
        job.cancel()
    }

    /**
     * Empresa que não resolve continua **escolhível**, com o id à mostra: esconder a opção deixaria a
     * pessoa presa numa tela sem a alternativa que ela de fato tem.
     */
    @Test
    fun `vinculo com empresa desconhecida ainda aparece`() = runTest(mainRule.dispatcher) {
        val vm = SelecaoVinculoViewModel(
            FakeSessaoUsuario.comDoisVinculos(segunda = "empresa-sumida"),
            empresasFake(),
        )
        advanceUntilIdle()

        assertEquals(
            listOf("Navegação Norte", "empresa-sumida"),
            vm.uiState.value.opcoes.map { it.empresa },
        )
    }
}