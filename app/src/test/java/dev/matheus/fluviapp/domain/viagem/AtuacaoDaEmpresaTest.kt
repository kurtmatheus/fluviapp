package dev.matheus.fluviapp.domain.viagem

import dev.matheus.fluviapp.domain.operacoes.Atuacao
import dev.matheus.fluviapp.domain.rota.Rota
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

/**
 * O par `(parte, atuação)` e a **concessão** (ADR-0016 §4/§7). O que estes casos travam é o fail-closed:
 * o que não foi concedido não se vende, e frota nova nasce não-concedida.
 */
class AtuacaoDaEmpresaTest {

    private val agenciamento = AtuacaoDaEmpresa(
        atuacao = Atuacao.AGENCIAMENTO,
        embarcacaoIds = setOf("embarcacao-1", "embarcacao-2"),
        portoIds = setOf("porto-manaus", "porto-parintins"),
    )

    private val manausParintins = Rota(
        id = "r1",
        portoOrigemId = "porto-manaus",
        portoDestinoId = "porto-parintins",
    )

    private fun viagem(embarcacaoId: String, rotaId: String = "r1") = Viagem(
        id = "v1",
        rotaId = rotaId,
        embarcacaoId = embarcacaoId,
        diaSemana = DayOfWeek.TUESDAY,
        horaMin = 18 * 60,
    )
    private val transporte = AtuacaoDaEmpresa(atuacao = Atuacao.TRANSPORTE)

    // --- concessão ---

    @Test
    fun `embarcacao concedido pode ser vendido`() {
        assertTrue(agenciamento.concedeu("embarcacao-1"))
        assertTrue(agenciamento.concedeu("embarcacao-2"))
    }

    @Test
    fun `embarcacao nao concedido nao pode — frota nova nasce fora`() {
        assertFalse(agenciamento.concedeu("embarcacao-3"))
    }

    @Test
    fun `id ausente e negado, e nao tratado como sem filtro`() {
        assertFalse(agenciamento.concedeu(null))
        assertFalse(agenciamento.concedeu(""))
        assertFalse(agenciamento.concedeu("   "))
    }

    @Test
    fun `atuacao sem concessao nao concede nada`() {
        assertFalse(transporte.concedeu("embarcacao-1"))
    }

    // --- a segunda dimensão: ONDE (F7, §7.1) ---

    @Test
    fun `porto concedido pode ser operado, e o de fora nao`() {
        assertTrue(agenciamento.operaNoPorto("porto-manaus"))
        assertFalse(agenciamento.operaNoPorto("porto-santarem"))
    }

    @Test
    fun `porto ausente e negado — porto novo nasce fora, como a frota`() {
        assertFalse(agenciamento.operaNoPorto(null))
        assertFalse(agenciamento.operaNoPorto(""))
        assertFalse(transporte.operaNoPorto("porto-manaus"))
    }

    /**
     * O ponto da F7: a linha ofertável **não é concedida**, é *deduzida* dos dois portos. É o que permite
     * a rota viver num pool compartilhado — quem a criou não importa; quem tem as duas pontas, vende.
     */
    @Test
    fun `oferta a travessia quem tem os DOIS portos`() {
        assertTrue(agenciamento.podeOfertar("porto-manaus", "porto-parintins"))
    }

    @Test
    fun `uma ponta so nao basta — nos dois sentidos`() {
        assertFalse(agenciamento.podeOfertar("porto-manaus", "porto-santarem"))
        assertFalse(agenciamento.podeOfertar("porto-santarem", "porto-manaus"))
    }

    // --- a pergunta completa: a VIAGEM (F8.1) ---

    /**
     * A F7 deixou a checagem pela metade de propósito: a rota sabe o *onde* e não sabe o *em quê*. É a
     * viagem que junta os dois eixos, porque é ela que tem a embarcação.
     */
    @Test
    fun `oferta a viagem quem tem os dois portos E a embarcacao`() {
        assertTrue(agenciamento.podeOfertar(viagem(embarcacaoId = "embarcacao-1"), manausParintins))
    }

    @Test
    fun `embarcacao alheia nao é ofertavel, mesmo em rota concedida`() {
        assertFalse(agenciamento.podeOfertar(viagem(embarcacaoId = "embarcacao-alheia"), manausParintins))
    }

    @Test
    fun `rota fora da concessao nao é ofertavel, mesmo com embarcacao concedida`() {
        val santaremManaus = Rota(
            id = "r2",
            portoOrigemId = "porto-santarem",
            portoDestinoId = "porto-manaus",
        )

        assertFalse(agenciamento.podeOfertar(viagem(embarcacaoId = "embarcacao-1"), santaremManaus))
    }

    // --- o conjunto de atuações da parte ---

    @Test
    fun `uma parte exerce varias atuacoes ao mesmo tempo`() {
        val atuacoes = listOf(agenciamento, transporte)

        assertTrue(atuacoes.exerce(Atuacao.AGENCIAMENTO))
        assertTrue(atuacoes.exerce(Atuacao.TRANSPORTE))
        assertFalse(atuacoes.exerce(Atuacao.PORTUARIA_OPERACAO))
    }

    @Test
    fun `de devolve a atuacao pedida, ou null quando a parte nao a exerce`() {
        val atuacoes = listOf(agenciamento, transporte)

        assertEquals(agenciamento, atuacoes.de(Atuacao.AGENCIAMENTO))
        assertNull(atuacoes.de(Atuacao.PORTUARIA_ARRENDAMENTO))
    }

    @Test
    fun `parte sem atuacao nenhuma nao exerce nada`() {
        val nenhuma = emptyList<AtuacaoDaEmpresa>()

        assertFalse(nenhuma.exerce(Atuacao.AGENCIAMENTO))
        assertNull(nenhuma.de(Atuacao.AGENCIAMENTO))
    }

    @Test
    fun `as dormentes ficam fora das operantes`() {
        val atuacoes = listOf(
            agenciamento,
            transporte,
            AtuacaoDaEmpresa(Atuacao.PORTUARIA_OPERACAO),
        )

        assertEquals(listOf(agenciamento, transporte), atuacoes.operantes())
    }
}