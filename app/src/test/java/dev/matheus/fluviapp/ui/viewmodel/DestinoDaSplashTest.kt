package dev.matheus.fluviapp.ui.viewmodel

import dev.matheus.fluviapp.fakes.FakeSessaoUsuario
import dev.matheus.fluviapp.ui.states.SplashScreenState
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * A decisão da splash (ADR-0020 D9), pura. O que estes casos travam é que **nenhum caminho entra pela
 * metade** e **nenhum fica preso em `Carregando`** — que é a "informação omitida" que a decisão existe
 * para impedir.
 */
class DestinoDaSplashTest {

    private val contexto = requireNotNull(FakeSessaoUsuario.agente().contexto)

    @Test
    fun `sem sessao vai para o login, sem sequer tentar carregar`() {
        assertEquals(SplashScreenState.Deslogado, destinoDaSplash(temSessao = false))
    }

    @Test
    fun `com sessao e contexto carregado, entra`() {
        assertEquals(
            SplashScreenState.Logado,
            destinoDaSplash(temSessao = true, contexto = contexto),
        )
    }

    @Test
    fun `falha ao carregar vira Erro — nao entra e nao fica carregando para sempre`() {
        assertEquals(
            SplashScreenState.Erro,
            destinoDaSplash(temSessao = true, falhouAoCarregar = true),
        )
    }

    @Test
    fun `sessao valida sem registro local volta ao login — e nao entra sem contexto`() {
        // Mudança de comportamento consciente: antes bastava haver sessão no Firebase para entrar, e o
        // painel montava sem saber quem era a pessoa na operação. É no login que o vínculo se refaz.
        assertEquals(
            SplashScreenState.Deslogado,
            destinoDaSplash(temSessao = true, contexto = null),
        )
    }

    @Test
    fun `a falha tem precedencia sobre o contexto ausente — o motivo certo, nao o generico`() {
        assertEquals(
            SplashScreenState.Erro,
            destinoDaSplash(temSessao = true, contexto = null, falhouAoCarregar = true),
        )
    }

    @Test
    fun `nenhuma combinacao resolve para Carregando`() {
        val combinacoes = listOf(
            destinoDaSplash(temSessao = false),
            destinoDaSplash(temSessao = false, contexto = contexto),
            destinoDaSplash(temSessao = true, contexto = contexto),
            destinoDaSplash(temSessao = true, contexto = null),
            destinoDaSplash(temSessao = true, falhouAoCarregar = true),
        )

        combinacoes.forEach { assertEquals(false, it == SplashScreenState.Carregando) }
    }
}