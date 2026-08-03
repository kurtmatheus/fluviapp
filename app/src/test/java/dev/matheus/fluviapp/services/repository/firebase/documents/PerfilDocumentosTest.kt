package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.domain.operacoes.Funcionario
import dev.matheus.fluviapp.domain.operacoes.Usuario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * As duas fronteiras documento → modelo dos contextos de sistema e de negócio (ADR-0015 §8.1), e a
 * **assimetria deliberada** entre elas: o papel atravessa cru (desconhecido tem que virar "sem
 * permissão"), o cargo ausente cai no menor privilégio.
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

    // --- Negócio: funcionarios/{id} ---

    @Test
    fun `funcionario sem cargo gravado vira AGENTE — o menor privilegio`() {
        // Firestore é schemaless: documento anterior ao campo simplesmente não o tem. Quem tem registro
        // de funcionário está na operação, então o vazio significa "o cargo mais baixo", não "sem cargo".
        val doc = FuncionarioDocumento(nome = "Ana", agencia = "MATRIZ", lotacao = "Porto Norte")

        val funcionario = doc.toFuncionario("f1")

        assertEquals(Funcionario.Cargo.AGENTE.name, funcionario.cargo)
        assertEquals("Ana", funcionario.descricaoNome)
    }

    @Test
    fun `cargo desconhecido atravessa cru — quem nega e a politica`() {
        val doc = FuncionarioDocumento(nome = "Ana", agencia = "MATRIZ", cargo = "GERENTE")

        val funcionario = doc.toFuncionario("f1")

        assertEquals("GERENTE", funcionario.cargo)
        assertNull(Funcionario.Cargo.de(funcionario.cargo))
    }

    @Test
    fun `agencia livre atravessa intacta — o enum nao valida o campo`() {
        // O seed tem agências fora do enum (ADR-0015 §8.1): nenhuma leitura pode colapsá-las em AUTONOMO.
        val doc = FuncionarioDocumento(nome = "Carla", agencia = "AGENCIA HORIZONTE")

        assertEquals("AGENCIA HORIZONTE", doc.toFuncionario("f3").agencia)
    }
}