package dev.matheus.fluviapp.ui.viewmodel.rota

import dev.matheus.fluviapp.domain.localidade.Localidade
import dev.matheus.fluviapp.domain.localidade.Uf
import dev.matheus.fluviapp.domain.porto.Porto
import dev.matheus.fluviapp.domain.rota.Rota
import dev.matheus.fluviapp.fakes.FakeLocalidadeRepository
import dev.matheus.fluviapp.fakes.FakePortoRepository
import dev.matheus.fluviapp.fakes.FakeRotaRepository
import dev.matheus.fluviapp.fakes.FakeSessaoUsuario
import dev.matheus.fluviapp.ui.states.ErroParRota
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

/**
 * As duas telas da Rota (F7.2).
 *
 * O que elas têm de próprio no app: **não há edição** — criar e inativar são os dois gestos — e
 * **inativar é ato de plataforma**, porque tira do pool algo que outra empresa pode estar vendendo.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RotaViewModelsTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val belem = Localidade("loc-belem", "Belém", Uf.PA, "1501402")
    private val parintins = Localidade("loc-parintins", "Parintins", Uf.AM, "1303205")

    private val portoBelem = Porto("porto-a", "Porto de Val-de-Cães", "loc-belem")
    private val portoParintins = Porto("porto-b", "Porto de Parintins", "loc-parintins")

    private fun locais() = FakeLocalidadeRepository().apply { localidades = listOf(belem, parintins) }
    private fun portos(lista: List<Porto> = listOf(portoBelem, portoParintins)) =
        FakePortoRepository().apply { portos = lista }

    private fun formVm(
        rotas: FakeRotaRepository = FakeRotaRepository(),
        portos: FakePortoRepository = portos(),
        sessao: FakeSessaoUsuario = FakeSessaoUsuario.supervisor(),
    ) = FormRotaViewModel(rotas, portos, locais(), sessao)

    private fun buscaVm(
        rotas: FakeRotaRepository,
        sessao: FakeSessaoUsuario = FakeSessaoUsuario.plataforma(),
    ) = PesquisaRotaViewModel(rotas, portos(), locais(), sessao)

    // --- Criação ---

    /** O rótulo traz a cidade: escolher entre dois "Porto Central" sem ela é escolher no escuro. */
    @Test
    fun `oferece os portos ativos com a cidade no rotulo`() = runTest(mainRule.dispatcher) {
        val vm = formVm()
        advanceUntilIdle()

        assertEquals(
            listOf("Porto de Parintins · Parintins/AM", "Porto de Val-de-Cães · Belém/PA"),
            vm.uiState.value.portos.map { it.rotulo },
        )
    }

    @Test
    fun `porto inativo nao entra no seletor`() = runTest(mainRule.dispatcher) {
        val vm = formVm(portos = portos(listOf(portoBelem, portoParintins.copy(ativo = false))))
        advanceUntilIdle()

        assertEquals(listOf("Porto de Val-de-Cães · Belém/PA"), vm.uiState.value.portos.map { it.rotulo })
    }

    @Test
    fun `criar persiste o par por id, com as medidas e a assinatura`() = runTest(mainRule.dispatcher) {
        val rotas = FakeRotaRepository()
        val vm = formVm(rotas)
        val eventos = mutableListOf<Unit>()
        val job = launch { vm.sucesso.toList(eventos) }
        advanceUntilIdle()

        vm.onPortoOrigemChange("Porto de Val-de-Cães · Belém/PA")
        vm.onPortoDestinoChange("Porto de Parintins · Parintins/AM")
        vm.onDistanciaChange("420.5")
        vm.onTempoChange("30")
        vm.salvar()
        advanceUntilIdle()

        val criada = rotas.criadas.single()
        assertEquals("porto-a", criada.portoOrigemId)
        assertEquals("porto-b", criada.portoDestinoId)
        assertEquals(420.5, criada.distanciaMn, 0.0)
        assertEquals(30.0, criada.tempoMedioH, 0.0)
        // Num pool sem dono, a assinatura é o que resta de responsabilidade (§7.1).
        assertEquals("f-op", criada.criadoPor)
        assertTrue(criada.criadoEm.isNotBlank())
        assertEquals(1, eventos.size)
        job.cancel()
    }

    @Test
    fun `nao cria rota duplicada no pool`() = runTest(mainRule.dispatcher) {
        val rotas = FakeRotaRepository().apply {
            rotas = listOf(Rota("r1", "porto-a", "porto-b", 400.0, 30.0))
        }
        val vm = formVm(rotas)
        advanceUntilIdle()

        vm.onPortoOrigemChange("Porto de Val-de-Cães · Belém/PA")
        vm.onPortoDestinoChange("Porto de Parintins · Parintins/AM")
        vm.onDistanciaChange("420")
        vm.onTempoChange("30")
        vm.salvar()
        advanceUntilIdle()

        assertEquals(ErroParRota.DUPLICADA, vm.uiState.value.erroPar)
        assertTrue(rotas.criadas.isEmpty())
    }

    /** Mexer em qualquer um dos lados apaga a queixa: as três razões do erro são do **par**. */
    @Test
    fun `trocar um porto limpa o erro do par`() = runTest(mainRule.dispatcher) {
        val vm = formVm()
        advanceUntilIdle()

        vm.salvar()
        assertEquals(ErroParRota.OBRIGATORIO, vm.uiState.value.erroPar)

        vm.onPortoOrigemChange("Porto de Val-de-Cães · Belém/PA")

        assertEquals(ErroParRota.NENHUM, vm.uiState.value.erroPar)
    }

    // --- Busca ---

    private fun poolComDuas() = FakeRotaRepository().apply {
        rotas = listOf(
            Rota("r1", "porto-a", "porto-b", 420.5, 30.0),
            Rota("r2", "porto-b", "porto-a", 420.5, 32.0, ativo = false),
        )
    }

    /** A lista mostra **as inativas também**, marcadas: o descartado é registro (§7.1). */
    @Test
    fun `a lista traz ativas e inativas, com os portos resolvidos`() = runTest(mainRule.dispatcher) {
        val vm = buscaVm(poolComDuas())
        advanceUntilIdle()

        val resultados = vm.uiState.value.resultados
        assertEquals(2, resultados.size)
        assertEquals("Porto de Val-de-Cães · Belém/PA", resultados.first { it.id == "r1" }.origem)
        assertTrue(resultados.first { it.id == "r1" }.ativa)
        assertFalse(resultados.first { it.id == "r2" }.ativa)
    }

    /** "O que liga daqui" inclui chegar aqui: o filtro casa contra os dois portos. */
    @Test
    fun `filtrar por porto casa origem e destino`() = runTest(mainRule.dispatcher) {
        val vm = buscaVm(poolComDuas())
        advanceUntilIdle()

        vm.onPortoChange("Parintins")

        assertEquals(2, vm.uiState.value.resultados.size)
    }

    @Test
    fun `plataforma inativa a rota, e ela continua na lista marcada`() = runTest(mainRule.dispatcher) {
        val rotas = poolComDuas()
        val vm = buscaVm(rotas)
        advanceUntilIdle()

        vm.onInativar("r1")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.podeInativar)
        assertFalse(vm.uiState.value.resultados.first { it.id == "r1" }.ativa)
        assertEquals(2, vm.uiState.value.resultados.size)
    }

    /**
     * **Inativar é o único poder do pool que atinge terceiros** (ADR-0022 D3): tira do ar algo que outra
     * empresa pode estar vendendo. O supervisor cria; para "não quero ver esta", o instrumento é a lista
     * de negadas da atuação (F8).
     */
    @Test
    fun `supervisor nao inativa rota do pool`() = runTest(mainRule.dispatcher) {
        val rotas = poolComDuas()
        val vm = buscaVm(rotas, FakeSessaoUsuario.supervisor())
        advanceUntilIdle()

        vm.onInativar("r1")
        advanceUntilIdle()

        assertFalse(vm.uiState.value.podeInativar)
        assertTrue(vm.uiState.value.resultados.first { it.id == "r1" }.ativa)
    }
}