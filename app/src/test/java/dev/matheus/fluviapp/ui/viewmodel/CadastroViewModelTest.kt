package dev.matheus.fluviapp.ui.viewmodel

import dev.matheus.fluviapp.services.repository.firebase.autenticacao.FakeAutenticacaoRepository
import dev.matheus.fluviapp.services.repository.firebase.autenticacao.MotivoFalhaAuth
import dev.matheus.fluviapp.services.repository.firebase.autenticacao.ResultadoAutenticacao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Prova do item 2: o fluxo de cadastro é JVM-testável com fake da porta — sem rede/Firebase.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CadastroViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vmPreenchido(fake: FakeAutenticacaoRepository): CadastroViewModel {
        val vm = CadastroViewModel(fake)
        vm.uiState.value.onNomeChange("Ana")
        vm.uiState.value.onEmailChange("ana@teste.com")
        vm.uiState.value.onSenhaChange("123456")
        vm.uiState.value.onConfirmarSenhaChange("123456")
        return vm
    }

    @Test
    fun `cadastro com sucesso cria perfil operador, sai e marca cadastrado`() {
        val fake = FakeAutenticacaoRepository().apply {
            resultado = ResultadoAutenticacao.Sucesso(emailVerificado = false)
        }
        val vm = vmPreenchido(fake)

        vm.cadastrar()

        assertTrue(vm.uiState.value.cadastrado)
        assertFalse(vm.uiState.value.cadastrando)
        assertEquals("OPERADOR", fake.perfilCriado?.third)
        assertEquals("ana@teste.com", fake.perfilCriado?.first)
        assertEquals(1, fake.saiuVezes)
    }

    @Test
    fun `cadastro com falha mostra erro e nao marca cadastrado`() {
        val fake = FakeAutenticacaoRepository().apply {
            resultado = ResultadoAutenticacao.Falha(MotivoFalhaAuth.DESCONHECIDO)
        }
        val vm = vmPreenchido(fake)

        vm.cadastrar()

        assertFalse(vm.uiState.value.cadastrado)
        assertTrue(vm.uiState.value.exibirErro)
        assertNull(fake.perfilCriado)
    }

    @Test
    fun `formulario invalido nao chama a porta`() {
        val fake = FakeAutenticacaoRepository()
        val vm = CadastroViewModel(fake) // campos em branco

        vm.cadastrar()

        assertFalse(vm.uiState.value.cadastrando)
        assertFalse(vm.uiState.value.cadastrado)
        assertNull(fake.perfilCriado)
    }
}