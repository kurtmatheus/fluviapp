package dev.matheus.fluviapp.model.operacoes

import dev.matheus.fluviapp.model.operacoes.Funcionario.Cargo
import dev.matheus.fluviapp.model.operacoes.Usuario.Papel
import dev.matheus.fluviapp.model.screendata.SecaoMenu
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Matriz de autorização dos **dois eixos** (ADR-0010 + ADR-0015 §8.2). Política pura, JVM-testável.
 *
 * - SISTEMA: `papel` — `ADM`/`GESTOR` (plataforma) e `OPERADOR` (o coringa que corresponde a um
 *   funcionário).
 * - NEGÓCIO: `cargo` — `SUPERVISOR`/`AGENTE`.
 *
 * Ambos chegam como a String persistida (o `.name` do enum). **Cargo ausente é caso normal**: quem tem
 * papel de plataforma não tem registro de funcionário.
 */
class PermissoesUsuarioTest {

    private val adm = Papel.ADM.name
    private val gestor = Papel.GESTOR.name
    private val operador = Papel.OPERADOR.name

    private val supervisor = Cargo.SUPERVISOR.name
    private val agente = Cargo.AGENTE.name

    // --- Fronteira String -> enum, nos dois eixos ---

    @Test
    fun `Papel de converte os tres papeis canonicos`() {
        assertEquals(Papel.ADM, Papel.de("ADM"))
        assertEquals(Papel.GESTOR, Papel.de("GESTOR"))
        assertEquals(Papel.OPERADOR, Papel.de("OPERADOR"))
    }

    @Test
    fun `Cargo de converte os dois cargos canonicos`() {
        assertEquals(Cargo.SUPERVISOR, Cargo.de("SUPERVISOR"))
        assertEquals(Cargo.AGENTE, Cargo.de("AGENTE"))
    }

    @Test
    fun `de retorna null para desconhecido ou nulo nos dois eixos`() {
        assertEquals(null, Papel.de(null))
        assertEquals(null, Papel.de(""))
        assertEquals(null, Papel.de("GERENTE"))
        assertEquals(null, Cargo.de(null))
        assertEquals(null, Cargo.de(""))
        assertEquals(null, Cargo.de("GERENTE"))
    }

    /**
     * Os eixos **não se misturam no vocabulário**: cargo de negócio não resolve como papel de sistema e
     * vice-versa. Sem isto, um perfil gravado com `cargo: "AGENTE"` (vocabulário de antes da divisão)
     * passaria a ser lido como papel — e um `SUPERVISOR` viraria papel de sistema, que é justamente o
     * acoplamento que a revisão estrutural desfez.
     */
    @Test
    fun `papel nao aceita cargo de negocio, e cargo nao aceita papel de sistema`() {
        assertEquals(null, Papel.de("SUPERVISOR"))
        assertEquals(null, Papel.de("AGENTE"))
        assertEquals(null, Cargo.de("ADM"))
        assertEquals(null, Cargo.de("GESTOR"))
        assertEquals(null, Cargo.de("OPERADOR"))
    }

    /** Vocabulário anterior ao ADR-0015 §4.2 continua sem alias de compatibilidade. */
    @Test
    fun `vocabulario antigo nao resolve — fail-closed no rename`() {
        assertEquals(null, Papel.de("DIRETOR"))
        assertEquals(null, Papel.de("COLABORADOR_MASTER"))
        assertEquals(null, Cargo.de("DIRETOR"))
        assertEquals(null, Cargo.de("COLABORADOR_MASTER"))
    }

    // --- ehPapelPlataforma ---

    @Test
    fun `apenas ADM e GESTOR sao papeis de plataforma`() {
        assertTrue(PermissoesUsuario.ehPapelPlataforma(adm))
        assertTrue(PermissoesUsuario.ehPapelPlataforma(gestor))
        assertFalse(PermissoesUsuario.ehPapelPlataforma(operador))
        assertFalse(PermissoesUsuario.ehPapelPlataforma(null))
        assertFalse(PermissoesUsuario.ehPapelPlataforma("DESCONHECIDO"))
    }

    // --- Eixo seção (menu): puramente de sistema ---

    @Test
    fun `papel de plataforma ve todas as secoes`() {
        assertEquals(SecaoMenu.entries, PermissoesUsuario.secoesVisiveis(adm))
        assertEquals(SecaoMenu.entries, PermissoesUsuario.secoesVisiveis(gestor))
    }

    @Test
    fun `operador so ve Passagem — inclusive sendo supervisor na operacao`() {
        // O menu não pergunta o cargo: seção é eixo de sistema (§8.2). O supervisor manda dentro da
        // agência, não no cadastro de viagens/navios/empresas.
        assertEquals(listOf(SecaoMenu.PASSAGEM), PermissoesUsuario.secoesVisiveis(operador))
        assertFalse(PermissoesUsuario.podeAcessar(SecaoMenu.EQUIPE, operador))
    }

