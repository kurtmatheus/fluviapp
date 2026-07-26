package dev.matheus.fluviapp.model.operacoes

import dev.matheus.fluviapp.services.repository.firebase.documents.UsuarioDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.toUsuario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Agência como capacidade organizacional do usuário (ADR-0015 §2) e o contrato da **fronteira**:
 * `de` é estrito (String→enum, null se desconhecido) e `deOuPadrao` é o que a leitura de documento usa,
 * caindo em `AUTONOMO`. A assimetria com o cargo é proposital e está travada aqui.
 */
class AgenciaTest {

    @Test
    fun `de converte o valor canonico e recusa desconhecido`() {
        assertEquals(Agencia.MATRIZ, Agencia.de("MATRIZ"))
        assertEquals(Agencia.AUTONOMO, Agencia.de("AUTONOMO"))
        assertNull(Agencia.de("AGENCIA HORIZONTE"))
        assertNull(Agencia.de(""))
        assertNull(Agencia.de(null))
    }

    @Test
    fun `deOuPadrao cai em AUTONOMO quando ausente ou desconhecida`() {
        assertEquals(Agencia.AUTONOMO, Agencia.deOuPadrao(null))
        assertEquals(Agencia.AUTONOMO, Agencia.deOuPadrao(""))
        assertEquals(Agencia.AUTONOMO, Agencia.deOuPadrao("AGENCIA QUE NAO EXISTE"))
        assertEquals(Agencia.MATRIZ, Agencia.deOuPadrao("MATRIZ"))
    }

    // --- Fronteira documento → modelo ---

    @Test
    fun `perfil antigo (sem o campo) vira AUTONOMO, nao agencia vazia`() {
        // Firestore é schemaless: documento gravado antes do campo simplesmente não o tem.
        val doc = UsuarioDocumento(email = "a@x.com", nome = "Ana", cargo = "AGENTE")

        val usuario = doc.toUsuario("uid-1")

        assertEquals(Agencia.AUTONOMO.name, usuario.agencia)
        assertEquals("", usuario.lotacao)
    }

    @Test
    fun `perfil alocado preserva agencia e lotacao`() {
        val doc = UsuarioDocumento(
            email = "a@x.com",
            nome = "Ana",
            cargo = "AGENTE",
            agencia = "MATRIZ",
            lotacao = "Porto Norte",
        )

        val usuario = doc.toUsuario("uid-1")

        assertEquals("MATRIZ", usuario.agencia)
        assertEquals("Porto Norte", usuario.lotacao)
    }

    @Test
    fun `cargo desconhecido NAO ganha default — segue fail-closed`() {
        // Contraste deliberado com a agência: cargo fora do enum tem que virar "sem permissão"
        // (ADR-0010), então a fronteira do documento o mantém como veio, e a política nega.
        val doc = UsuarioDocumento(email = "a@x.com", nome = "Ana", cargo = "OPERADOR")

        val usuario = doc.toUsuario("uid-1")

        assertEquals("OPERADOR", usuario.cargo)
        assertNull(Usuario.Cargo.de(usuario.cargo))
    }
}