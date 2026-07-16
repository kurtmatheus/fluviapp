package dev.matheus.fluviapp.model.mappers

import dev.matheus.fluviapp.fakes.FakeNavioRepository
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.INTEIRA
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.REDE
import dev.matheus.fluviapp.model.passagem.Passagem
import dev.matheus.fluviapp.model.viagem.Navio
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Trava o balanço na Fase 2 do ADR-0008: agrega pelo `navioId` CONGELADO na Passagem, não pela
 * Viagem viva nem pelo nome do navio. Consequências verificadas: rename-safe (nome atual do navio
 * vem do repo por id, não do snapshot) e órfão detectável (navioId sem navio → grupo descartado).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BalancoPassagensMapperTest {

    private fun navio(id: String, nome: String) = Navio(
        id = id,
        descricaoNome = nome,
        capacidadeVeiculo = 10,
        capacidadeSuite2 = 4,
        capacidadeSuite3 = 2,
        capacidadeCamarote = 1,
        empresaId = "empresa-1",
    )

    private fun passagem(
        id: String,
        navioId: String,
        navioSnapshot: String,
        acomodacao: String? = REDE.name,
        tipoPassagem: String? = INTEIRA.name,
    ) = Passagem(
        id = id,
        numero = id,
        navioId = navioId,
        // snapshot por valor (pode estar defasado de propósito — não deve ser usado p/ casar)
        navio = navioSnapshot,
        codigoViagem = "PN-IC-001",
        empresa = "Empresa Modelo",
        origem = "Porto Norte",
        destino = "Ilha Central",
        dataViagem = "10/06/2024",
        horaViagem = "12:00",
        acomodacao = acomodacao,
        tipoPassagem = tipoPassagem,
        funcionarioResponsavel = "Operador",
        status = "A_EMITIR",
    )

    private fun mapper(navios: List<Navio>) = BalancoPassagensMapper(
        FakeNavioRepository().apply { this.navios = navios }
    )

    @Test
    fun `agrega por navioId congelado e resolve o nome atual do navio pelo id`() = runTest {
        // navio renomeado: passagens carregam o nome ANTIGO no snapshot; o resultado deve usar o ATUAL.
        val navios = listOf(navio("navio-1", "F/B Nome Novo"))
        val passagens = listOf(
            passagem("1", navioId = "navio-1", navioSnapshot = "F/B Nome Antigo"),
            passagem("2", navioId = "navio-1", navioSnapshot = "F/B Nome Antigo"),
        )

        val balanco = mapper(navios).map(passagens)

        assertEquals(1, balanco.size)
        // nome vem do navio vivo (por id), não do snapshot defasado — rename-safe.
        assertEquals("F/B Nome Novo", balanco.first().navio)
        assertEquals("2", balanco.first().preenchidasRedes)
        assertEquals("2", balanco.first().preenchidasInteiras)
    }

    @Test
    fun `separa grupos por navioId distinto`() = runTest {
        val navios = listOf(navio("navio-1", "F/B Um"), navio("navio-2", "F/B Dois"))
        val passagens = listOf(
            passagem("1", navioId = "navio-1", navioSnapshot = "F/B Um"),
            passagem("2", navioId = "navio-2", navioSnapshot = "F/B Dois"),
            passagem("3", navioId = "navio-2", navioSnapshot = "F/B Dois"),
        )

        val balanco = mapper(navios).map(passagens)

        assertEquals(2, balanco.size)
        assertEquals("1", balanco.first { it.navio == "F/B Um" }.preenchidasRedes)
        assertEquals("2", balanco.first { it.navio == "F/B Dois" }.preenchidasRedes)
    }

    @Test
    fun `descarta grupo orfao quando o navioId nao existe mais`() = runTest {
        val navios = listOf(navio("navio-1", "F/B Um"))
        val passagens = listOf(
            passagem("1", navioId = "navio-1", navioSnapshot = "F/B Um"),
            passagem("2", navioId = "navio-removido", navioSnapshot = "F/B Sumido"),
        )

        val balanco = mapper(navios).map(passagens)

        assertEquals(1, balanco.size)
        assertNull(balanco.find { it.navio == "F/B Sumido" })
        assertTrue(balanco.all { it.navio == "F/B Um" })
    }
}