    @Test
    fun `papel desconhecido nao ve nenhuma secao operacional`() {
        assertEquals(listOf(SecaoMenu.PASSAGEM), PermissoesUsuario.secoesVisiveis(null))
        assertFalse(PermissoesUsuario.podeAcessar(SecaoMenu.VIAGEM, null))
        assertFalse(PermissoesUsuario.podeAcessar(SecaoMenu.EMPRESA, "X"))
        assertFalse(PermissoesUsuario.podeAcessar(SecaoMenu.NAVIO, operador))
    }

    @Test
    fun `Passagem e visivel para todos`() {
        listOf(adm, gestor, operador, null, "X").forEach {
            assertTrue(PermissoesUsuario.podeAcessar(SecaoMenu.PASSAGEM, it))
        }
    }

    // --- Eixo ação: criar ---

    @Test
    fun `os tres papeis conhecidos podem criar passagem`() {
        listOf(adm, gestor, operador).forEach {
            assertTrue("papel $it deveria poder criar", PermissoesUsuario.podeCriarPassagem(it))
        }
    }

    @Test
    fun `papel desconhecido nao pode criar passagem`() {
        assertFalse(PermissoesUsuario.podeCriarPassagem(null))
        assertFalse(PermissoesUsuario.podeCriarPassagem("GERENTE"))
    }

    // --- Eixo ação: editar qualquer / ver todas (é aqui que os dois eixos se encontram) ---

    @Test
    fun `plataforma edita qualquer passagem mesmo sem cargo de negocio`() {
        assertTrue(PermissoesUsuario.podeEditarQualquerPassagem(adm, cargo = null))
        assertTrue(PermissoesUsuario.podeEditarQualquerPassagem(gestor, cargo = null))
    }

    @Test
    fun `supervisor edita qualquer passagem pelo CARGO, com papel de operador`() {
        assertTrue(PermissoesUsuario.podeEditarQualquerPassagem(operador, supervisor))
    }

    @Test
    fun `agente nao edita qualquer passagem`() {
        assertFalse(PermissoesUsuario.podeEditarQualquerPassagem(operador, agente))
        assertFalse(PermissoesUsuario.podeEditarQualquerPassagem(operador, cargo = null))
        assertFalse(PermissoesUsuario.podeEditarQualquerPassagem(null, null))
    }

    @Test
    fun `ver todas na pesquisa acompanha o editar-qualquer`() {
        assertTrue(PermissoesUsuario.podeVerTodasPassagens(adm, null))
        assertTrue(PermissoesUsuario.podeVerTodasPassagens(operador, supervisor))
        assertFalse(PermissoesUsuario.podeVerTodasPassagens(operador, agente))
    }

    // --- Eixo ação: editar passagem específica (com posse) ---

    @Test
    fun `papel de plataforma edita passagem independentemente da posse`() {
        assertTrue(PermissoesUsuario.podeEditarPassagem(adm, null, ehDono = false))
        assertTrue(PermissoesUsuario.podeEditarPassagem(gestor, null, ehDono = false))
    }

    @Test
    fun `supervisor edita passagem de outros`() {
        assertTrue(PermissoesUsuario.podeEditarPassagem(operador, supervisor, ehDono = false))
        assertTrue(PermissoesUsuario.podeEditarPassagem(operador, supervisor, ehDono = true))
    }

    @Test
    fun `agente edita apenas a propria passagem`() {
        assertTrue(PermissoesUsuario.podeEditarPassagem(operador, agente, ehDono = true))
        // regressão do gate do detalhe: agente NÃO edita passagem de outro
        assertFalse(PermissoesUsuario.podeEditarPassagem(operador, agente, ehDono = false))
    }

    @Test
    fun `desconhecido nos dois eixos so edita se for dono`() {
        assertTrue(PermissoesUsuario.podeEditarPassagem(null, null, ehDono = true))
        assertFalse(PermissoesUsuario.podeEditarPassagem(null, null, ehDono = false))
        assertFalse(PermissoesUsuario.podeEditarPassagem("X", "Y", ehDono = false))
    }

    // --- Eixo ação: confirmar embarque (ADR-0012) ---

    @Test
    fun `qualquer papel conhecido pode confirmar embarque`() {
        listOf(adm, gestor, operador).forEach {
            assertTrue("papel $it deveria poder confirmar embarque", PermissoesUsuario.podeConfirmarEmbarque(it))
        }
    }

    @Test
    fun `papel desconhecido nao confirma embarque`() {
        assertFalse(PermissoesUsuario.podeConfirmarEmbarque(null))
        assertFalse(PermissoesUsuario.podeConfirmarEmbarque("GERENTE"))
    }

    // --- Deletar segue as mesmas regras de editar ---

    @Test
    fun `deletar segue exatamente o editar em toda a matriz`() {
        listOf(adm, gestor, operador, null, "X").forEach { papel ->
            listOf(supervisor, agente, null, "Y").forEach { cargo ->
                listOf(true, false).forEach { ehDono ->
                    assertEquals(
                        "deletar deve espelhar editar para papel=$papel cargo=$cargo dono=$ehDono",
                        PermissoesUsuario.podeEditarPassagem(papel, cargo, ehDono),
                        PermissoesUsuario.podeDeletarPassagem(papel, cargo, ehDono)
                    )
                }
            }
        }
    }
}