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
    fun `toFuncionarioDocumento le os textos e campos ausentes viram default`() {
        val bruto = DocumentoBruto("a1", mapOf("nome" to "Agente", "lotacao" to "PORTO NORTE"))

        val doc = bruto.toFuncionarioDocumento()

        assertEquals("Agente", doc.nome)
        assertEquals("PORTO NORTE", doc.lotacao)
        assertEquals("", doc.agencia) // ausente → default vazio
    }

    @Test
    fun `toContadorDocumento le o inteiro do contador`() {
        val doc = DocumentoBruto("contador", mapOf("numeroBilhete" to 42L)).toContadorDocumento()

        assertEquals(42, doc.numeroBilhete)
    }

    @Test
    fun `toViagemDocumento coage o mapa de tarifas (valores como Number)`() {
        val bruto = DocumentoBruto(
            "v1",
            mapOf(
                "codigo" to "MAN-STZ-BALV",
                // Firestore devolve número ora Double, ora Long — ambos têm de virar Double.
                "tarifas" to mapOf("REDE" to 300.0, "SUITE" to 450L),
            ),
        )

        val doc = bruto.toViagemDocumento()

        assertEquals(300.0, doc.tarifas["REDE"]!!, 0.0)
        assertEquals(450.0, doc.tarifas["SUITE"]!!, 0.0)
    }

    @Test
    fun `toViagemDocumento sem tarifas vira mapa vazio`() {
        val doc = DocumentoBruto("v1", mapOf("codigo" to "X")).toViagemDocumento()

        assertTrue(doc.tarifas.isEmpty())
    }
}
