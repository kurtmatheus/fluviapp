package dev.matheus.fluviapp.ui.viewmodel.empresa

import androidx.lifecycle.SavedStateHandle
import dev.matheus.fluviapp.fakes.FakeEmpresaRepository
import dev.matheus.fluviapp.domain.operacoes.Atuacao
import dev.matheus.fluviapp.domain.viagem.AtuacaoDaEmpresa
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
        vm.onAtuacaoToggle(Atuacao.AGENCIAMENTO) // obrigatória (domínio §3.1)
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

    // --- Atuações: a subcoleção `empresas/{id}/atuacoes` (ADR-0016 §4, ADR-0020 F5c) ---

    @Test
    fun `atuacao marcada e salva na subcolecao, pendurada no id gerado`() = runTest(mainRule.dispatcher) {
        val fake = FakeEmpresaRepository()
        val vm = FormEmpresaViewModel(fake, SavedStateHandle())

        preencherObrigatorios(vm)
        vm.onAtuacaoToggle(Atuacao.AGENCIAMENTO)
        vm.salvar()
        advanceUntilIdle()

        val id = fake.salvos.single().id.ifBlank { "id-gerado-1" }
        assertEquals(
            listOf(AtuacaoDaEmpresa(Atuacao.AGENCIAMENTO)),
            fake.atuacoesPorEmpresa[id],
        )
    }

    @Test
    fun `o toggle liga e desliga — desmarcar deixa a parte sem aquela atuacao`() {
        val vm = FormEmpresaViewModel(FakeEmpresaRepository(), SavedStateHandle())

        vm.onAtuacaoToggle(Atuacao.TRANSPORTE)
        assertEquals(setOf(Atuacao.TRANSPORTE), vm.uiState.value.atuacoes)

        vm.onAtuacaoToggle(Atuacao.TRANSPORTE)
        assertTrue(vm.uiState.value.atuacoes.isEmpty())
    }

    @Test
    fun `uma parte exerce varias atuacoes ao mesmo tempo`() {
        val vm = FormEmpresaViewModel(FakeEmpresaRepository(), SavedStateHandle())

        vm.onAtuacaoToggle(Atuacao.AGENCIAMENTO)
        vm.onAtuacaoToggle(Atuacao.TRANSPORTE)

        assertEquals(setOf(Atuacao.AGENCIAMENTO, Atuacao.TRANSPORTE), vm.uiState.value.atuacoes)
    }

    @Test
    fun `salvar preserva a concessao ja concedida em vez de zera-la`() = runTest(mainRule.dispatcher) {
        // O form decide QUAIS atuações a parte exerce, não os embarcacoes concedidos. Salvar a empresa não
        // pode apagar concessão feita noutro lugar.
        val fake = FakeEmpresaRepository()
        fake.empresas = listOf(empresaValida("e1"))
        fake.atuacoesPorEmpresa["e1"] =
            listOf(AtuacaoDaEmpresa(Atuacao.AGENCIAMENTO, embarcacaoIds = setOf("embarcacao-7")))
        val vm = FormEmpresaViewModel(fake, SavedStateHandle(mapOf("idEmpresa" to "e1")))
        advanceUntilIdle()

        vm.salvar()
        advanceUntilIdle()

        assertEquals(
            setOf("embarcacao-7"),
            fake.atuacoesPorEmpresa["e1"]?.single()?.embarcacaoIds,
        )
    }

    @Test
    fun `edicao carrega as atuacoes ja gravadas`() = runTest(mainRule.dispatcher) {
        val fake = FakeEmpresaRepository()
        fake.empresas = listOf(empresaValida("e1"))
        fake.atuacoesPorEmpresa["e1"] = listOf(AtuacaoDaEmpresa(Atuacao.TRANSPORTE))
        val vm = FormEmpresaViewModel(fake, SavedStateHandle(mapOf("idEmpresa" to "e1")))

        advanceUntilIdle()

        assertEquals(setOf(Atuacao.TRANSPORTE), vm.uiState.value.atuacoes)
    }

    @Test
    fun `parte sem atuacao nenhuma NAO e salva — o que a empresa faz vive nas atuacoes`() =
        runTest(mainRule.dispatcher) {
            // Domínio §3.1: a empresa não tem campo de segmento nem de tipo. Sem atuação ela não pode
            // ser escolhida em lugar nenhum — não tem cargo, não abre seção, não recebe concessão.
            val fake = FakeEmpresaRepository()
            val vm = FormEmpresaViewModel(fake, SavedStateHandle())

            preencherObrigatorios(vm)
            vm.salvar()
            advanceUntilIdle()

            assertTrue(vm.uiState.value.isAtuacoesError)
            assertTrue(fake.salvos.isEmpty())
        }

    private fun preencherObrigatorios(vm: FormEmpresaViewModel) {
        vm.onNomeChange("EMPRESA MODELO")
        vm.onRazaoSocialChange("EMPRESA MODELO LTDA")
        vm.onCnpjChange("11222333000181")
    }

    private fun empresaValida(id: String) = Empresa(
        id = id,
        nome = "EMPRESA MODELO",
        razaoSocial = "EMPRESA MODELO LTDA",
        cnpj = "11222333000181",
        endereco = "",
        telefone1 = "",
        telefone2 = "",
    )
}
