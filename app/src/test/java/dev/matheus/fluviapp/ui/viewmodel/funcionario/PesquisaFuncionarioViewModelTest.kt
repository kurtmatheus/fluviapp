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
        Funcionario("1", "Ana", vinculo = Vinculo("empresa-1", Cargo.SUPERVISOR)),
        Funcionario("2", "Bruno", vinculo = Vinculo("empresa-2", Cargo.AGENTE)),
        // Sem vínculo: é o pré-cadastro (§2.1), e ele aparece na lista para poder ser completado.
        Funcionario("3", "Carla"),
    )

    @Test
    fun `carrega todos os membros e as empresas`() = runTest(mainRule.dispatcher) {
        val vm = vm(FakeFuncionarioRepository().apply { funcionarios = amostra })
        advanceUntilIdle()

        assertEquals(3, vm.uiState.value.resultados.size)
        assertEquals(2, vm.uiState.value.empresas.size)
    }

    /** A linha diz onde a pessoa atua e como — e quem não tem vínculo aparece sem essa linha. */
    @Test
    fun `o vinculo vira linha com empresa e cargo, e a ausencia vira nada`() = runTest(mainRule.dispatcher) {
        val vm = vm(FakeFuncionarioRepository().apply { funcionarios = amostra })
        advanceUntilIdle()

        assertEquals(
            "Navegação Norte · SUPERVISOR",
            vm.uiState.value.resultados.first { it.id == "1" }.vinculo,
        )
        assertNull(vm.uiState.value.resultados.first { it.id == "3" }.vinculo)
    }

    @Test
    fun `filtra por inicio do nome`() = runTest(mainRule.dispatcher) {
        val vm = vm(FakeFuncionarioRepository().apply { funcionarios = amostra })
        advanceUntilIdle()

        vm.onNomeChange("an")

        assertEquals(listOf("Ana"), vm.uiState.value.resultados.map { it.nome })
    }

    /** Filtrar por empresa deixa de fora quem não tem vínculo — não há empresa em que ela conte. */
    @Test
    fun `filtra por empresa`() = runTest(mainRule.dispatcher) {
        val vm = vm(FakeFuncionarioRepository().apply { funcionarios = amostra })
        advanceUntilIdle()

        vm.onEmpresaChange("Rio Sul")

        assertEquals(listOf("Bruno"), vm.uiState.value.resultados.map { it.nome })
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
        // Nem Bruno (outra empresa) nem Carla (sem vínculo): a lista é a equipe dele, e só.
        assertEquals(listOf("Ana"), s.resultados.map { it.nome })
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
        assertEquals(listOf("Ana"), vm.uiState.value.resultados.map { it.nome })
    }

    /** **Quem gere a equipe, gere por inteiro** (F6.7): o supervisor também remove — na empresa dele. */
    @Test
    fun `supervisor remove membro da propria empresa`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository().apply { funcionarios = amostra }
        val vm = vm(fake, FakeSessaoUsuario.supervisor(empresaId = "empresa-1"))
        advanceUntilIdle()

        vm.onDeletar("1")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.podeDeletar)
        assertTrue(fake.deletados.contains("1"))
    }

    /** O agente não gere ninguém — e a barreira é dupla: a tela esconde, e o VM recusa. */
    @Test
    fun `agente nao remove membro`() = runTest(mainRule.dispatcher) {
        val fake = FakeFuncionarioRepository().apply { funcionarios = amostra }
        val vm = vm(fake, FakeSessaoUsuario.agente(empresaId = "empresa-1"))
        advanceUntilIdle()

        vm.onDeletar("1")
        advanceUntilIdle()

        assertFalse(vm.uiState.value.podeDeletar)
        assertTrue(fake.deletados.isEmpty())
    }
}