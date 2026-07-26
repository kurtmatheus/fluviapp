package dev.matheus.fluviapp.model.operacoes

import dev.matheus.fluviapp.model.operacoes.Usuario.Cargo
import dev.matheus.fluviapp.model.screendata.SecaoMenu
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Matriz de autorização por cargo (ADR-0010). Política pura, JVM-testável.
 *
 * Cargos de PLATAFORMA: ADM / GESTOR. Cargos de AGÊNCIA: SUPERVISOR / AGENTE (ADR-0015 §4.1/§4.2).
 * `cargo` chega como a String persistida (o `.name` do enum).
 */
class PermissoesUsuarioTest {

    private val adm = Cargo.ADM.name
    private val gestor = Cargo.GESTOR.name
    private val supervisor = Cargo.SUPERVISOR.name
    private val agente = Cargo.AGENTE.name

    // --- Cargo.de: fronteira String -> enum ---

    @Test
    fun `Cargo de converte os quatro cargos canonicos`() {
        assertEquals(Cargo.ADM, Cargo.de("ADM"))
        assertEquals(Cargo.GESTOR, Cargo.de("GESTOR"))
        assertEquals(Cargo.SUPERVISOR, Cargo.de("SUPERVISOR"))
        assertEquals(Cargo.AGENTE, Cargo.de("AGENTE"))
    }

    @Test
    fun `Cargo de retorna null para desconhecido ou nulo`() {
        assertEquals(null, Cargo.de(null))
        assertEquals(null, Cargo.de(""))
        assertEquals(null, Cargo.de("GERENTE"))
    }

    /**
     * O vocabulário antigo (ADR-0015 §4.2) **não** é aceito: nada de alias de compatibilidade. Um perfil
     * que ainda tenha o cargo antigo gravado cai em fail-closed (sem permissão) até ser atualizado — é o
     * custo consciente do rename, e este teste é o lock de que ninguém vai "resolver" isso com um alias.
     */
    @Test
    fun `cargos do vocabulario antigo nao resolvem — fail-closed no rename`() {
        assertEquals(null, Cargo.de("DIRETOR"))
        assertEquals(null, Cargo.de("COLABORADOR_MASTER"))
        assertEquals(null, Cargo.de("OPERADOR"))
    }

    // --- ehCargoPlataforma ---

    @Test
    fun `apenas ADM e GESTOR sao cargos de plataforma`() {
        assertTrue(PermissoesUsuario.ehCargoPlataforma(adm))
        assertTrue(PermissoesUsuario.ehCargoPlataforma(gestor))
        assertFalse(PermissoesUsuario.ehCargoPlataforma(supervisor))
        assertFalse(PermissoesUsuario.ehCargoPlataforma(agente))
        assertFalse(PermissoesUsuario.ehCargoPlataforma(null))
        assertFalse(PermissoesUsuario.ehCargoPlataforma("DESCONHECIDO"))
    }

    // --- Eixo seção (menu) ---

    @Test
    fun `cargo de plataforma ve todas as secoes`() {
        assertEquals(SecaoMenu.entries, PermissoesUsuario.secoesVisiveis(adm))
        assertEquals(SecaoMenu.entries, PermissoesUsuario.secoesVisiveis(gestor))
    }

    @Test
    fun `supervisor e agente so veem Passagem`() {
        assertEquals(listOf(SecaoMenu.PASSAGEM), PermissoesUsuario.secoesVisiveis(supervisor))
        assertEquals(listOf(SecaoMenu.PASSAGEM), PermissoesUsuario.secoesVisiveis(agente))
    }

    @Test
    fun `cargo desconhecido nao ve nenhuma secao operacional`() {
        assertEquals(listOf(SecaoMenu.PASSAGEM), PermissoesUsuario.secoesVisiveis(null))
        assertFalse(PermissoesUsuario.podeAcessar(SecaoMenu.VIAGEM, null))
        assertFalse(PermissoesUsuario.podeAcessar(SecaoMenu.EQUIPE, agente))
        assertFalse(PermissoesUsuario.podeAcessar(SecaoMenu.EMPRESA, supervisor))
        assertFalse(PermissoesUsuario.podeAcessar(SecaoMenu.NAVIO, agente))
    }

