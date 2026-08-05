package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.domain.viagem.Embarcacao
import dev.matheus.fluviapp.domain.viagem.TipoEmbarcacao
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * A **fronteira de dados da Embarcação** (ADR-0019 D2): `Map` → domínio na leitura, domínio → `Map` na
 * escrita. É a única parte do `EmbarcacaoFirestoreRepository` que roda sem SDK — o resto do CRUD é da
 * `ColecaoFirestore`, testada uma vez para todas as coleções.
 *
 * Aqui mora o teste do **invariante**: não existe embarcação sem tipo, então esta é a primeira leitura do
 * projeto que pode devolver `null`. Os casos abaixo separam as duas famílias de campo — os que têm
 * fail-closed por valor (capacidade ausente = `0`) e o que só tem fail-closed por **recusa**.
 */
class EmbarcacaoDocumentoTest {

    private val embarcacao = Embarcacao(
        id = "emb-1",
        descricaoNome = "FLUVI I",
        tipo = TipoEmbarcacao.FERRY_BOAT,
        capacidadeVeiculo = 12,
        capacidadeSuite2 = 4,
        capacidadeSuite3 = 2,
        capacidadeCamarote = 6,
        empresaId = "emp-1",
    )

    private fun documento(id: String = "emb-1", dados: Map<String, Any?>) = DocumentoBruto(id, dados)

    // --- Leitura: Map -> domínio ---

    @Test
    fun `toEmbarcacao le todos os campos do documento`() {
        assertEquals(embarcacao, documento(dados = embarcacao.paraMapa()).toEmbarcacao())
    }

    /** A identidade vem do nome do documento, nunca do corpo. */
    @Test
    fun `toEmbarcacao tira o id do documento`() {
        val lida = documento(id = "outro", dados = embarcacao.paraMapa()).toEmbarcacao()

        assertEquals("outro", lida?.id)
    }

    /**
     * O Firestore devolve inteiro como `Long`. Ler direto como `Int` estouraria — o `inteiro()` coage, e
     * é a área de risco que este teste existe para travar.
     */
    @Test
    fun `capacidades coagem Number para Int`() {
        val lida = documento(
            dados = mapOf(
                "nome" to "FLUVI II",
                "tipo" to "FERRY_BOAT",
                "capacidadeVeiculo" to 10L,
                "capacidadeCamarote" to 3L,
            ),
        ).toEmbarcacao()

        assertEquals(10, lida?.capacidadeVeiculo)
        assertEquals(3, lida?.capacidadeCamarote)
    }

    /** Capacidade ausente é capacidade nenhuma: embarcação sem lugar declarado não vende lugar. */
    @Test
    fun `campos ausentes viram zero e vazio em vez de falhar`() {
        val lida = documento(dados = mapOf("nome" to "SÓ O NOME", "tipo" to "LANCHA")).toEmbarcacao()

        assertEquals("SÓ O NOME", lida?.descricaoNome)
        assertEquals(0, lida?.capacidadeVeiculo)
        assertEquals(0, lida?.capacidadeSuite2)
        assertEquals(0, lida?.capacidadeSuite3)
        assertEquals(0, lida?.capacidadeCamarote)
        assertEquals("", lida?.empresaId)
    }

    /** Documento de antes do vínculo por id (ADR-0008): sem `empresaId`, e a leitura não quebra. */
    @Test
    fun `documento sem empresaId desserializa para vazio`() {
        val lida = documento(dados = mapOf("nome" to "FLUVI III", "tipo" to "NAVIO")).toEmbarcacao()

        assertEquals("", lida?.empresaId)
    }

    // --- O invariante: não existe embarcação sem tipo ---

    /**
     * Documento gravado antes do campo existir. Não vira embarcação de tipo padrão — não vira nada: um
     * padrão seria uma afirmação inventada sobre o que ela transporta.
     */
    @Test
    fun `documento sem tipo nao vira embarcacao`() {
        assertNull(documento(dados = mapOf("nome" to "SEM TIPO", "capacidadeVeiculo" to 10L)).toEmbarcacao())
    }

    /** Valor que o app não conhece (catamarã, lixo, tipo removido): mesma recusa. */
    @Test
    fun `documento com tipo desconhecido nao vira embarcacao`() {
        assertNull(documento(dados = mapOf("nome" to "X", "tipo" to "CATAMARA")).toEmbarcacao())
        assertNull(documento(dados = mapOf("nome" to "X", "tipo" to "")).toEmbarcacao())
    }

    /**
     * O tipo gravado é o `name`, e o **rótulo não serve** para ler de volta. Parece rigor inútil até
     * lembrar do efeito contrário: se "Ferry Boat" fosse aceito na leitura, renomear o rótulo na tela
     * apagaria silenciosamente a frota inteira do app.
     */
    @Test
    fun `o rotulo de tela nao e aceito no lugar do name`() {
        assertNull(documento(dados = mapOf("nome" to "X", "tipo" to "Lancha Rápida")).toEmbarcacao())
    }

    // --- Escrita: domínio -> Map ---

    @Test
    fun `paraMapa nao grava o id`() {
        assertFalse(embarcacao.paraMapa().containsKey("id"))
    }

    @Test
    fun `paraMapa grava o name do tipo, nao o rotulo`() {
        assertEquals("FERRY_BOAT", embarcacao.paraMapa()["tipo"])
    }

    @Test
    fun `gravar e ler de volta devolve a mesma embarcacao`() {
        assertEquals(embarcacao, documento(id = embarcacao.id, dados = embarcacao.paraMapa()).toEmbarcacao())
    }

    /** A propriedade vale para todos os tipos, e não só para o que o exemplo usa. */
    @Test
    fun `o round-trip fecha para todo tipo de embarcacao`() {
        TipoEmbarcacao.entries.forEach { tipo ->
            val original = embarcacao.copy(tipo = tipo)
            assertEquals(original, documento(id = original.id, dados = original.paraMapa()).toEmbarcacao())
        }
    }
}