package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.domain.rota.Rota
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A fronteira de dados da Rota (ADR-0019 D2), com duas escolhas próprias: a **recusa** por porto
 * ausente e a **não-recusa** por sentido inválido.
 */
class RotaDocumentoTest {

    private val belemParintins = Rota(
        id = "rota-1",
        portoOrigemId = "porto-belem",
        portoDestinoId = "porto-parintins",
        distanciaMn = 420.5,
        tempoMedioH = 30.0,
        criadoPor = "func-1",
        criadoEm = "2026-08-08T10:00:00",
        ativo = true,
    )

    private fun documento(id: String = "rota-1", dados: Map<String, Any?>) = DocumentoBruto(id, dados)

    // --- Leitura ---

    @Test
    fun `toRota le todos os campos`() {
        assertEquals(belemParintins, documento(dados = belemParintins.paraMapa()).toRota())
    }

    /** Sem os dois portos não há ligação nenhuma — recusa estrutural, como o porto sem localidade. */
    @Test
    fun `documento sem um dos portos nao vira rota`() {
        assertNull(documento(dados = mapOf("portoOrigemId" to "porto-a")).toRota())
        assertNull(documento(dados = mapOf("portoDestinoId" to "porto-b")).toRota())
        assertNull(documento(dados = mapOf("portoOrigemId" to "", "portoDestinoId" to "porto-b")).toRota())
    }

    /**
     * **Sentido inválido não é recusa de fronteira**, e a assimetria é deliberada: um documento com os
     * dois portos iguais é dado ruim, não "não é uma rota". Escondê-lo da lista esconderia justamente o
     * que alguém precisa inativar.
     */
    @Test
    fun `documento com origem igual ao destino ainda vira rota — e a lista mostra`() {
        val lida = documento(
            dados = mapOf("portoOrigemId" to "porto-a", "portoDestinoId" to "porto-a")
        ).toRota()

        assertEquals("porto-a", lida?.portoOrigemId)
    }

    /** O Firestore devolve número como Long ou Double conforme o que foi gravado; os dois têm de ler. */
    @Test
    fun `distancia e tempo leem inteiro ou decimal`() {
        val lida = documento(
            dados = mapOf(
                "portoOrigemId" to "porto-a",
                "portoDestinoId" to "porto-b",
                "distanciaMn" to 420L,
                "tempoMedioH" to 30.5,
            )
        ).toRota()

        assertEquals(420.0, lida?.distanciaMn)
        assertEquals(30.5, lida?.tempoMedioH)
    }

    @Test
    fun `ativo ausente vale true`() {
        val lida = documento(
            dados = mapOf("portoOrigemId" to "porto-a", "portoDestinoId" to "porto-b")
        ).toRota()

        assertTrue(lida!!.ativo)
    }

    // --- Escrita ---

    @Test
    fun `paraMapa nao grava o id e guarda a assinatura`() {
        val mapa = belemParintins.paraMapa()

        assertFalse(mapa.containsKey("id"))
        // Num pool sem dono, a assinatura é o que resta de responsabilidade (§7.1).
        assertEquals("func-1", mapa["criadoPor"])
        assertEquals("2026-08-08T10:00:00", mapa["criadoEm"])
    }

    /** Nem cidade, nem tarifa, nem embarcação: as três pertencem a outra entidade (§5, §7.1). */
    @Test
    fun `paraMapa nao grava o que e de outra entidade`() {
        val mapa = belemParintins.paraMapa()

        listOf("origem", "destino", "tarifas", "embarcacaoId", "empresaId").forEach {
            assertFalse("não deveria gravar '$it'", mapa.containsKey(it))
        }
    }

    @Test
    fun `gravar e ler de volta devolve a mesma rota`() {
        assertEquals(
            belemParintins,
            documento(id = belemParintins.id, dados = belemParintins.paraMapa()).toRota(),
        )
    }
}