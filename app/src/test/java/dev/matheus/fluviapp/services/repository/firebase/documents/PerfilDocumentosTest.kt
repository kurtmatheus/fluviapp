package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.domain.operacoes.Funcionario
import dev.matheus.fluviapp.domain.operacoes.Usuario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A fronteira documento → modelo do contexto de **sistema** (`users/{uid}`, ADR-0015 §8.1).
 *
 * O lado de **negócio** saiu daqui na F6.2, junto com o Room: a Equipe passou a ler por
 * `DocumentoBruto.toFuncionario()`, e os casos dela vivem em `FuncionarioDocumentoTest`. A
 * **assimetria deliberada** que esta classe registrava continua valendo, agora repartida entre os dois
 * arquivos: o **papel** atravessa cru (desconhecido tem que virar "sem permissão"), enquanto o **cargo**
 * ausente cai no menor privilégio.
 */
class PerfilDocumentosTest {

    // --- Sistema: users/{uid} ---

    @Test
    fun `perfil sem vinculo nasce sem funcionarioId — e isso e valido`() {
        val doc = UsuarioDocumento(email = "adm@x.com", username = "adm", papel = "ADM")

        val usuario = doc.toUsuario("uid-1")

        assertEquals("uid-1", usuario.id)
        assertEquals("adm", usuario.username)
        assertTrue(usuario.funcionarioId.isEmpty())
    }

    @Test
    fun `papel desconhecido NAO ganha default — segue fail-closed`() {
        // Contraste com o cargo (abaixo): papel fora do enum tem que virar "sem permissão" (ADR-0010),
        // então a fronteira o mantém como veio e quem nega é a política.
        val doc = UsuarioDocumento(email = "a@x.com", username = "ana", papel = "AGENTE")

        val usuario = doc.toUsuario("uid-1")

        assertEquals("AGENTE", usuario.papel)
        assertNull(Usuario.Papel.de(usuario.papel))
    }

}