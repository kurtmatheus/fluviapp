package dev.matheus.fluviapp.telemetry

import dev.matheus.fluviapp.domain.operacoes.ContextoUsuario
import dev.matheus.fluviapp.domain.operacoes.Funcionario
import dev.matheus.fluviapp.domain.operacoes.Usuario
import dev.matheus.fluviapp.domain.operacoes.Vinculo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * As coordenadas de um evento ([ADR-0032] D4) — **agregáveis, e nenhuma identidade**.
 *
 * A régua da decisão em uma frase: *o evento conta, o documento prova*. O evento responde *"quantas
 * emissões por agência nesta semana"*; *"o que a Ana fez às 14h"* é do carimbo dentro do documento, e é
 * lá que a auditoria mora.
 */
class CoordenadasDaSessaoTest {

    private val ana = Usuario(
        id = "uid-da-ana",
        email = "ana@empresa.com",
        username = "ana",
        papel = Usuario.Papel.OPERADOR.name,
        funcionarioId = "func-da-ana",
    )

    private val funcionaria = Funcionario(
        id = "func-da-ana",
        descricaoNome = "Ana Ribeiro",
        email = "ana@empresa.com",
        cargo = Funcionario.Cargo.AGENTE.name,
        vinculo = Vinculo("empresa-1", Funcionario.Cargo.AGENTE),
    )

    private val contexto = ContextoUsuario(usuario = ana, funcionario = funcionaria)

    @Test
    fun `as tres coordenadas sao papel, cargo e agencia`() {
        val coordenadas = coordenadasDe(contexto)

        assertEquals(Usuario.Papel.OPERADOR.name, coordenadas[PARAM_PAPEL])
        assertEquals(Funcionario.Cargo.AGENTE.name, coordenadas[PARAM_CARGO])
        assertEquals("empresa-1", coordenadas[PARAM_AGENCIA])
    }

    /**
     * **O caso que define a decisão.** Se um dia alguém acrescentar o uid "só para depurar", é aqui que
     * a mudança aparece — e a conversa volta a ser sobre PII, que é onde ela deve acontecer.
     */
    @Test
    fun `nenhuma coordenada identifica a pessoa`() {
        val coordenadas = coordenadasDe(contexto)
        val valores = coordenadas.values.toSet()

        assertTrue("o uid vazou", ana.id !in valores)
        assertTrue("o funcionarioId vazou", ana.funcionarioId !in valores)
        assertTrue("o e-mail vazou", ana.email !in valores)
        assertTrue("o nome vazou", funcionaria.descricaoNome !in valores)
        // E as chaves são as três declaradas — nada entra sem passar por este teste.
        assertEquals(setOf(PARAM_PAPEL, PARAM_CARGO, PARAM_AGENCIA), coordenadas.keys)
    }

    /** Papel puro de plataforma não atua em empresa nenhuma: sobra o papel, e é a informação correta. */
    @Test
    fun `papel de plataforma entra sem cargo e sem agencia`() {
        val adm = ContextoUsuario(
            usuario = Usuario(id = "uid-adm", email = "adm@x.com", username = "adm", papel = "ADM"),
            funcionario = null,
        )

        assertEquals(mapOf(PARAM_PAPEL to "ADM"), coordenadasDe(adm))
    }

    /** Sem sessão, um evento não tem de onde vir — e mapa vazio é a resposta honesta. */
    @Test
    fun `sem contexto nao ha coordenada nenhuma`() {
        assertTrue(coordenadasDe(null).isEmpty())
    }
}
