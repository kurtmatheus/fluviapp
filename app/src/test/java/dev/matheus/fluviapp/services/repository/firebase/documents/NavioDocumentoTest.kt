package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.revitalizacao.ForaDoEscopo
import org.junit.experimental.categories.Category
import dev.matheus.fluviapp.domain.viagem.toDocumento
import org.junit.Assert.assertEquals
import org.junit.Test

/** Round-trip do mapper Navio ↔ NavioDocumento — vínculo com Empresa só por id (ADR-0008, Fase 3). */
@Category(ForaDoEscopo::class)
class NavioDocumentoTest {

    @Test
    fun `toNavio preserva empresaId`() {
        val doc = NavioDocumento(nome = "FLUVI I", capacidadeVeiculo = 12, empresaId = "e1")

        val navio = doc.toNavio("n1")

        assertEquals("n1", navio.id)
        assertEquals("FLUVI I", navio.descricaoNome)
        assertEquals("e1", navio.empresaId)
    }

    @Test
    fun `doc antigo sem empresaId desserializa para vazio (schemaless)`() {
        // Firestore preenche o default quando o campo não existe no documento.
        val doc = NavioDocumento(nome = "FLUVI II")

        assertEquals("", doc.empresaId)
        assertEquals("", doc.toNavio("n2").empresaId)
    }

    @Test
    fun `toDocumento leva empresaId de volta ao documento`() {
        val doc = NavioDocumento(nome = "FLUVI I", empresaId = "e1")

        val roundTrip = doc.toNavio("n1").toDocumento()

        assertEquals("e1", roundTrip.empresaId)
    }
}
