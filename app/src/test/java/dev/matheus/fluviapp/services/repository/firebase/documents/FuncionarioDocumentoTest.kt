package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.domain.operacoes.Funcionario
import dev.matheus.fluviapp.domain.operacoes.Funcionario.Cargo
import dev.matheus.fluviapp.domain.operacoes.Vinculo
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A fronteira de dados da Equipe depois que ela saiu do Room (F6.2, ADR-0019 D2).
 *
 * O que estes casos protegem é a convivência entre a forma nova e a velha: **o vínculo entra sem que os
 * campos legados saiam**, porque quem os lê (a Passagem) ainda não foi revitalizado. Um mapeamento que
 * "limpasse" o legado agora quebraria a emissão sem ninguém pedir.
 */
class FuncionarioDocumentoTest {

    private val ana = Funcionario(
        id = "func-1",
        descricaoNome = "Ana Ribeiro",
        cargo = Cargo.SUPERVISOR.name,
        email = "ana@fluviapp.com.br",
        vinculo = Vinculo("empresa-1", Cargo.SUPERVISOR),
    )

    private fun documento(id: String = "func-1", dados: Map<String, Any?>) = DocumentoBruto(id, dados)

    // --- Leitura ---

    @Test
    fun `toFuncionario le os campos novos e os legados`() {
        assertEquals(ana, documento(dados = ana.paraMapa()).toFuncionario())
    }

    @Test
    fun `toFuncionario tira o id do documento`() {
        assertEquals("outro", documento(id = "outro", dados = ana.paraMapa()).toFuncionario().id)
    }

    /** Documento sem `vinculo`: continua sendo uma pessoa — é o pré-cadastro do §2.1. */
    @Test
    fun `documento sem vinculo vira funcionario sem vinculo`() {
        val lido = documento(dados = mapOf("nome" to "Bruno", "agencia" to "MATRIZ")).toFuncionario()

        assertEquals("Bruno", lido.descricaoNome)
        assertNull(lido.vinculo)
    }

    /**
     * **O array antigo não é lido como vínculo.** O campo mudou de nome e de forma na mesma escrita
     * (`vinculos: []` → `vinculo: {}`), então um documento da forma anterior atravessa como pessoa sem
     * vínculo em vez de meio-lido.
     *
     * É a régua da casa (portfólio, sem produção): **regenera-se pelo seed, não se faz backfill** — e o
     * que o caso trava é que a leitura da forma velha não invente um vínculo nem estoure.
     */
    @Test
    fun `array antigo nao vira vinculo`() {
        val lido = documento(
            dados = mapOf(
                "nome" to "Ana",
                "vinculos" to listOf(mapOf("empresaId" to "empresa-1", "cargo" to "SUPERVISOR")),
            )
        ).toFuncionario()

        assertEquals("Ana", lido.descricaoNome)
        assertNull(lido.vinculo)
    }

    /**
     * **Vínculo ilegível some; a pessoa fica.** É a assimetria proposital em relação ao Porto: lá, a
     * referência quebrada invalida o documento inteiro (porto sem lugar não é porto); aqui, funcionário
     * sem vínculo é estado legítimo — é o pré-cadastro do §2.1 —, e perder o nome de quem existe por
     * causa de um vínculo corrompido seria o pior dos dois erros.
     */
    @Test
    fun `vinculo com cargo desconhecido e descartado sem levar a pessoa junto`() {
        val comCargoIlegivel = documento(
            dados = mapOf(
                "nome" to "Ana",
                "vinculo" to mapOf("empresaId" to "empresa-1", "cargo" to "CHEFAO"),
            )
        ).toFuncionario()
        val semEmpresa = documento(
            dados = mapOf("nome" to "Ana", "vinculo" to mapOf("empresaId" to "", "cargo" to "AGENTE"))
        ).toFuncionario()

        assertEquals("Ana", comCargoIlegivel.descricaoNome)
        assertNull(comCargoIlegivel.vinculo)
        assertNull(semEmpresa.vinculo)
    }

    /** Cargo ausente vira AGENTE — o menor privilégio, não "sem cargo" (quem tem registro está na operação). */
    @Test
    fun `cargo legado ausente vira AGENTE`() {
        assertEquals(Cargo.AGENTE.name, documento(dados = mapOf("nome" to "X")).toFuncionario().cargo)
    }

    /**
     * A **assimetria** que o `PerfilDocumentosTest` guardava do outro lado: cargo *ausente* ganha o menor
     * privilégio, cargo *desconhecido* atravessa cru — quem nega é a política (fail-closed, ADR-0010).
     */
    @Test
    fun `cargo legado desconhecido atravessa cru`() {
        val lido = documento(dados = mapOf("nome" to "Ana", "cargo" to "GERENTE")).toFuncionario()

        assertEquals("GERENTE", lido.cargo)
        assertEquals(null, Cargo.de(lido.cargo))
    }

    /**
     * O documento antigo continua legível **inteiro** — a `agencia` gravada por versões anteriores
     * simplesmente deixa de ser lida (F6.5), e nada no caminho estoura por causa dela. É a contrapartida
     * de um Firestore schemaless: campo que ninguém lê não precisa ser removido do banco.
     */
    @Test
    fun `campo legado que ninguem le mais nao atrapalha a leitura`() {
        val lido = documento(
            dados = mapOf("nome" to "Carla", "agencia" to "AGENCIA HORIZONTE", "lotacao" to "ILHA")
        ).toFuncionario()

        assertEquals("Carla", lido.descricaoNome)
    }

    // --- Escrita ---

    @Test
    fun `paraMapa nao grava o id e grava o vinculo como par empresa-cargo`() {
        val mapa = ana.paraMapa()

        assertFalse(mapa.containsKey("id"))
        assertEquals(mapOf("empresaId" to "empresa-1", "cargo" to "SUPERVISOR"), mapa["vinculo"])
        // A atuação NÃO é gravada: ela é derivada do cargo (§6.1), e um campo ao lado poderia contradizê-lo.
        assertFalse((mapa["vinculo"] as Map<*, *>).containsKey("atuacao"))
    }

    /**
     * **O derivado não existe mais.** `empresaIds` era denormalização deliberada — o Firestore não
     * consulta campo de dentro de elemento de array —, e com o vínculo em mapa `vinculo.empresaId` é
     * caminho de campo comum. O caso trava a ausência: um derivado que voltasse a ser gravado voltaria a
     * poder divergir da origem.
     */
    @Test
    fun `paraMapa nao grava derivado de empresa`() {
        assertFalse(ana.paraMapa().containsKey("empresaIds"))
        assertFalse(ana.paraMapa().containsKey("empresaId"))
    }

    /** Sem vínculo, a chave vai a `null` **explícito** — é o que permite tirar o vínculo de quem tinha. */
    @Test
    fun `paraMapa sem vinculo grava a chave nula, e nao a omite`() {
        val mapa = ana.copy(vinculo = null).paraMapa()

        assertTrue(mapa.containsKey("vinculo"))
        assertNull(mapa["vinculo"])
    }

    @Test
    fun `gravar e ler de volta devolve o mesmo funcionario`() {
        assertEquals(ana, documento(id = ana.id, dados = ana.paraMapa()).toFuncionario())
    }
}