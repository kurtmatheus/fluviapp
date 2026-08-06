package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.domain.porto.Porto
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A fronteira de dados do Porto (ADR-0019 D2). Dois pontos próprios: a **recusa** por referência ausente
 * — que é estrutural, e não estética — e o `ativo` ausente valendo `true`, como na Localidade.
 */
class PortoDocumentoTest {

    private val valDeCaes = Porto(
        id = "porto-1",
        nome = "Porto de Val-de-Cães",
        localidadeId = "loc-1",
        ativo = true,
    )

    private fun documento(id: String = "porto-1", dados: Map<String, Any?>) = DocumentoBruto(id, dados)

    // --- Leitura ---

    @Test
    fun `toPorto le todos os campos`() {
        assertEquals(valDeCaes, documento(dados = valDeCaes.paraMapa()).toPorto())
    }

    @Test
    fun `toPorto tira o id do documento`() {
        assertEquals("outro", documento(id = "outro", dados = valDeCaes.paraMapa()).toPorto()?.id)
    }

    /**
     * Sem `localidadeId` não há porto: a referência é o que o coloca em algum lugar do mundo. Deixar
     * passar produziria uma linha sem lugar que nenhuma tela conserta.
     */
    @Test
    fun `documento sem localidade nao vira porto`() {
        assertNull(documento(dados = mapOf("nome" to "Porto Central")).toPorto())
        assertNull(documento(dados = mapOf("nome" to "Porto Central", "localidadeId" to "")).toPorto())
    }

    /** Nome vazio é cadastro malfeito, não "não é porto" — e a diferença decide quem some da lista. */
    @Test
    fun `documento sem nome ainda vira porto`() {
        val lido = documento(dados = mapOf("localidadeId" to "loc-1")).toPorto()

        assertEquals("", lido?.nome)
        assertEquals("loc-1", lido?.localidadeId)
    }

    @Test
    fun `ativo ausente vale true`() {
        val lido = documento(dados = mapOf("nome" to "Porto Central", "localidadeId" to "loc-1")).toPorto()

        assertTrue(lido!!.ativo)
    }

    @Test
    fun `ativo false e lido como false`() {
        val lido = documento(dados = valDeCaes.copy(ativo = false).paraMapa()).toPorto()

        assertFalse(lido!!.ativo)
    }

    // --- Escrita ---

    @Test
    fun `paraMapa nao grava o id e grava a localidade por referencia`() {
        val mapa = valDeCaes.paraMapa()

        assertFalse(mapa.containsKey("id"))
        assertEquals("loc-1", mapa["localidadeId"])
        // O rótulo da localidade **não** é gravado: seria a cópia viva que o §5 recusa.
        assertFalse(mapa.containsKey("localidade"))
        assertFalse(mapa.containsKey("municipio"))
        assertFalse(mapa.containsKey("uf"))
    }

    @Test
    fun `gravar e ler de volta devolve o mesmo porto`() {
        assertEquals(valDeCaes, documento(id = valDeCaes.id, dados = valDeCaes.paraMapa()).toPorto())
    }
}