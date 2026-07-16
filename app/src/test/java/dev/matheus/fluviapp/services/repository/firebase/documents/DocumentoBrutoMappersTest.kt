package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Trava a desserialização manual Map→Documento (§10 Nível 2 — substitui o `toObject` do Firestore).
 * É a área de risco (coerção de tipos): inteiros chegam como Number, ausentes viram default.
 */
class DocumentoBrutoMappersTest {

    @Test
    fun `toNavioDocumento coage inteiros vindos como Number`() {
        val bruto = DocumentoBruto(
            "n1",
            mapOf("nome" to "F/B", "capacidadeVeiculo" to 10L, "capacidadeSuite2" to 4L, "empresaId" to "e1"),
        )

        val doc = bruto.toNavioDocumento()

        assertEquals("F/B", doc.nome)
        assertEquals(10, doc.capacidadeVeiculo)
        assertEquals(4, doc.capacidadeSuite2)
        assertEquals("e1", doc.empresaId)
    }

    @Test
    fun `toAgenteDocumento le boolean e campos ausentes viram default`() {
        val bruto = DocumentoBruto("a1", mapOf("nome" to "Agente", "podeSelecionarFormaPagamento" to true))

        val doc = bruto.toAgenteDocumento()

        assertEquals("Agente", doc.nome)
        assertEquals("", doc.agencia) // ausente → default vazio
        assertTrue(doc.podeSelecionarFormaPagamento)
    }

    @Test
    fun `toContadorDocumento le o inteiro do contador`() {
        val doc = DocumentoBruto("contador", mapOf("numeroBilhete" to 42L)).toContadorDocumento()

        assertEquals(42, doc.numeroBilhete)
    }
}
