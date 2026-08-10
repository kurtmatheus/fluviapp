package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.domain.viagem.MINUTOS_POR_DIA
import dev.matheus.fluviapp.domain.viagem.Viagem
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

/**
 * A fronteira de dados da Viagem (ADR-0019 D2), com três recusas e uma **não-recusa** deliberada.
 */
class ViagemDocumentoTest {

    private val tercaAsSeis = Viagem(
        id = "viagem-1",
        rotaId = "rota-1",
        embarcacaoId = "emb-1",
        diaSemana = DayOfWeek.TUESDAY,
        horaMin = 18 * 60,
        criadoPor = "func-1",
        criadoEm = "2026-08-10T10:00:00",
        ativo = true,
    )

    private fun documento(id: String = "viagem-1", dados: Map<String, Any?>) = DocumentoBruto(id, dados)

    // --- Leitura ---

    @Test
    fun `toViagem le todos os campos`() {
        assertEquals(tercaAsSeis, documento(dados = tercaAsSeis.paraMapa()).toViagem())
    }

    /** O Firestore devolve número como `Long` ou `Double` conforme o que foi gravado. */
    @Test
    fun `le a hora gravada como Long`() {
        val dados = tercaAsSeis.paraMapa() + ("horaMin" to 1_080L)

        assertEquals(18 * 60, documento(dados = dados).toViagem()?.horaMin)
    }

    // --- Recusas estruturais ---

    @Test
    fun `documento sem rota nao vira viagem`() {
        assertNull(documento(dados = tercaAsSeis.paraMapa() - "rotaId").toViagem())
        assertNull(documento(dados = tercaAsSeis.paraMapa() + ("rotaId" to "")).toViagem())
    }

    @Test
    fun `documento sem embarcacao nao vira viagem`() {
        assertNull(documento(dados = tercaAsSeis.paraMapa() - "embarcacaoId").toViagem())
        assertNull(documento(dados = tercaAsSeis.paraMapa() + ("embarcacaoId" to "")).toViagem())
    }

    /**
     * O dia é invariante, não campo opcional — precedente de `Embarcacao.tipo`. Documento com dia ausente
     * ou ilegível some da lista em vez de derrubar a coleção inteira.
     */
    @Test
    fun `documento sem dia conhecido nao vira viagem`() {
        assertNull(documento(dados = tercaAsSeis.paraMapa() - "diaSemana").toViagem())
        assertNull(documento(dados = tercaAsSeis.paraMapa() + ("diaSemana" to "")).toViagem())
        assertNull(documento(dados = tercaAsSeis.paraMapa() + ("diaSemana" to "TERCA")).toViagem())
        assertNull(documento(dados = tercaAsSeis.paraMapa() + ("diaSemana" to 2)).toViagem())
    }

    /**
     * **Os documentos da Viagem-trecho, demolida na F8.0, são recusados por consequência** — nenhum deles
     * tem `rotaId`. É o mesmo desfecho de `navios/` depois do rename da Flotilha: ficam lá, invisíveis.
     */
    @Test
    fun `documento da viagem-trecho antiga e recusado`() {
        val antigo = mapOf(
            "codigo" to "PN-IC-001",
            "origem" to "Porto Norte",
            "destino" to "Ilha Central",
            "empresaId" to "1",
            "embarcacaoId" to "emb-1",
        )

        assertNull(documento(dados = antigo).toViagem())
    }

    // --- A não-recusa ---

    /**
     * Hora fora do relógio **passa**, e a assimetria é a mesma da Rota com origem=destino: é dado ruim,
     * não "não é uma viagem", e escondê-la esconderia justamente o que alguém precisa inativar. Quem
     * cobra a faixa é `temSentido()`, no cadastro.
     */
    @Test
    fun `hora fora do relogio nao impede de ler o documento`() {
        val dados = tercaAsSeis.paraMapa() + ("horaMin" to MINUTOS_POR_DIA + 5)
        val viagem = documento(dados = dados).toViagem()

        assertNotNull(viagem)
        assertEquals(MINUTOS_POR_DIA + 5, viagem?.horaMin)
    }

    /** Documento antigo sem `ativo` é registro **em uso** — assumir `false` esconderia dado bom. */
    @Test
    fun `documento sem ativo nasce ativo`() {
        assertTrue(documento(dados = tercaAsSeis.paraMapa() - "ativo").toViagem()!!.ativo)
    }

    @Test
    fun `ativo falso e respeitado`() {
        assertFalse(documento(dados = tercaAsSeis.paraMapa() + ("ativo" to false)).toViagem()!!.ativo)
    }

    // --- Escrita ---

    /** O `id` é o nome do documento; gravá-lo dentro criaria duas verdades para a mesma pergunta. */
    @Test
    fun `paraMapa nao grava o id`() {
        assertFalse(tercaAsSeis.paraMapa().containsKey("id"))
    }

    @Test
    fun `grava o dia como name, nao como numero`() {
        assertEquals("TUESDAY", tercaAsSeis.paraMapa()["diaSemana"])
    }

    @Test
    fun `ida e volta preserva a viagem`() {
        val ida = tercaAsSeis.paraMapa()

        assertEquals(tercaAsSeis, DocumentoBruto("viagem-1", ida).toViagem())
    }
}