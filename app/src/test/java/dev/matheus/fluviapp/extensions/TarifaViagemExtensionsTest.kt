package dev.matheus.fluviapp.extensions

import dev.matheus.fluviapp.domain.viagem.TarifaViagem
import dev.matheus.fluviapp.services.repository.firebase.documents.ViagemDocumento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Costura mapa↔linhas da tabela de tarifas da Viagem (ADR-0013). Trava o achatamento nas duas direções
 * e o round-trip (a forma do Firestore e a do Room têm de casar).
 */
class TarifaViagemExtensionsTest {

    @Test
    fun `mapa do doc vira linhas atadas ao viagemId`() {
        val doc = ViagemDocumento(tarifas = mapOf("REDE" to 300.0, "SUITE" to 450.0))

        val linhas = doc.tarifasParaLinhas("v1")

        assertEquals(2, linhas.size)
        assertTrue(linhas.contains(TarifaViagem("v1", "REDE", 300.0)))
        assertTrue(linhas.contains(TarifaViagem("v1", "SUITE", 450.0)))
    }

    @Test
    fun `doc sem tarifas vira lista vazia`() {
        assertTrue(ViagemDocumento().tarifasParaLinhas("v1").isEmpty())
    }

    @Test
    fun `linhas viram mapa chave-valor`() {
        val linhas = listOf(
            TarifaViagem("v1", "REDE", 300.0),
            TarifaViagem("v1", "CAMAROTE", 500.0),
        )

        assertEquals(mapOf("REDE" to 300.0, "CAMAROTE" to 500.0), linhas.paraMapaTarifas())
    }

    @Test
    fun `round-trip mapa - linhas - mapa preserva`() {
        val original = mapOf("REDE" to 300.0, "SUITE" to 450.0, "CARRO" to 200.0)

        val roundtrip = ViagemDocumento(tarifas = original)
            .tarifasParaLinhas("v1")
            .paraMapaTarifas()

        assertEquals(original, roundtrip)
    }
}