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
 * Cargos: ADM / DIRETOR (gestor), COLABORADOR_MASTER, OPERADOR.
 * `cargo` chega como a String persistida (o `.name` do enum).
 */
class PermissoesUsuarioTest {

    private val adm = Cargo.ADM.name
    private val diretor = Cargo.DIRETOR.name
    private val colab = Cargo.COLABORADOR_MASTER.name
    private val operador = Cargo.OPERADOR.name

    // --- Cargo.de: fronteira String -> enum ---

    @Test
    fun `Cargo de converte os quatro cargos canonicos`() {
        assertEquals(Cargo.ADM, Cargo.de("ADM"))
        assertEquals(Cargo.DIRETOR, Cargo.de("DIRETOR"))
        assertEquals(Cargo.COLABORADOR_MASTER, Cargo.de("COLABORADOR_MASTER"))
        assertEquals(Cargo.OPERADOR, Cargo.de("OPERADOR"))
    }

    @Test
    fun `Cargo de retorna null para desconhecido ou nulo`() {
        assertEquals(null, Cargo.de(null))
        assertEquals(null, Cargo.de(""))
        assertEquals(null, Cargo.de("GERENTE"))
        // regressão: o valor formatado (com espaço) NÃO é um cargo válido
        assertEquals(null, Cargo.de("COLABORADOR MASTER"))
    }

    // --- ehGestor ---

    @Test
    fun `apenas ADM e DIRETOR sao gestores`() {
        assertTrue(PermissoesUsuario.ehGestor(adm))
        assertTrue(PermissoesUsuario.ehGestor(diretor))
        assertFalse(PermissoesUsuario.ehGestor(colab))
        assertFalse(PermissoesUsuario.ehGestor(operador))
        assertFalse(PermissoesUsuario.ehGestor(null))
        assertFalse(PermissoesUsuario.ehGestor("DESCONHECIDO"))
    }

    // --- Eixo seção (menu) ---

    @Test
    fun `gestor ve todas as secoes`() {
        assertEquals(SecaoMenu.entries, PermissoesUsuario.secoesVisiveis(adm))
        assertEquals(SecaoMenu.entries, PermissoesUsuario.secoesVisiveis(diretor))
    }

    @Test
    fun `colaborador master e operador so veem Passagem`() {
        assertEquals(listOf(SecaoMenu.PASSAGEM), PermissoesUsuario.secoesVisiveis(colab))
        assertEquals(listOf(SecaoMenu.PASSAGEM), PermissoesUsuario.secoesVisiveis(operador))
    }

    @Test
    fun `cargo desconhecido nao ve nenhuma secao operacional`() {
        assertEquals(listOf(SecaoMenu.PASSAGEM), PermissoesUsuario.secoesVisiveis(null))
        assertFalse(PermissoesUsuario.podeAcessar(SecaoMenu.VIAGEM, null))
        assertFalse(PermissoesUsuario.podeAcessar(SecaoMenu.AGENTE, operador))
        assertFalse(PermissoesUsuario.podeAcessar(SecaoMenu.EMPRESA, colab))
        assertFalse(PermissoesUsuario.podeAcessar(SecaoMenu.NAVIO, operador))
    }

    @Test
    fun `Passagem e visivel para todos`() {
        listOf(adm, diretor, colab, operador, null, "X").forEach {
            assertTrue(PermissoesUsuario.podeAcessar(SecaoMenu.PASSAGEM, it))
        }
    }

    // --- Eixo ação: criar ---

    @Test
    fun `os quatro cargos conhecidos podem criar passagem`() {
        listOf(adm, diretor, colab, operador).forEach {
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
    fun `gestor e colaborador master editam qualquer passagem`() {
        assertTrue(PermissoesUsuario.podeEditarQualquerPassagem(adm))
        assertTrue(PermissoesUsuario.podeEditarQualquerPassagem(diretor))
        assertTrue(PermissoesUsuario.podeEditarQualquerPassagem(colab))
    }

    @Test
    fun `operador nao edita qualquer passagem`() {
        assertFalse(PermissoesUsuario.podeEditarQualquerPassagem(operador))
        assertFalse(PermissoesUsuario.podeEditarQualquerPassagem(null))
    }

    @Test
    fun `ver todas na pesquisa acompanha o editar-qualquer`() {
        assertTrue(PermissoesUsuario.podeVerTodasPassagens(adm))
        assertTrue(PermissoesUsuario.podeVerTodasPassagens(diretor))
        assertTrue(PermissoesUsuario.podeVerTodasPassagens(colab))
        assertFalse(PermissoesUsuario.podeVerTodasPassagens(operador))
    }

    // --- Eixo ação: editar passagem específica (com posse) ---

    @Test
    fun `gestor edita passagem independentemente da posse`() {
        assertTrue(PermissoesUsuario.podeEditarPassagem(adm, ehDono = false))
        assertTrue(PermissoesUsuario.podeEditarPassagem(diretor, ehDono = false))
    }

    @Test
    fun `colaborador master edita passagem de outros`() {
        assertTrue(PermissoesUsuario.podeEditarPassagem(colab, ehDono = false))
        assertTrue(PermissoesUsuario.podeEditarPassagem(colab, ehDono = true))
    }

    @Test
    fun `operador edita apenas a propria passagem`() {
        assertTrue(PermissoesUsuario.podeEditarPassagem(operador, ehDono = true))
        // regressão do gate do detalhe: operador NÃO edita passagem de outro
        assertFalse(PermissoesUsuario.podeEditarPassagem(operador, ehDono = false))
    }

    @Test
    fun `cargo desconhecido so edite se for dono`() {
        assertTrue(PermissoesUsuario.podeEditarPassagem(null, ehDono = true))
        assertFalse(PermissoesUsuario.podeEditarPassagem(null, ehDono = false))
    }

    // --- Deletar segue as mesmas regras de editar ---

    @Test
    fun `deletar segue exatamente o editar para todos os cargos e posses`() {
        listOf(adm, diretor, colab, operador, null, "X").forEach { cargo ->
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