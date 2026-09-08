package dev.matheus.fluviapp.domain.operacoes

import dev.matheus.fluviapp.domain.operacoes.Funcionario.Cargo
import dev.matheus.fluviapp.domain.operacoes.Usuario.Papel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **A troca de perfil, como regra pura** ([ADR-0032] D5): a mesma coisa que a seleção de contexto (F6.4)
 * era, no eixo novo — e por isso os casos aqui são os herdeiros diretos dos que saíram do `VinculoTest`.
 *
 * O que eles fixam é o que faz da preferência uma preferência: **ela só é consultada de quem tem os dois
 * perfis**, e deixa de casar sozinha quando um deles desaparece.
 */
class PerfilTest {

    private val adm = Papel.ADM.name
    private val gestor = Papel.GESTOR.name
    private val operador = Papel.OPERADOR.name

    private val vinculo = Vinculo("empresa-1", Cargo.SUPERVISOR)

    // --- Fronteira ---

    @Test
    fun `de converte os dois perfis, e recusa o resto`() {
        assertEquals(Perfil.PLATAFORMA, Perfil.de("PLATAFORMA"))
        assertEquals(Perfil.EMPRESA, Perfil.de("EMPRESA"))
        assertNull(Perfil.de("AGENCIA"))
        assertNull(Perfil.de(null))
        assertNull(Perfil.de(""))
    }

    /** Perfil não se confunde com papel: os vocabulários são de eixos diferentes. */
    @Test
    fun `perfil nao aceita papel nem cargo`() {
        assertNull(Perfil.de("ADM"))
        assertNull(Perfil.de("OPERADOR"))
        assertNull(Perfil.de("SUPERVISOR"))
    }

    // --- Quem pode trocar ---

    /**
     * **As duas condições, e cada uma sozinha não basta.** O papel é a decisão (a troca é lente, e
     * prometê-la a um `OPERADOR` seria mentir sobre segurança); o vínculo é o fato (sem ele não há para
     * onde ir).
     */
    @Test
    fun `troca de perfil e de quem tem papel de plataforma E vinculo`() {
        assertTrue(PermissoesUsuario.podeTrocarPerfil(adm, vinculo))
        assertTrue(PermissoesUsuario.podeTrocarPerfil(gestor, vinculo))

        assertFalse(PermissoesUsuario.podeTrocarPerfil(adm, null))
        assertFalse(PermissoesUsuario.podeTrocarPerfil(operador, vinculo))
        assertFalse(PermissoesUsuario.podeTrocarPerfil(null, vinculo))
    }

    // --- Qual perfil vale (a regra inteira, sem DataStore e sem tela) ---

    /** Quem opera não tem perfil de plataforma para ativar — nem com uma escolha guardada dizendo o contrário. */
    @Test
    fun `quem opera esta sempre no perfil de empresa`() {
        assertEquals(Perfil.EMPRESA, resolverPerfilAtivo(operador, vinculo, escolha = null))
        assertEquals(Perfil.EMPRESA, resolverPerfilAtivo(operador, vinculo, Perfil.PLATAFORMA))
        // Sem vínculo também: é o pré-cadastro, e o painel dele não é o da plataforma.
        assertEquals(Perfil.EMPRESA, resolverPerfilAtivo(operador, vinculo = null, escolha = null))
    }

    /** Papel desconhecido cai no mesmo lado — o fail-closed é não conceder o perfil que administra. */
    @Test
    fun `papel desconhecido nao ativa o perfil de plataforma`() {
        assertEquals(Perfil.EMPRESA, resolverPerfilAtivo("GERENTE", vinculo, Perfil.PLATAFORMA))
        assertEquals(Perfil.EMPRESA, resolverPerfilAtivo(null, null, Perfil.PLATAFORMA))
    }

    @Test
    fun `sem vinculo, quem administra fica na plataforma`() {
        assertEquals(Perfil.PLATAFORMA, resolverPerfilAtivo(adm, vinculo = null, escolha = null))
        assertEquals(Perfil.PLATAFORMA, resolverPerfilAtivo(adm, vinculo = null, escolha = Perfil.EMPRESA))
    }

    @Test
    fun `com os dois perfis, vale a escolha — e o padrao e administrar`() {
        assertEquals(Perfil.PLATAFORMA, resolverPerfilAtivo(adm, vinculo, escolha = null))
        assertEquals(Perfil.EMPRESA, resolverPerfilAtivo(adm, vinculo, Perfil.EMPRESA))
        assertEquals(Perfil.PLATAFORMA, resolverPerfilAtivo(adm, vinculo, Perfil.PLATAFORMA))
    }

    /**
     * **A escolha vencida** — o mesmo cuidado que a seleção entre empresas tinha, no eixo novo: alguém
     * guarda `EMPRESA` no aparelho, perde o vínculo, e não pode continuar operando em nome de uma empresa
     * que não é mais dele. A preferência simplesmente deixa de casar, e não há nada a invalidar.
     */
    @Test
    fun `escolha de empresa sem vinculo nao vale`() {
        assertEquals(Perfil.PLATAFORMA, resolverPerfilAtivo(adm, vinculo = null, escolha = Perfil.EMPRESA))
    }
}
