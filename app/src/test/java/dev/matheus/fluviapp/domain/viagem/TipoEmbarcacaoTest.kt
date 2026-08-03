package dev.matheus.fluviapp.domain.viagem

import dev.matheus.fluviapp.revitalizacao.ForaDoEscopo
import org.junit.experimental.categories.Category
import dev.matheus.fluviapp.domain.passagem.ClasseVeiculo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tipo da embarcação como tipo de domínio (ADR-0020 D4). A tabela do ADR-0016 §8, agora executável:
 * F/B leva tudo, navio leva carro e moto, lancha só passageiro.
 */
@Category(ForaDoEscopo::class)
class TipoEmbarcacaoTest {

    @Test
    fun `de converte o name canonico e recusa desconhecido`() {
        assertEquals(TipoEmbarcacao.FERRY_BOAT, TipoEmbarcacao.de("FERRY_BOAT"))
        assertEquals(TipoEmbarcacao.NAVIO, TipoEmbarcacao.de("navio"))
        assertEquals(TipoEmbarcacao.LANCHA, TipoEmbarcacao.de(" Lancha "))
        assertNull(TipoEmbarcacao.de(null))
        // O "Catamarã" do §8: cadastrado no catálogo, nascia inerte. Agora simplesmente não existe.
        assertNull(TipoEmbarcacao.de("CATAMARA"))
    }

    @Test
    fun `de tolera espaco no lugar do underscore`() {
        assertEquals(TipoEmbarcacao.FERRY_BOAT, TipoEmbarcacao.de("FERRY BOAT"))
    }

    @Test
    fun `a balsa leva todas as classes, inclusive as pesadas`() {
        ClasseVeiculo.entries.forEach { assertTrue(TipoEmbarcacao.FERRY_BOAT.admite(it)) }
    }

    @Test
    fun `o navio leva carro e moto, mas nao carga pesada`() {
        assertTrue(TipoEmbarcacao.NAVIO.admite(ClasseVeiculo.CARRO))
        assertTrue(TipoEmbarcacao.NAVIO.admite(ClasseVeiculo.MOTO))
        assertFalse(TipoEmbarcacao.NAVIO.admite(ClasseVeiculo.CAMINHAO))
        assertFalse(TipoEmbarcacao.NAVIO.admite(ClasseVeiculo.CARRETA))
    }

    @Test
    fun `nao se vende veiculo para uma lancha`() {
        ClasseVeiculo.entries.forEach { assertFalse(TipoEmbarcacao.LANCHA.admite(it)) }
        assertFalse(TipoEmbarcacao.LANCHA.levaVeiculo)
    }

    @Test
    fun `admite recusa classe nula`() {
        assertFalse(TipoEmbarcacao.FERRY_BOAT.admite(null))
    }

    @Test
    fun `levaVeiculo separa quem oferece o modo veiculo`() {
        assertTrue(TipoEmbarcacao.FERRY_BOAT.levaVeiculo)
        assertTrue(TipoEmbarcacao.NAVIO.levaVeiculo)
        assertFalse(TipoEmbarcacao.LANCHA.levaVeiculo)
    }
}
