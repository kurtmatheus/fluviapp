package dev.matheus.fluviapp.ui.viewmodel.helpers.inicio

import dev.matheus.fluviapp.domain.rota.Rota
import dev.matheus.fluviapp.domain.viagem.InicioDoPainel
import dev.matheus.fluviapp.domain.viagem.Viagem
import dev.matheus.fluviapp.domain.viagem.ViagemSemana
import dev.matheus.fluviapp.ui.states.InicioDaTela
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * A tradução domínio → tela do Início (F8.4).
 *
 * Ela vive fora do `MainScreenViewModel` porque aquele depende de `FirebaseAuth` e de `DataStore` e não se
 * constrói numa JVM — o que é formatação passou a ser exercitável sem Android, e o que ficou lá é
 * orquestração.
 */
class InicioDaTelaMapperTest {

    /** Terça, 11 de agosto de 2026. */
    private val terca = LocalDate.of(2026, 8, 11)

    private val rota = Rota("r1", "porto-a", "porto-b", distanciaMn = 420.0, tempoMedioH = 30.0)
    private val rotaCurta = Rota("r2", "porto-a", "porto-b", distanciaMn = 60.0, tempoMedioH = 6.0)

    private val rotasPorId = mapOf("r1" to rota, "r2" to rotaCurta)
    private val portosPorId = mapOf(
        "porto-a" to "Porto de Val-de-Cães · Belém/PA",
        "porto-b" to "Porto de Parintins · Parintins/AM",
    )
    private val embarcacoes = mapOf("e1" to "F/B Modelo")

    /** O dia da viagem acompanha a data: é o invariante que `disponiveisAPartirDe` garante. */
    private fun ocorrencia(rotaId: String = "r1", horaMin: Int = 18 * 60, data: LocalDate = terca) =
        ViagemSemana(Viagem("v1", rotaId, "e1", data.dayOfWeek, horaMin), data)

    private fun telaCom(vararg ocorrencias: ViagemSemana) =
        InicioDoPainel.DaEmpresa(ocorrencias.toList())
            .paraTela(rotasPorId, portosPorId, embarcacoes) as InicioDaTela.DaEmpresa

    // --- As faces que atravessam sem dado ---

    /** Não há o que formatar quando a resposta é "este painel não é este". */
    @Test
    fun `plataforma e sem-concessao atravessam sem cards`() {
        assertEquals(
            InicioDaTela.DaPlataforma,
            InicioDoPainel.DaPlataforma.paraTela(rotasPorId, portosPorId, embarcacoes),
        )
        assertEquals(
            InicioDaTela.SemConcessao,
            InicioDoPainel.SemConcessao.paraTela(rotasPorId, portosPorId, embarcacoes),
        )
    }

    @Test
    fun `empresa sem saida vira lista vazia, nao outro estado`() {
        val tela = InicioDoPainel.DaEmpresa(emptyList())
            .paraTela(rotasPorId, portosPorId, embarcacoes)

        assertTrue((tela as InicioDaTela.DaEmpresa).disponiveis.isEmpty())
    }

    // --- A partida ---

    /**
     * O dia da semana responde *quando sai*; a data responde *qual delas*. A viagem é semanal, então o
     * dia sozinho é ambíguo entre as ocorrências — e é por isso que os dois aparecem.
     */
    @Test
    fun `a partida traz dia da semana, data e hora`() {
        val card = telaCom(ocorrencia()).disponiveis.single()

        assertEquals("Terça-feira, 11/08 · 18:00", card.partida)
    }

    @Test
    fun `o id identifica a ocorrencia, e o viagemId a viagem`() {
        val card = telaCom(ocorrencia()).disponiveis.single()

        assertEquals("v1@2026-08-11", card.id)
        assertEquals("v1", card.viagemId)
    }

    @Test
    fun `a rota vira o par de portos com cidade, e a embarcacao o nome`() {
        val card = telaCom(ocorrencia()).disponiveis.single()

        assertEquals(
            "Porto de Val-de-Cães · Belém/PA → Porto de Parintins · Parintins/AM",
            card.rota,
        )
        assertEquals("F/B Modelo", card.embarcacao)
    }

    // --- A chegada ---

    /** Terça 18h + 30h = quinta 00:00: sem o dia, "chega às 00:00" parece a madrugada da saída. */
    @Test
    fun `a chegada mostra o dia quando a travessia o atravessa`() {
        val card = telaCom(ocorrencia()).disponiveis.single()

        assertEquals("Qui 00:00", card.chegada)
    }

    /** No mesmo dia o dia seria ruído: só a hora. */
    @Test
    fun `a chegada no mesmo dia mostra so a hora`() {
        val card = telaCom(ocorrencia(rotaId = "r2", horaMin = 6 * 60)).disponiveis.single()

        assertEquals("12:00", card.chegada)
    }

    // --- Os buracos ---

    /**
     * Rota fora do mapa **não derruba o card**: a ocorrência existe, e some-la esconderia do operador uma
     * saída que ele pode vender. O que fica vazio é o que não se soube resolver.
     */
    @Test
    fun `rota desconhecida deixa o card sem rota e sem chegada`() {
        val card = telaCom(ocorrencia(rotaId = "sumida")).disponiveis.single()

        assertEquals("", card.rota)
        assertEquals("", card.chegada)
        assertEquals("Terça-feira, 11/08 · 18:00", card.partida)
    }

    @Test
    fun `porto desconhecido cai no proprio id, e nao em branco`() {
        val card = InicioDoPainel.DaEmpresa(listOf(ocorrencia()))
            .paraTela(rotasPorId, emptyMap(), embarcacoes)
            .let { (it as InicioDaTela.DaEmpresa).disponiveis.single() }

        assertEquals("porto-a → porto-b", card.rota)
    }

    @Test
    fun `embarcacao desconhecida deixa o campo vazio`() {
        val card = InicioDoPainel.DaEmpresa(listOf(ocorrencia()))
            .paraTela(rotasPorId, portosPorId, emptyMap())
            .let { (it as InicioDaTela.DaEmpresa).disponiveis.single() }

        assertEquals("", card.embarcacao)
    }

    // --- Ordem preservada ---

    /** Quem ordena é o domínio (pela partida); o mapeador não reordena. */
    @Test
    fun `a ordem que o dominio deu e preservada`() {
        val tela = telaCom(
            ocorrencia(horaMin = 6 * 60),
            ocorrencia(horaMin = 18 * 60, data = terca.plusDays(3)),
        )

        assertEquals(
            listOf("Terça-feira, 11/08 · 06:00", "Sexta-feira, 14/08 · 18:00"),
            tela.disponiveis.map { it.partida },
        )
    }
}