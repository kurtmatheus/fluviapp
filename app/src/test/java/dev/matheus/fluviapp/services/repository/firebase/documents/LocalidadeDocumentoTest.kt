package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.domain.localidade.Localidade
import dev.matheus.fluviapp.domain.localidade.Uf
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A fronteira de dados da Localidade (ADR-0019 D2), com dois pontos próprios: a **recusa** por UF ilegível
 * e o **padrão do `ativo`**, que é o único fail-*open* do projeto — e tem razão para ser.
 */
class LocalidadeDocumentoTest {

    private val belem = Localidade(
        id = "loc-1",
        municipio = "Belém",
        uf = Uf.PA,
        codigoIbge = "1501402",
        ativo = true,
    )

    private fun documento(id: String = "loc-1", dados: Map<String, Any?>) = DocumentoBruto(id, dados)

    // --- Leitura ---

    @Test
    fun `toLocalidade le todos os campos`() {
        assertEquals(belem, documento(dados = belem.paraMapa()).toLocalidade())
    }

    @Test
    fun `toLocalidade tira o id do documento`() {
        assertEquals("outro", documento(id = "outro", dados = belem.paraMapa()).toLocalidade()?.id)
    }

    /** UF é tipo fechado: documento com sigla ilegível não vira localidade de UF padrão — não vira nada. */
    @Test
    fun `documento com uf desconhecida nao vira localidade`() {
        assertNull(documento(dados = mapOf("municipio" to "X", "uf" to "XX")).toLocalidade())
        assertNull(documento(dados = mapOf("municipio" to "X")).toLocalidade())
    }

    /** A tolerância que o `Uf.de` já tinha: o dado nasceu no catálogo escrito por extenso. */
    @Test
    fun `uf por extenso ainda e lida`() {
        val lida = documento(dados = mapOf("municipio" to "Belém", "uf" to "Pará")).toLocalidade()

        assertEquals(Uf.PA, lida?.uf)
    }

    /**
     * Documento gravado antes do delete lógico existir é uma localidade **em uso**. Este é o único
     * campo do projeto cuja ausência vale `true`: ele governa visibilidade, não permissão, e assumir
     * `false` esconderia dado bom em vez de proteger alguma coisa.
     */
    @Test
    fun `ativo ausente vale true`() {
        val lida = documento(dados = mapOf("municipio" to "Belém", "uf" to "PA")).toLocalidade()

        assertTrue(lida!!.ativo)
    }

    @Test
    fun `ativo false e lido como false`() {
        val lida = documento(dados = belem.copy(ativo = false).paraMapa()).toLocalidade()

        assertFalse(lida!!.ativo)
    }

    // --- Escrita ---

    @Test
    fun `paraMapa nao grava o id e grava a sigla da uf`() {
        val mapa = belem.paraMapa()

        assertFalse(mapa.containsKey("id"))
        assertEquals("PA", mapa["uf"])
    }

    @Test
    fun `gravar e ler de volta devolve a mesma localidade`() {
        assertEquals(belem, documento(id = belem.id, dados = belem.paraMapa()).toLocalidade())
    }

    @Test
    fun `o rotulo junta municipio e sigla`() {
        assertEquals("Belém/PA", belem.rotulo)
    }
}