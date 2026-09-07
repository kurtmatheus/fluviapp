package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.domain.operacoes.Usuario
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A fronteira documento → perfil do contexto de **sistema** (`users/{uid}`, ADR-0015 §8.1).
 *
 * O lado de **negócio** saiu daqui na F6.2, junto com o Room: a Equipe passou a ler por
 * `DocumentoBruto.toFuncionario()`, e os casos dela vivem em `FuncionarioDocumentoTest`. A
 * **assimetria deliberada** que esta classe registrava continua valendo, agora repartida entre os dois
 * arquivos: o **papel** atravessa cru (desconhecido tem que virar "sem permissão"), enquanto o **cargo**
 * ausente cai no menor privilégio.
 *
 * Os casos mudaram de sujeito em 2026-09-07: eram sobre `UsuarioDocumento.toUsuario(id)`, o DTO que o
 * `toObject` do Firestore preenchia por reflexão. Com a leitura passando a `Map` (para que o R8 não
 * tenha o que quebrar), o sujeito é `DocumentoBruto.toPerfilAutenticado`.
 */
class PerfilDocumentosTest {

    private fun documento(vararg campos: Pair<String, Any?>) = DocumentoBruto("uid-1", mapOf(*campos))

    @Test
    fun `perfil sem vinculo nasce sem funcionarioId — e isso e valido`() {
        val doc = documento("email" to "adm@x.com", "username" to "adm", "papel" to "ADM")

        val perfil = doc.toPerfilAutenticado(uid = "uid-1", funcionario = null)

        assertEquals("uid-1", perfil.id)
        assertEquals("adm", perfil.username)
        assertTrue(perfil.funcionarioId.isEmpty())
        // Sem o segundo salto, cargo e nome ficam vazios — quem decide ali é o papel.
        assertTrue(perfil.cargo.isEmpty())
        assertTrue(perfil.nome.isEmpty())
    }

    @Test
    fun `papel desconhecido NAO ganha default — segue fail-closed`() {
        // Contraste com o cargo: papel fora do enum tem que virar "sem permissão" (ADR-0010), então a
        // fronteira o mantém como veio e quem nega é a política.
        val doc = documento("email" to "a@x.com", "username" to "ana", "papel" to "AGENTE")

        val perfil = doc.toPerfilAutenticado(uid = "uid-1", funcionario = null)

        assertEquals("AGENTE", perfil.papel)
        assertNull(Usuario.Papel.de(perfil.papel))
    }

    /** Campo ausente no documento vira vazio, e não exceção: um perfil torto não pode derrubar o login. */
    @Test
    fun `documento sem campo nenhum atravessa vazio`() {
        val perfil = documento().toPerfilAutenticado(uid = "uid-1", funcionario = null)

        assertEquals("uid-1", perfil.id)
        assertTrue(perfil.email.isEmpty())
        assertTrue(perfil.papel.isEmpty())
        assertNull(Usuario.Papel.de(perfil.papel))
    }

    /** O segundo salto traz **só** cargo e nome — o resto continua sendo do documento de sistema. */
    @Test
    fun `o funcionario ligado entra com cargo e nome`() {
        val doc = documento(
            "email" to "b@x.com",
            "username" to "bruno",
            "papel" to "OPERADOR",
            "funcionarioId" to "f-9",
        )
        val funcionario = DocumentoBruto("f-9", mapOf("nome" to "Bruno Lima", "cargo" to "AGENTE"))

        val perfil = doc.toPerfilAutenticado(uid = "uid-1", funcionario = funcionario)

        assertEquals("f-9", perfil.funcionarioId)
        assertEquals("AGENTE", perfil.cargo)
        assertEquals("Bruno Lima", perfil.nome)
    }

    /**
     * Domínio → `Map`: o `id` **não entra**, porque ele é o nome do documento (o `uid` do Auth). Duplicá-lo
     * criaria duas fontes para a mesma identidade.
     */
    @Test
    fun `o mapa gravado tem os quatro campos, e o id nao e um deles`() {
        val mapa = Usuario(
            id = "uid-1",
            email = "a@x.com",
            username = "ana",
            papel = "OPERADOR",
            funcionarioId = "f-1",
        ).paraMapa()

        assertEquals(setOf("email", "username", "papel", "funcionarioId"), mapa.keys)
        assertEquals("a@x.com", mapa["email"])
        assertEquals("f-1", mapa["funcionarioId"])
    }
}
