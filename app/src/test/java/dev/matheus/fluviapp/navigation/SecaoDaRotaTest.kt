package dev.matheus.fluviapp.navigation

import dev.matheus.fluviapp.domain.operacoes.PermissoesUsuario
import dev.matheus.fluviapp.domain.operacoes.Usuario
import dev.matheus.fluviapp.domain.screendata.SecaoMenu
import dev.matheus.fluviapp.navigation.destinations.FluviAppNavComposableDestinations
import dev.matheus.fluviapp.navigation.destinations.TODOS_OS_DESTINOS
import dev.matheus.fluviapp.navigation.destinations.secaoDaRota
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A tabela que a **guarda de navegação** consulta ([ADR-0032] D1).
 *
 * A guarda em si é um `LaunchedEffect` sobre a pilha do Navigation — testá-la exigiria instrumentar. O
 * que **dá** para provar em JVM é o que ela decide: a rota vira seção, e a seção vira permissão. Se estas
 * duas peças estiverem certas, o que sobra da guarda é uma linha de `popBackStack`.
 */
class SecaoDaRotaTest {

    @Test
    fun `a rota declarada resolve na secao do destino`() {
        assertEquals(SecaoMenu.EMPRESA, secaoDaRota("formEmpresa"))
        assertEquals(SecaoMenu.EQUIPE, secaoDaRota("pesquisarFuncionario"))
        assertEquals(SecaoMenu.USUARIOS, secaoDaRota("formUsuario"))
        assertEquals(SecaoMenu.VIAGEM, secaoDaRota("pesquisarViagem"))
    }

    /**
     * Fora do menu é resposta, não lacuna: o bilhete, a emissão e o embarque são **gestos** alcançados de
     * dentro da Passagem, e quem os governa é o guarda do ViewModel.
     */
    @Test
    fun `destino que nao e secao resolve em null`() {
        assertNull(secaoDaRota("embarque"))
        assertNull(secaoDaRota("emissao/{ocorrencia}"))
        assertNull(secaoDaRota("bilhete/{idPassagem}"))
        assertNull(secaoDaRota("mainScreen"))
    }

    /** Rota desconhecida não vira seção — e a guarda deixa passar, porque não é dela a decisão. */
    @Test
    fun `rota estranha nao resolve em secao nenhuma`() {
        assertNull(secaoDaRota("rotaQueNaoExiste"))
        assertNull(secaoDaRota(null))
    }

    /**
     * **A lista e as declarações não podem discordar.** `TODOS_OS_DESTINOS` é escrita à mão porque Kotlin
     * não enumera classe selada sem reflexão — então o risco real é alguém declarar um destino e esquecer
     * de acrescentá-lo aqui.
     *
     * Este caso não elimina o risco (nada sem reflexão elimina), mas fecha a porta pela qual ele
     * apareceria: **rota duplicada**. Duas entradas com a mesma rota fariam `secaoDaRota` devolver a
     * primeira, e a segunda tela ficaria com a permissão da outra.
     */
    @Test
    fun `nenhuma rota aparece duas vezes na lista`() {
        val rotas = TODOS_OS_DESTINOS.map { it.route }

        assertEquals(rotas.size, rotas.toSet().size)
    }

    /** Toda seção do menu tem pelo menos um destino — senão a seção existe e não leva a lugar nenhum. */
    @Test
    fun `toda secao do menu tem destino`() {
        val comDestino = TODOS_OS_DESTINOS.mapNotNull { it.secao }.toSet()

        assertEquals(SecaoMenu.entries.toSet(), comDestino)
    }

    // --- O que a guarda decide, com a política no meio ---

    /**
     * O caso que a guarda existe para impedir: `USUARIOS` é `ADM`-only (ADR-0021 D1), e até 2026-09-07 o
     * operador que chegasse ao destino por outro caminho — pilha de retorno, `navigate` novo — abria a
     * tela, porque a única barreira era o botão ausente no menu.
     */
    @Test
    fun `operador nao acessa o destino de Usuarios`() {
        val secao = secaoDaRota(FluviAppNavComposableDestinations.FormUsuarioNavComposable.route)

        assertEquals(SecaoMenu.USUARIOS, secao)
        assertFalse(PermissoesUsuario.podeAcessar(secao!!, Usuario.Papel.OPERADOR.name))
        assertTrue(PermissoesUsuario.podeAcessar(secao, Usuario.Papel.ADM.name))
    }

    /** Sem sessão, papel é `null` e a política nega — o fail-closed vale também para a navegação. */
    @Test
    fun `sem papel, nenhum destino de secao e acessivel`() {
        val secoes = TODOS_OS_DESTINOS.mapNotNull { it.secao }.toSet()

        val acessiveis = secoes.filter { PermissoesUsuario.podeAcessar(it, papel = null) }

        // A Passagem é a exceção declarada da política (`podeAcessar` devolve `true` para ela), e o que a
        // barra não é o menu: é o guarda do gesto, que pergunta `podeCriarPassagem`.
        assertEquals(listOf(SecaoMenu.PASSAGEM), acessiveis)
    }
}
