package dev.matheus.fluviapp.ui.viewmodel.empresa

import androidx.lifecycle.SavedStateHandle
import dev.matheus.fluviapp.fakes.FakeEmpresaRepository
import dev.matheus.fluviapp.domain.viagem.Empresa
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
class FormEmpresaViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun `salvar invalido marca erros e nao persiste`() = runTest(mainRule.dispatcher) {
        val fake = FakeEmpresaRepository()
        val vm = FormEmpresaViewModel(fake, SavedStateHandle())

        vm.salvar()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertTrue(s.isNomeError)
        assertTrue(s.isRazaoSocialError)
        assertTrue(s.isCnpjError)
        assertTrue(fake.salvos.isEmpty())
    }

    @Test
    fun `salvar valido persiste cnpj em digitos e emite sucesso`() = runTest(mainRule.dispatcher) {
        val fake = FakeEmpresaRepository()
        val vm = FormEmpresaViewModel(fake, SavedStateHandle())
        val eventos = mutableListOf<Unit>()
        val job = launch { vm.sucesso.toList(eventos) }

        vm.onNomeChange("ACME")
        vm.onRazaoSocialChange("ACME LTDA")
        vm.onCnpjChange("11.222.333/0001-81") // com máscara → guarda só dígitos
        vm.salvar()
        advanceUntilIdle()

        assertEquals(1, fake.salvos.size)
        assertEquals("11222333000181", fake.salvos.first().cnpj)
        assertEquals(1, eventos.size)
        job.cancel()
    }

    @Test
    fun `falha ao salvar nao emite sucesso e libera processamento`() = runTest(mainRule.dispatcher) {
        val fake = FakeEmpresaRepository().apply { falharAoSalvar = true }
        val vm = FormEmpresaViewModel(fake, SavedStateHandle())
        val eventos = mutableListOf<Unit>()
        val job = launch { vm.sucesso.toList(eventos) }

        vm.onNomeChange("ACME")
        vm.onRazaoSocialChange("ACME LTDA")
        vm.onCnpjChange("11222333000181")
        vm.salvar()
        advanceUntilIdle()

        assertTrue(eventos.isEmpty())
        assertFalse(vm.uiState.value.isProcessing)
        job.cancel()
    }

    @Test
    fun `edicao carrega empresa existente`() = runTest(mainRule.dispatcher) {
        val fake = FakeEmpresaRepository().apply {
            empresas = listOf(Empresa("e1", "ACME", "ACME LTDA", "11222333000181", "Rua 1", "111", "222"))
        }
        val vm = FormEmpresaViewModel(fake, SavedStateHandle(mapOf("idEmpresa" to "e1")))
        advanceUntilIdle()

        val s = vm.uiState.value
        assertEquals("ACME", s.nome)
        assertEquals("ACME LTDA", s.razaoSocial)
        assertEquals("11222333000181", s.cnpj)
    }
}
