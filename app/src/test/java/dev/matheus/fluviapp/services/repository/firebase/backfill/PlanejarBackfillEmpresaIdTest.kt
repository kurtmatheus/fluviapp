package dev.matheus.fluviapp.services.repository.firebase.backfill

import dev.matheus.fluviapp.model.viagem.Empresa
import dev.matheus.fluviapp.model.viagem.Navio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Decisão pura do backfill do `empresaId` (ADR-0008, Fase 0). */
class PlanejarBackfillEmpresaIdTest {

    private fun empresa(id: String, nome: String) =
        Empresa(id, nome, "$nome LTDA", "00", "end", "1", "2")

    private fun navio(id: String, empresa: String, empresaId: String = "") =
        Navio(id, "NAVIO $id", 0, 0, 0, 0, empresa, empresaId)

    @Test
    fun `nome unico resolve para o id da empresa`() {
        val plano = planejarBackfillEmpresaId(
            navios = listOf(navio("n1", empresa = "ACME")),
            empresas = listOf(empresa("e1", "ACME")),
        )

        assertEquals(1, plano.atualizados.size)
        assertEquals(AtualizacaoEmpresaId("n1", "e1", "ACME"), plano.atualizados.first())
        assertFalse(plano.temHomonimos)
    }

    @Test
    fun `navio que ja tem empresaId e pulado (idempotente)`() {
        val plano = planejarBackfillEmpresaId(
            navios = listOf(navio("n1", empresa = "ACME", empresaId = "e1")),
            empresas = listOf(empresa("e1", "ACME")),
        )

        assertTrue(plano.atualizados.isEmpty())
        assertEquals(listOf("n1"), plano.jaTinham)
    }

    @Test
    fun `nome sem empresa correspondente vai para semMatch`() {
        val plano = planejarBackfillEmpresaId(
            navios = listOf(navio("n1", empresa = "FANTASMA")),
            empresas = listOf(empresa("e1", "ACME")),
        )

        assertTrue(plano.atualizados.isEmpty())
        assertEquals(listOf("n1"), plano.semMatch)
    }

    @Test
    fun `nome homonimo nao resolve - vira ambiguo e reporta o nome`() {
        val plano = planejarBackfillEmpresaId(
            navios = listOf(navio("n1", empresa = "ACME")),
            empresas = listOf(empresa("e1", "ACME"), empresa("e2", "ACME")),
        )

        assertTrue(plano.atualizados.isEmpty())
        assertEquals(listOf("n1"), plano.ambiguos)
        assertTrue(plano.temHomonimos)
        assertEquals(setOf("ACME"), plano.nomesHomonimos)
    }

    @Test
    fun `homonimo nao contamina os navios de nome unico`() {
        val plano = planejarBackfillEmpresaId(
            navios = listOf(
                navio("n1", empresa = "ACME"),      // homônimo -> ambíguo
                navio("n2", empresa = "BRAVO"),     // único -> atualiza
                navio("n3", empresa = "ACME"),      // homônimo -> ambíguo
                navio("n4", empresa = "SUMIDA"),    // sem match
                navio("n5", empresa = "BRAVO", empresaId = "e3"), // já tinha
            ),
            empresas = listOf(
                empresa("e1", "ACME"), empresa("e2", "ACME"), empresa("e3", "BRAVO"),
            ),
        )

        assertEquals(listOf(AtualizacaoEmpresaId("n2", "e3", "BRAVO")), plano.atualizados)
        assertEquals(listOf("n1", "n3"), plano.ambiguos)
        assertEquals(listOf("n4"), plano.semMatch)
        assertEquals(listOf("n5"), plano.jaTinham)
        assertEquals(5, plano.totalNavios)
    }
}