package dev.matheus.fluviapp.domain.rota

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A Rota (ADR-0016 §7.1, F7) — o pool compartilhado.
 *
 * Duas regras puras respondem por ela: **sentido** (uma travessia liga lugares diferentes) e
 * **duplicidade** (dois documentos para o mesmo par fragmentam a ocupação, que é o ganho principal do
 * pool sem dono).
 */
class RotaTest {

    private fun rota(
        id: String = "r1",
        origem: String = "porto-a",
        destino: String = "porto-b",
        ativo: Boolean = true,
    ) = Rota(id = id, portoOrigemId = origem, portoDestinoId = destino, ativo = ativo)

    // --- Sentido ---

    @Test
    fun `dois portos diferentes fazem uma travessia`() {
        assertTrue(rota().temSentido())
    }

    @Test
    fun `mesmo porto nos dois lados nao e travessia`() {
        assertFalse(rota(origem = "porto-a", destino = "porto-a").temSentido())
    }

    @Test
    fun `porto faltando nao e travessia`() {
        assertFalse(rota(origem = "", destino = "porto-b").temSentido())
        assertFalse(rota(origem = "porto-a", destino = "").temSentido())
    }

    /** **Ida e volta são rotas diferentes** — e é por isso que o par é ordenado. */
    @Test
    fun `o par preserva a ordem`() {
        assertEquals("porto-a" to "porto-b", rota().par)
        assertEquals("porto-b" to "porto-a", rota(origem = "porto-b", destino = "porto-a").par)
    }

    // --- Duplicidade no pool ---

    private val existentes = listOf(
        rota(id = "r1", origem = "porto-a", destino = "porto-b"),
        rota(id = "r2", origem = "porto-b", destino = "porto-a"),
    )

    @Test
    fun `acha a rota ativa do par, na ordem`() {
        assertEquals("r1", existentes.ativaComPar("porto-a", "porto-b")?.id)
        assertEquals("r2", existentes.ativaComPar("porto-b", "porto-a")?.id)
        assertNull(existentes.ativaComPar("porto-a", "porto-c"))
    }

    /**
     * Rota inativada com o mesmo par é **registro do passado**. Recusar por causa dela impediria de
     * recriar exatamente o que se acabou de corrigir — que é o gesto que a imutabilidade obriga.
     */
    @Test
    fun `rota inativa nao bloqueia a criacao do mesmo par`() {
        val comInativa = listOf(rota(id = "r1", ativo = false))

        assertNull(comInativa.ativaComPar("porto-a", "porto-b"))
    }
}