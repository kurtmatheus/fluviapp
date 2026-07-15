package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.model.viagem.toDocumento
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Round-trip do mapper Navio ↔ NavioDocumento com o link estável `empresaId` (ADR-0008, Fase 0/1).
 */
class NavioDocumentoTest {

    @Test
    fun `toNavio preserva empresa (nome) e empresaId`() {
        val doc = NavioDocumento(
            nome = "FLUVI I",
            capacidadeVeiculo = 12,
            empresa = "ACME",
            empresaId = "e1",
        )

        val navio = doc.toNavio("n1")

        assertEquals("n1", navio.id)
        assertEquals("FLUVI I", navio.descricaoNome)
        assertEquals("ACME", navio.empresa)
        assertEquals("e1", navio.empresaId)
    }

    @Test
    fun `doc antigo sem empresaId desserializa para vazio (schemaless)`() {
        // Firestore preenche o default quando o campo não existe no documento.
        val doc = NavioDocumento(nome = "FLUVI II", empresa = "ACME")

        assertEquals("", doc.empresaId)
        assertEquals("", doc.toNavio("n2").empresaId)
    }

    @Test
    fun `toDocumento leva empresaId de volta ao documento`() {
        val doc = NavioDocumento(nome = "FLUVI I", empresa = "ACME", empresaId = "e1")

        val roundTrip = doc.toNavio("n1").toDocumento()

        assertEquals("ACME", roundTrip.empresa)
        assertEquals("e1", roundTrip.empresaId)
    }
}
