package dev.matheus.fluviapp.ui.viewmodel.funcionario

import dev.matheus.fluviapp.domain.operacoes.Funcionario
import dev.matheus.fluviapp.domain.operacoes.Funcionario.Cargo
import dev.matheus.fluviapp.domain.operacoes.Vinculo
import dev.matheus.fluviapp.domain.viagem.Empresa
import dev.matheus.fluviapp.fakes.FakeEmpresaRepository
import dev.matheus.fluviapp.fakes.FakeFuncionarioRepository
import dev.matheus.fluviapp.fakes.FakeSessaoUsuario
import dev.matheus.fluviapp.services.repository.operacoes.SessaoUsuario
import dev.matheus.fluviapp.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * A busca de membros com o recorte por **empresa** (F6.3).
 *
 * O recorte continua sendo aplicado ao universo, e não ao filtro — é o que impede qualquer caminho de UI
 * de contorná-lo. O que passou a existir é a linha por vínculo: quem serve a duas empresas aparece com as
 * duas.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PesquisaFuncionarioViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    /** Do cadastro de empresa só importam id e nome aqui — o resto é preenchimento obrigatório. */
    private fun empresa(id: String, nome: String) =
        Empresa(id = id, nome = nome, razaoSocial = nome, cnpj = "", endereco = "", telefone1 = "", telefone2 = "")

    private fun empresasFake() = FakeEmpresaRepository().apply {
        empresas = listOf(empresa("empresa-1", "Navegação Norte"), empresa("empresa-2", "Rio Sul"))
    }

    private fun vm(repo: FakeFuncionarioRepository, sessao: SessaoUsuario = FakeSessaoUsuario.plataforma()) =
        PesquisaFuncionarioViewModel(repo, empresasFake(), sessao)

    private val amostra = listOf(
        Funcionario("1", "Ana", "Navegação Norte", vinculos = listOf(Vinculo("empresa-1", Cargo.SUPERVISOR))),
        Funcionario("2", "Bruno", "Rio Sul", vinculos = listOf(Vinculo("empresa-2", Cargo.AGENTE))),
        Funcionario(
            "3", "Carla", "Navegação Norte",
            vinculos = listOf(Vinculo("empresa-1", Cargo.AGENTE), Vinculo("empresa-2", Cargo.AGENTE)),
        ),
    )

    @Test
    fun `carrega todos os membros e as empresas`() = runTest(mainRule.dispatcher) {
        val vm = vm(FakeFuncionarioRepository().apply { funcionarios = amostra })
        advanceUntilIdle()

        assertEquals(3, vm.uiState.value.resultados.size)
        assertEquals(2, vm.uiState.value.empresas.size)
    }

    /** A linha diz onde a pessoa atua e como — e quem atua em duas mostra as duas. */
    @Test
    fun `cada vinculo vira uma linha, com empresa e cargo`() = runTest(mainRule.dispatcher) {
        val vm = vm(FakeFuncionarioRepository().apply { funcionarios = amostra })
        advanceUntilIdle()

        assertEquals(
            listOf("Navegação Norte · SUPERVISOR"),
            vm.uiState.value.resultados.first { it.id == "1" }.vinculos,
        )
        assertEquals(
            listOf("Navegação Norte · AGENTE", "Rio Sul · AGENTE"),
            vm.uiState.value.resultados.first { it.id == "3" }.vinculos,
        )
    }

    @Test
    fun `filtra por inicio do nome`() = runTest(mainRule.dispatcher) {
        val vm = vm(FakeFuncionarioRepository().apply { funcionarios = amostra })
        advanceUntilIdle()

        vm.onNomeChange("an")

        assertEquals(listOf("Ana"), vm.uiState.value.resultados.map { it.nome })
    }

    @Test
    fun `filtra por empresa, incluindo quem serve a duas`() = runTest(mainRule.dispatcher) {
        val vm = vm(FakeFuncionarioRepository().apply { funcionarios = amostra })
        advanceUntilIdle()

        vm.onEmpresaChange("Rio Sul")

        assertEquals(setOf("Bruno", "Carla"), vm.uiState.value.resultados.map { it.nome }.toSet())
    }

    @Test
    fun `deletar remove o membro e recarrega os resultados`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository().apply { funcionarios = amostra }
        val vm = vm(fake)
        advanceUntilIdle()

        vm.onDeletar("1")
        advanceUntilIdle()

        assertTrue(fake.deletados.contains("1"))
        assertEquals(2, vm.uiState.value.resultados.size)
        assertNull(vm.uiState.value.resultados.find { it.id == "1" })
    }

    // --- Recorte por cargo na listagem (ADR-0015 §2.2) ---

    @Test
    fun `supervisor ve apenas a propria empresa e nao filtra por empresa`() = runTest(mainRule.dispatcher) {
        val vm = vm(
            FakeFuncionarioRepository().apply { funcionarios = amostra },
            FakeSessaoUsuario.supervisor(empresaId = "empresa-1"),
        )
        advanceUntilIdle()

        val s = vm.uiState.value
        assertFalse(s.podeFiltrarPorEmpresa)
        assertEquals(setOf("Ana", "Carla"), s.resultados.map { it.nome }.toSet())
        // Sem filtro de empresa, também não se oferece a lista das outras.
        assertTrue(s.empresas.isEmpty())
    }

    @Test
    fun `supervisor nao escapa do recorte tentando filtrar por outra empresa`() = runTest(mainRule.dispatcher) {
        val vm = vm(
            FakeFuncionarioRepository().apply { funcionarios = amostra },
            FakeSessaoUsuario.supervisor(empresaId = "empresa-1"),
        )
        advanceUntilIdle()

        vm.onEmpresaChange("Rio Sul")

        // O recorte é do universo, não do filtro: o evento é ignorado e a lista continua sendo a dele.
        assertEquals(setOf("Ana", "Carla"), vm.uiState.value.resultados.map { it.nome }.toSet())
    }

    @Test
    fun `supervisor nao deleta membro`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository().apply { funcionarios = amostra }
        val vm = vm(fake, FakeSessaoUsuario.supervisor(empresaId = "empresa-1"))
        advanceUntilIdle()

        vm.onDeletar("1")
        advanceUntilIdle()

        assertFalse(vm.uiState.value.podeDeletar)
        assertTrue(fake.deletados.isEmpty())
    }
}