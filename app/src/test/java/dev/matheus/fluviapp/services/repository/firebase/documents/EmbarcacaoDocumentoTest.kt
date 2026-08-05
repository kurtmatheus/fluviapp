package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.domain.viagem.Embarcacao
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * A **fronteira de dados da Embarcação** (ADR-0019 D2): `Map` → domínio na leitura, domínio → `Map` na
 * escrita. É a única parte do `EmbarcacaoFirestoreRepository` que roda sem SDK — o resto do CRUD é da
 * `ColecaoFirestore`, testada uma vez para todas as coleções.
 *
 * Trocou de forma junto com a entidade: o teste antigo cobria o salto
 * `EmbarcacaoDocumento → Embarcacao`, que deixou de existir quando a fronteira virou `Map`.
 */
class EmbarcacaoDocumentoTest {

    private val embarcacao = Embarcacao(
        id = "emb-1",
        descricaoNome = "FLUVI I",
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

        assertEquals("outro", lida.id)
    }

    /**
     * O Firestore devolve inteiro como `Long`. Ler direto como `Int` estouraria — o `inteiro()` coage, e
     * é a área de risco que este teste existe para travar.
     */
    @Test
    fun `capacidades coagem Number para Int`() {
        val lida = documento(
            dados = mapOf("nome" to "FLUVI II", "capacidadeVeiculo" to 10L, "capacidadeCamarote" to 3L),
        ).toEmbarcacao()

        assertEquals(10, lida.capacidadeVeiculo)
        assertEquals(3, lida.capacidadeCamarote)
    }

    /** Capacidade ausente é capacidade nenhuma: embarcação sem lugar declarado não vende lugar. */
    @Test
    fun `campos ausentes viram zero e vazio em vez de falhar`() {
        val lida = documento(dados = mapOf("nome" to "SÓ O NOME")).toEmbarcacao()

        assertEquals("SÓ O NOME", lida.descricaoNome)
        assertEquals(0, lida.capacidadeVeiculo)
        assertEquals(0, lida.capacidadeSuite2)
        assertEquals(0, lida.capacidadeSuite3)
        assertEquals(0, lida.capacidadeCamarote)
        assertEquals("", lida.empresaId)
    }

    /** Documento de antes do vínculo por id (ADR-0008): sem `empresaId`, e a leitura não quebra. */
    @Test
    fun `documento sem empresaId desserializa para vazio`() {
        assertEquals("", documento(dados = mapOf("nome" to "FLUVI III")).toEmbarcacao().empresaId)
    }

    // --- Escrita: domínio -> Map ---

    @Test
    fun `paraMapa nao grava o id`() {
        assertFalse(embarcacao.paraMapa().containsKey("id"))
    }

    @Test
    fun `gravar e ler de volta devolve a mesma embarcacao`() {
        assertEquals(embarcacao, documento(id = embarcacao.id, dados = embarcacao.paraMapa()).toEmbarcacao())
    }
}