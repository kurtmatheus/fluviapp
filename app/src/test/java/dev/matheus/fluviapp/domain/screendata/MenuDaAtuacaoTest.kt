package dev.matheus.fluviapp.domain.screendata

import dev.matheus.fluviapp.domain.operacoes.Atuacao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A família do menu derivada da atuação (ADR-0016 §2, ADR-0020 F3) — a função pura que substitui a
 * enumeração à mão. É o que torna a plataforma multi-segmento sem tocar o modelo de permissão.
 */
class MenuDaAtuacaoTest {

    @Test
    fun `o agenciamento vende passagem`() {
        assertTrue(SecaoMenu.PASSAGEM in secoesDa(Atuacao.AGENCIAMENTO))
    }

    /**
     * **Nada atravessa mais os dois lados** (F6.6). A `EQUIPE` era a única, e o efeito em tela era o
     * `ADM` abrindo o quadro de pessoal de uma empresa. O que desfez o nó foi o convite: a plataforma
     * cria *quem entra*, e a empresa se gere a partir daí — cada painel com o seu.
     */
    @Test
    fun `equipe e das atuacoes, e nao do painel — nada atravessa`() {
        Atuacao.operantes().forEach { atuacao ->
            assertTrue(
                "Equipe deveria estar em $atuacao",
                SecaoMenu.EQUIPE in secoesDa(atuacao),
            )
        }
        assertTrue(SecaoMenu.EQUIPE !in secoesDoPainel())
        assertTrue(SecaoMenu.USUARIOS in secoesDoPainel())
        assertEquals(emptySet<SecaoMenu>(), SECOES_TRANSVERSAIS)
    }

    @Test
    fun `atuacao dormente nao ganha painel sozinha (fail-closed)`() {
        assertEquals(emptySet<SecaoMenu>(), secoesDa(Atuacao.PORTUARIA_OPERACAO))
        assertEquals(emptySet<SecaoMenu>(), secoesDa(Atuacao.PORTUARIA_ARRENDAMENTO))
    }

    /** O transporte ainda não vende nada — mas tem quadro próprio, como qualquer operação (F6.6). */
    @Test
    fun `o transporte tem a Equipe, e nada mais — a frota nao existe como cadastro`() {
        assertEquals(setOf(SecaoMenu.EQUIPE), secoesDa(Atuacao.TRANSPORTE))
    }

    @Test
    fun `o painel administra as partes e os ativos, nao a operacao`() {
        val painel = secoesDoPainel()
        assertTrue(SecaoMenu.EMPRESA in painel)
        assertTrue(SecaoMenu.EMBARCACAO in painel)
        // PASSAGEM é operação: quem administra a plataforma não emite (ADR-0016 §2).
        assertTrue(SecaoMenu.PASSAGEM !in painel)
    }

    @Test
    fun `nenhuma secao fica orfa — toda seção pertence ao painel ou a alguma atuacao`() {
        val cobertas = secoesDoPainel() + Atuacao.entries.flatMap { secoesDa(it) }
        assertEquals(SecaoMenu.entries.toSet(), cobertas)
    }
}