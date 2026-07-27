package dev.matheus.fluviapp.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.fakes.FakeFuncionarioRepository
import dev.matheus.fluviapp.model.operacoes.Funcionario
import dev.matheus.fluviapp.model.operacoes.Usuario
import dev.matheus.fluviapp.navigation.destinations.ARG_EMAIL_PRIMEIRO_ACESSO
import dev.matheus.fluviapp.services.repository.firebase.autenticacao.FakeAutenticacaoRepository
import dev.matheus.fluviapp.services.repository.firebase.autenticacao.MotivoFalhaAuth
import dev.matheus.fluviapp.services.repository.firebase.autenticacao.ResultadoAutenticacao
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
 * Primeiro acesso (ADR-0015 §2.1). O que estes testes travam, além do caminho feliz, é a **ordem** das
 * duas escritas — senha primeiro, perfil depois — porque é ela que decide se um erro no meio deixa a
 * pessoa presa à senha compartilhada ou não.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PrimeiroAcessoViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val email = "ana.ribeiro@fluviapp.com.br"

    private val funcionario = Funcionario(
        id = "f1",
        descricaoNome = "Ana Ribeiro",
        agencia = "MATRIZ",
        lotacao = "PORTO NORTE",
        email = email,
    )

    private fun vm(
        auth: FakeAutenticacaoRepository = FakeAutenticacaoRepository(),
        funcionarios: List<Funcionario> = listOf(funcionario),
        emailRota: String = email,
    ) = PrimeiroAcessoViewModel(
        auth,
        FakeFuncionarioRepository().apply { this.funcionarios = funcionarios },
        SavedStateHandle(mapOf(ARG_EMAIL_PRIMEIRO_ACESSO to emailRota)),
    )

    @Test
    fun `exibe o nome de quem a gestao cadastrou`() = runTest(mainRule.dispatcher) {
        val vm = vm()
        advanceUntilIdle()

        assertEquals("Ana Ribeiro", vm.uiState.value.nome)
    }

    @Test
    fun `senha curta nao chega ao Auth`() = runTest(mainRule.dispatcher) {
        val auth = FakeAutenticacaoRepository()
        val vm = vm(auth)
        advanceUntilIdle()

        vm.onSenhaChange("12345")
        vm.onConfirmacaoChange("12345")
        vm.confirmar()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isSenhaError)
        assertEquals(R.string.error_senha_curta, vm.uiState.value.mensagemErro)
        assertNull(auth.senhaAlterada)
    }

    @Test
    fun `confirmacao diferente acende so o campo da confirmacao`() = runTest(mainRule.dispatcher) {
        val auth = FakeAutenticacaoRepository()
        val vm = vm(auth)
        advanceUntilIdle()

        vm.onSenhaChange("senha123")
        vm.onConfirmacaoChange("senha124")
        vm.confirmar()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isSenhaError)
        assertTrue(vm.uiState.value.isConfirmacaoError)
        assertEquals(R.string.error_senhas_diferentes, vm.uiState.value.mensagemErro)
        assertNull(auth.senhaAlterada)
    }

    @Test
    fun `caminho feliz troca a senha, cria o perfil vinculado e sai da sessao`() = runTest(mainRule.dispatcher) {
        val auth = FakeAutenticacaoRepository()
        val vm = vm(auth)
        advanceUntilIdle()

        vm.onSenhaChange("senha123")
        vm.onConfirmacaoChange("senha123")
        vm.confirmar()
        advanceUntilIdle()

        assertEquals("senha123", auth.senhaAlterada)
        // Perfil nasce do registro do funcionário: papel OPERADOR, vínculo pelo id, username do e-mail.
        assertEquals(listOf(email, "ana.ribeiro", Usuario.Papel.OPERADOR.name, "f1"), auth.perfilCriado)
        assertEquals(1, auth.saiuVezes)
        assertTrue(vm.uiState.value.concluido)
    }

    @Test
    fun `falha ao trocar a senha NAO cria perfil`() = runTest(mainRule.dispatcher) {
        val auth = FakeAutenticacaoRepository().apply {
            resultadoAlterarSenha = ResultadoAutenticacao.Falha(MotivoFalhaAuth.DESCONHECIDO)
        }
        val vm = vm(auth)
        advanceUntilIdle()

        vm.onSenhaChange("senha123")
        vm.onConfirmacaoChange("senha123")
        vm.confirmar()
        advanceUntilIdle()

        // Se o perfil nascesse aqui, o próximo login deixaria de ser primeiro acesso — e a pessoa
        // ficaria presa à senha padrão, sem tela que a deixe trocar. Por isso a ordem é senha primeiro.
        assertNull(auth.perfilCriado)
        assertFalse(vm.uiState.value.concluido)
        assertEquals(R.string.error_falha_auth, vm.uiState.value.mensagemErro)
    }

    @Test
    fun `falha ao criar o perfil deixa a senha nova valendo e o fluxo repetivel`() = runTest(mainRule.dispatcher) {
        val auth = FakeAutenticacaoRepository().apply { falharAoCriarPerfil = true }
        val vm = vm(auth)
        advanceUntilIdle()

        vm.onSenhaChange("senha123")
        vm.onConfirmacaoChange("senha123")
        vm.confirmar()
        advanceUntilIdle()

        // Pior caso reparável: a senha já é dela e o primeiro acesso simplesmente acontece de novo.
        assertEquals("senha123", auth.senhaAlterada)
        assertFalse(vm.uiState.value.concluido)
        assertFalse(vm.uiState.value.processando)
    }

    @Test
    fun `sem funcionario para o e-mail nao troca senha nenhuma`() = runTest(mainRule.dispatcher) {
        val auth = FakeAutenticacaoRepository()
        val vm = vm(auth, funcionarios = emptyList())
        advanceUntilIdle()

        vm.onSenhaChange("senha123")
        vm.onConfirmacaoChange("senha123")
        vm.confirmar()
        advanceUntilIdle()

        // Trocar a senha sozinha deixaria uma conta órfã: sem registro na equipe, não há perfil a criar.
        assertNull(auth.senhaAlterada)
        assertNull(auth.perfilCriado)
        assertEquals(R.string.error_sem_cadastro_na_equipe, vm.uiState.value.mensagemErro)
    }
}