    @Test
    fun `Passagem e visivel para todos`() {
        listOf(adm, gestor, supervisor, agente, null, "X").forEach {
            assertTrue(PermissoesUsuario.podeAcessar(SecaoMenu.PASSAGEM, it))
        }
    }

    // --- Eixo ação: criar ---

    @Test
    fun `os quatro cargos conhecidos podem criar passagem`() {
        listOf(adm, gestor, supervisor, agente).forEach {
            assertTrue("cargo $it deveria poder criar", PermissoesUsuario.podeCriarPassagem(it))
        }
    }

    @Test
    fun `cargo desconhecido nao pode criar passagem`() {
        assertFalse(PermissoesUsuario.podeCriarPassagem(null))
        assertFalse(PermissoesUsuario.podeCriarPassagem("GERENTE"))
    }

    // --- Eixo ação: editar qualquer / ver todas ---

    @Test
    fun `plataforma e supervisor editam qualquer passagem`() {
        assertTrue(PermissoesUsuario.podeEditarQualquerPassagem(adm))
        assertTrue(PermissoesUsuario.podeEditarQualquerPassagem(gestor))
        assertTrue(PermissoesUsuario.podeEditarQualquerPassagem(supervisor))
    }

    @Test
    fun `agente nao edita qualquer passagem`() {
        assertFalse(PermissoesUsuario.podeEditarQualquerPassagem(agente))
        assertFalse(PermissoesUsuario.podeEditarQualquerPassagem(null))
    }

    @Test
    fun `ver todas na pesquisa acompanha o editar-qualquer`() {
        assertTrue(PermissoesUsuario.podeVerTodasPassagens(adm))
        assertTrue(PermissoesUsuario.podeVerTodasPassagens(gestor))
        assertTrue(PermissoesUsuario.podeVerTodasPassagens(supervisor))
        assertFalse(PermissoesUsuario.podeVerTodasPassagens(agente))
    }

    // --- Eixo ação: editar passagem específica (com posse) ---

    @Test
    fun `cargo de plataforma edita passagem independentemente da posse`() {
        assertTrue(PermissoesUsuario.podeEditarPassagem(adm, ehDono = false))
        assertTrue(PermissoesUsuario.podeEditarPassagem(gestor, ehDono = false))
    }

    @Test
    fun `supervisor edita passagem de outros`() {
        assertTrue(PermissoesUsuario.podeEditarPassagem(supervisor, ehDono = false))
        assertTrue(PermissoesUsuario.podeEditarPassagem(supervisor, ehDono = true))
    }

    @Test
    fun `agente edita apenas a propria passagem`() {
        assertTrue(PermissoesUsuario.podeEditarPassagem(agente, ehDono = true))
        // regressão do gate do detalhe: agente NÃO edita passagem de outro
        assertFalse(PermissoesUsuario.podeEditarPassagem(agente, ehDono = false))
    }

    @Test
    fun `cargo desconhecido so edite se for dono`() {
        assertTrue(PermissoesUsuario.podeEditarPassagem(null, ehDono = true))
        assertFalse(PermissoesUsuario.podeEditarPassagem(null, ehDono = false))
    }

    // --- Eixo ação: confirmar embarque (ADR-0012) ---

    @Test
    fun `qualquer cargo conhecido pode confirmar embarque`() {
        listOf(adm, gestor, supervisor, agente).forEach {
            assertTrue("cargo $it deveria poder confirmar embarque", PermissoesUsuario.podeConfirmarEmbarque(it))
        }
    }

    @Test
    fun `cargo desconhecido nao confirma embarque`() {
        assertFalse(PermissoesUsuario.podeConfirmarEmbarque(null))
        assertFalse(PermissoesUsuario.podeConfirmarEmbarque("GERENTE"))
    }

    // --- Deletar segue as mesmas regras de editar ---

    @Test
    fun `deletar segue exatamente o editar para todos os cargos e posses`() {
        listOf(adm, gestor, supervisor, agente, null, "X").forEach { cargo ->
            listOf(true, false).forEach { ehDono ->
                assertEquals(
                    "deletar deve espelhar editar para cargo=$cargo dono=$ehDono",
                    PermissoesUsuario.podeEditarPassagem(cargo, ehDono),
                    PermissoesUsuario.podeDeletarPassagem(cargo, ehDono)
                )
            }
        }
    }
}