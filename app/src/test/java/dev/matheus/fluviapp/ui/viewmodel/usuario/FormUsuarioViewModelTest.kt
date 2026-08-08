package dev.matheus.fluviapp.ui.viewmodel.usuario

import dev.matheus.fluviapp.domain.operacoes.Funcionario.Cargo
import dev.matheus.fluviapp.domain.operacoes.Usuario.Papel
import dev.matheus.fluviapp.domain.operacoes.Vinculo
import dev.matheus.fluviapp.domain.viagem.Empresa
import dev.matheus.fluviapp.fakes.FakeConviteRepository
import dev.matheus.fluviapp.fakes.FakeEmpresaRepository
import dev.matheus.fluviapp.fakes.FakeFuncionarioRepository
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

/**
 * **Novo usuário** — o convite (F6.6).
 *
 * O caso que importa é o do operador: um gesto, **duas escritas**, e o funcionário tem de existir antes
 * do convite. É o que garante que quem for convidado encontre o registro para se ligar no primeiro
 * acesso — a regra do servidor só aceita o elo se o funcionário já existir com aquele e-mail.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FormUsuarioViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private fun empresa(id: String, nome: String) =
        Empresa(id = id, nome = nome, razaoSocial = nome, cnpj = "", endereco = "", telefone1 = "", telefone2 = "")

    private fun vm(
        convites: FakeConviteRepository = FakeConviteRepository(),
        funcionarios: FakeFuncionarioRepository = FakeFuncionarioRepository(),
    ) = FormUsuarioViewModel(
        convites,
        funcionarios,
        FakeEmpresaRepository().apply { empresas = listOf(empresa("empresa-1", "Navegação Norte")) },
    )

    @Test
    fun `salvar invalido marca erros e nao persiste`() = runTest(mainRule.dispatcher) {
        val convites = FakeConviteRepository()
        val vm = vm(convites)
        advanceUntilIdle()

        vm.salvar()

        val s = vm.uiState.value
        assertTrue(s.isNomeError)
        assertTrue(s.isEmailError)
        assertTrue(s.isPapelError)
        assertTrue(convites.salvos.isEmpty())
    }

    @Test
    fun `convite de operador cria o funcionario com o vinculo e depois o convite`() =
        runTest(mainRule.dispatcher) {
            val convites = FakeConviteRepository()
            val funcionarios = FakeFuncionarioRepository()
            val vm = vm(convites, funcionarios)
            val eventos = mutableListOf<Unit>()
            val job = launch { vm.sucesso.toList(eventos) }
            advanceUntilIdle()

            vm.onNomeChange("Ana Ribeiro")
            vm.onEmailChange("Ana@Fluviapp.com.br")
            vm.onPapelChange(Papel.OPERADOR.name)
            vm.onEmpresaChange("Navegação Norte")
            vm.onCargoChange(Cargo.SUPERVISOR.name)
            vm.salvar()
            advanceUntilIdle()

            val funcionario = funcionarios.salvos.single()
            assertEquals("Ana Ribeiro", funcionario.descricaoNome)
            // O e-mail é a chave que casa as duas frentes no primeiro acesso: gravado normalizado.
            assertEquals("ana@fluviapp.com.br", funcionario.email)
            assertEquals(listOf(Vinculo("empresa-1", Cargo.SUPERVISOR)), funcionario.vinculos)

            val convite = convites.salvos.single()
            assertEquals("ana@fluviapp.com.br", convite.email)
            assertEquals(Papel.OPERADOR, convite.papel)
            assertEquals(1, eventos.size)
            job.cancel()
        }

    /** Papel de plataforma não tem registro na operação (§8.1): convida, e não cria funcionário nenhum. */
    @Test
    fun `convite de plataforma nao cria funcionario`() = runTest(mainRule.dispatcher) {
        val convites = FakeConviteRepository()
        val funcionarios = FakeFuncionarioRepository()
        val vm = vm(convites, funcionarios)
        advanceUntilIdle()

        vm.onNomeChange("Novo Gestor")
        vm.onEmailChange("gestor@fluviapp.com.br")
        vm.onPapelChange(Papel.GESTOR.name)
        vm.salvar()
        advanceUntilIdle()

        assertTrue(funcionarios.salvos.isEmpty())
        assertEquals(Papel.GESTOR, convites.salvos.single().papel)
    }

    @Test
    fun `operador exige empresa e cargo`() = runTest(mainRule.dispatcher) {
        val convites = FakeConviteRepository()
        val vm = vm(convites)
        advanceUntilIdle()

        vm.onNomeChange("Ana")
        vm.onEmailChange("ana@fluviapp.com.br")
        vm.onPapelChange(Papel.OPERADOR.name)
        vm.salvar()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isEmpresaError)
        assertTrue(convites.salvos.isEmpty())
    }

    /**
     * Trocar para papel de plataforma **apaga** a empresa escolhida: gravá-la seria prometer um vínculo
     * que ninguém vai criar, e o estado ficaria diferente do que a tela mostra.
     */
    @Test
    fun `trocar para papel de plataforma limpa a empresa`() = runTest(mainRule.dispatcher) {
        val vm = vm()
        advanceUntilIdle()

        vm.onPapelChange(Papel.OPERADOR.name)
        vm.onEmpresaChange("Navegação Norte")
        vm.onPapelChange(Papel.ADM.name)

        assertEquals("", vm.uiState.value.empresa)
        assertTrue(!vm.uiState.value.perguntaVinculo)
    }
}