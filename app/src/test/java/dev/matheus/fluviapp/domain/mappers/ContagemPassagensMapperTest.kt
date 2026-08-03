package dev.matheus.fluviapp.domain.mappers

import dev.matheus.fluviapp.fakes.FakeNavioRepository
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Descricao.INTEIRA
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Descricao.REDE
import dev.matheus.fluviapp.domain.passagem.Passagem
import dev.matheus.fluviapp.domain.viagem.Navio
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
class ContagemPassagensMapperTest {

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

    private fun mapper(navios: List<Navio>) = ContagemPassagensMapper(
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

        val contagem = mapper(navios).map(passagens)

        assertEquals(1, contagem.size)
        // nome vem do navio vivo (por id), não do snapshot defasado — rename-safe.
        assertEquals("F/B Nome Novo", contagem.first().navio)
        assertEquals("2", contagem.first().preenchidasRedes)
        assertEquals("2", contagem.first().preenchidasInteiras)
    }

    @Test
    fun `separa grupos por navioId distinto`() = runTest {
        val navios = listOf(navio("navio-1", "F/B Um"), navio("navio-2", "F/B Dois"))
        val passagens = listOf(
            passagem("1", navioId = "navio-1", navioSnapshot = "F/B Um"),
            passagem("2", navioId = "navio-2", navioSnapshot = "F/B Dois"),
            passagem("3", navioId = "navio-2", navioSnapshot = "F/B Dois"),
        )

        val contagem = mapper(navios).map(passagens)

        assertEquals(2, contagem.size)
        assertEquals("1", contagem.first { it.navio == "F/B Um" }.preenchidasRedes)
        assertEquals("2", contagem.first { it.navio == "F/B Dois" }.preenchidasRedes)
    }

    @Test
    fun `descarta grupo orfao quando o navioId nao existe mais`() = runTest {
        val navios = listOf(navio("navio-1", "F/B Um"))
        val passagens = listOf(
            passagem("1", navioId = "navio-1", navioSnapshot = "F/B Um"),
            passagem("2", navioId = "navio-removido", navioSnapshot = "F/B Sumido"),
        )

        val contagem = mapper(navios).map(passagens)

        assertEquals(1, contagem.size)
        assertNull(contagem.find { it.navio == "F/B Sumido" })
        assertTrue(contagem.all { it.navio == "F/B Um" })
    }

    // --- Contagem pura por caso (contarOcupacaoNavio): suíte por bilhete, trio→3p, solo conta (§7) ---

    @Test
    fun `suite solo conta uma suite no bucket de 2 pessoas`() {
        val solo = passagem("s", navioId = "navio-1", navioSnapshot = "x", acomodacao = "SUITE", tipoPassagem = "")
        val dados = contarOcupacaoNavio(navio("navio-1", "F/B Um"), listOf(solo))
        assertEquals("1", dados.preenchidasSuitesGeral)
        assertEquals("1", dados.preenchidasSuites2Pessoas)
        assertEquals("0", dados.preenchidasSuites3Pessoas)
    }

    @Test
    fun `suite dupla conta uma suite no bucket de 2 pessoas`() {
        val dupla = passagem("s", navioId = "navio-1", navioSnapshot = "x", acomodacao = "SUITE", tipoPassagem = "")
            .copy(nomePassageiro2 = "Acomp 2")
        val dados = contarOcupacaoNavio(navio("navio-1", "F/B Um"), listOf(dupla))
        assertEquals("1", dados.preenchidasSuitesGeral)
        assertEquals("1", dados.preenchidasSuites2Pessoas)
        assertEquals("0", dados.preenchidasSuites3Pessoas)
    }

    @Test
    fun `suite trio conta uma suite no bucket de 3 pessoas`() {
        val trio = passagem("s", navioId = "navio-1", navioSnapshot = "x", acomodacao = "SUITE", tipoPassagem = "")
            .copy(nomePassageiro2 = "Acomp 2", nomePassageiro3 = "Acomp 3")
        val dados = contarOcupacaoNavio(navio("navio-1", "F/B Um"), listOf(trio))
        assertEquals("1", dados.preenchidasSuitesGeral)
        assertEquals("0", dados.preenchidasSuites2Pessoas)
        assertEquals("1", dados.preenchidasSuites3Pessoas)
    }

    @Test
    fun `camarote e veiculo contam por bilhete`() {
        val camarote = passagem("c", navioId = "navio-1", navioSnapshot = "x", acomodacao = "CAMAROTE", tipoPassagem = "")
        val carro = passagem("v", navioId = "navio-1", navioSnapshot = "x", acomodacao = "", tipoPassagem = "")
            .copy(placaVeiculo = "ABC1D23", tipoVeiculo = "CARRO")
        val dados = contarOcupacaoNavio(navio("navio-1", "F/B Um"), listOf(camarote, carro))
        assertEquals("1", dados.preenchidosCamarotes)
        assertEquals("1", dados.preenchidosVeiculo)
        assertEquals("1", dados.totalCarros)
    }
}