package dev.matheus.fluviapp.domain.mappers

import dev.matheus.fluviapp.revitalizacao.ForaDoEscopo
import org.junit.experimental.categories.Category
import dev.matheus.fluviapp.fakes.FakeEmbarcacaoRepository
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Descricao.INTEIRA
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Descricao.REDE
import dev.matheus.fluviapp.database.PassagemEntity
import dev.matheus.fluviapp.domain.viagem.Embarcacao
import dev.matheus.fluviapp.domain.viagem.TipoEmbarcacao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Trava o balanço na Fase 2 do ADR-0008: agrega pelo `embarcacaoId` CONGELADO na PassagemEntity, não pela
 * Viagem viva nem pelo nome da embarcação. Consequências verificadas: rename-safe (nome atual da embarcação
 * vem do repo por id, não do snapshot) e órfão detectável (embarcacaoId sem embarcacao → grupo descartado).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Category(ForaDoEscopo::class)
class ContagemPassagensMapperTest {

    private fun embarcacao(id: String, nome: String) = Embarcacao(
        id = id,
        descricaoNome = nome,
        tipo = TipoEmbarcacao.FERRY_BOAT,
        capacidadeVeiculo = 10,
        capacidadeSuite2 = 4,
        capacidadeSuite3 = 2,
        capacidadeCamarote = 1,
        empresaId = "empresa-1",
    )

    private fun passagem(
        id: String,
        embarcacaoId: String,
        embarcacaoSnapshot: String,
        acomodacao: String? = REDE.name,
        tipoPassagem: String? = INTEIRA.name,
    ) = PassagemEntity(
        id = id,
        numero = id,
        embarcacaoId = embarcacaoId,
        // snapshot por valor (pode estar defasado de propósito — não deve ser usado p/ casar)
        embarcacao = embarcacaoSnapshot,
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

    private fun mapper(embarcacoes: List<Embarcacao>) = ContagemPassagensMapper(
        FakeEmbarcacaoRepository().apply { this.embarcacoes = embarcacoes }
    )

    @Test
    fun `agrega por embarcacaoId congelado e resolve o nome atual da embarcacao pelo id`() = runTest {
        // embarcacao renomeado: passagens carregam o nome ANTIGO no snapshot; o resultado deve usar o ATUAL.
        val embarcacoes = listOf(embarcacao("embarcacao-1", "F/B Nome Novo"))
        val passagens = listOf(
            passagem("1", embarcacaoId = "embarcacao-1", embarcacaoSnapshot = "F/B Nome Antigo"),
            passagem("2", embarcacaoId = "embarcacao-1", embarcacaoSnapshot = "F/B Nome Antigo"),
        )

        val contagem = mapper(embarcacoes).map(passagens)

        assertEquals(1, contagem.size)
        // nome vem do embarcacao vivo (por id), não do snapshot defasado — rename-safe.
        assertEquals("F/B Nome Novo", contagem.first().embarcacao)
        assertEquals("2", contagem.first().preenchidasRedes)
        assertEquals("2", contagem.first().preenchidasInteiras)
    }

    @Test
    fun `separa grupos por embarcacaoId distinto`() = runTest {
        val embarcacoes = listOf(embarcacao("embarcacao-1", "F/B Um"), embarcacao("embarcacao-2", "F/B Dois"))
        val passagens = listOf(
            passagem("1", embarcacaoId = "embarcacao-1", embarcacaoSnapshot = "F/B Um"),
            passagem("2", embarcacaoId = "embarcacao-2", embarcacaoSnapshot = "F/B Dois"),
            passagem("3", embarcacaoId = "embarcacao-2", embarcacaoSnapshot = "F/B Dois"),
        )

        val contagem = mapper(embarcacoes).map(passagens)

        assertEquals(2, contagem.size)
        assertEquals("1", contagem.first { it.embarcacao == "F/B Um" }.preenchidasRedes)
        assertEquals("2", contagem.first { it.embarcacao == "F/B Dois" }.preenchidasRedes)
    }

    @Test
    fun `descarta grupo orfao quando o embarcacaoId nao existe mais`() = runTest {
        val embarcacoes = listOf(embarcacao("embarcacao-1", "F/B Um"))
        val passagens = listOf(
            passagem("1", embarcacaoId = "embarcacao-1", embarcacaoSnapshot = "F/B Um"),
            passagem("2", embarcacaoId = "embarcacao-removido", embarcacaoSnapshot = "F/B Sumido"),
        )

        val contagem = mapper(embarcacoes).map(passagens)

        assertEquals(1, contagem.size)
        assertNull(contagem.find { it.embarcacao == "F/B Sumido" })
        assertTrue(contagem.all { it.embarcacao == "F/B Um" })
    }

    // --- Contagem pura por caso (contarOcupacaoEmbarcacao): suíte por bilhete, trio→3p, solo conta (§7) ---

    @Test
    fun `suite solo conta uma suite no bucket de 2 pessoas`() {
        val solo = passagem("s", embarcacaoId = "embarcacao-1", embarcacaoSnapshot = "x", acomodacao = "SUITE", tipoPassagem = "")
        val dados = contarOcupacaoEmbarcacao(embarcacao("embarcacao-1", "F/B Um"), listOf(solo))
        assertEquals("1", dados.preenchidasSuitesGeral)
        assertEquals("1", dados.preenchidasSuites2Pessoas)
        assertEquals("0", dados.preenchidasSuites3Pessoas)
    }

    @Test
    fun `suite dupla conta uma suite no bucket de 2 pessoas`() {
        val dupla = passagem("s", embarcacaoId = "embarcacao-1", embarcacaoSnapshot = "x", acomodacao = "SUITE", tipoPassagem = "")
            .copy(nomePassageiro2 = "Acomp 2")
        val dados = contarOcupacaoEmbarcacao(embarcacao("embarcacao-1", "F/B Um"), listOf(dupla))
        assertEquals("1", dados.preenchidasSuitesGeral)
        assertEquals("1", dados.preenchidasSuites2Pessoas)
        assertEquals("0", dados.preenchidasSuites3Pessoas)
    }

    @Test
    fun `suite trio conta uma suite no bucket de 3 pessoas`() {
        val trio = passagem("s", embarcacaoId = "embarcacao-1", embarcacaoSnapshot = "x", acomodacao = "SUITE", tipoPassagem = "")
            .copy(nomePassageiro2 = "Acomp 2", nomePassageiro3 = "Acomp 3")
        val dados = contarOcupacaoEmbarcacao(embarcacao("embarcacao-1", "F/B Um"), listOf(trio))
        assertEquals("1", dados.preenchidasSuitesGeral)
        assertEquals("0", dados.preenchidasSuites2Pessoas)
        assertEquals("1", dados.preenchidasSuites3Pessoas)
    }

    @Test
    fun `camarote e veiculo contam por bilhete`() {
        val camarote = passagem("c", embarcacaoId = "embarcacao-1", embarcacaoSnapshot = "x", acomodacao = "CAMAROTE", tipoPassagem = "")
        val carro = passagem("v", embarcacaoId = "embarcacao-1", embarcacaoSnapshot = "x", acomodacao = "", tipoPassagem = "")
            .copy(placaVeiculo = "ABC1D23", tipoVeiculo = "CARRO")
        val dados = contarOcupacaoEmbarcacao(embarcacao("embarcacao-1", "F/B Um"), listOf(camarote, carro))
        assertEquals("1", dados.preenchidosCamarotes)
        assertEquals("1", dados.preenchidosVeiculo)
        assertEquals("1", dados.totalCarros)
    }
}
