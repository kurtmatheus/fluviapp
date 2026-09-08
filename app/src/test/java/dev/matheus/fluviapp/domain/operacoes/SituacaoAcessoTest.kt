package dev.matheus.fluviapp.domain.operacoes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * O estado do acesso ([ADR-0032] D6/Q1), puro — e o **espelho em Kotlin** da regra do servidor, no mesmo
 * regime que `PermissoesUsuario` espelha `firestore.rules` (ADR-0011).
 *
 * A fronteira que vale é a de lá; esta existe para o app **falhar antes e falhar explicando** (D1), que é
 * o que o *permission denied* do Firestore não faz.
 */
class SituacaoAcessoTest {

    private val agora = 1_800_000_000_000L
    private val ontem = agora - 86_400_000L
    private val amanha = agora + 86_400_000L

    @Test
    fun `ativo sem prazo e o caso normal`() {
        assertEquals(SituacaoAcesso.ATIVO, situacaoDoAcesso(ativo = true, expiraEm = null, agora = agora))
    }

    @Test
    fun `ativo com prazo no futuro continua ativo`() {
        assertEquals(SituacaoAcesso.ATIVO, situacaoDoAcesso(ativo = true, expiraEm = amanha, agora = agora))
    }

    @Test
    fun `prazo vencido expira`() {
        assertEquals(SituacaoAcesso.EXPIRADO, situacaoDoAcesso(ativo = true, expiraEm = ontem, agora = agora))
    }

    /** O limite é exclusivo — a mesma comparação que a regra faz com `request.time`. */
    @Test
    fun `no instante do limite ja expirou`() {
        assertEquals(SituacaoAcesso.EXPIRADO, situacaoDoAcesso(ativo = true, expiraEm = agora, agora = agora))
    }

    @Test
    fun `desligado e desativado, com prazo ou sem`() {
        assertEquals(SituacaoAcesso.DESATIVADO, situacaoDoAcesso(ativo = false, expiraEm = null, agora = agora))
        assertEquals(SituacaoAcesso.DESATIVADO, situacaoDoAcesso(ativo = false, expiraEm = amanha, agora = agora))
    }

    /**
     * **O gesto de alguém vence o relógio na hora de explicar.** Quem foi desativado *e* tem prazo vencido
     * é desativado, porque é essa a informação acionável: reativar resolve, esperar não.
     */
    @Test
    fun `desativado e vencido e desativado, nao expirado`() {
        assertEquals(SituacaoAcesso.DESATIVADO, situacaoDoAcesso(ativo = false, expiraEm = ontem, agora = agora))
    }

    @Test
    fun `so o ativo e vigente`() {
        assertTrue(SituacaoAcesso.ATIVO.vigente)
        assertFalse(SituacaoAcesso.DESATIVADO.vigente)
        assertFalse(SituacaoAcesso.EXPIRADO.vigente)
    }
}
