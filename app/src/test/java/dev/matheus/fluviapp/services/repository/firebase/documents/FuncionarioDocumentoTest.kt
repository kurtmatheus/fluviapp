package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.domain.operacoes.Funcionario
import dev.matheus.fluviapp.domain.operacoes.Funcionario.Cargo
import dev.matheus.fluviapp.domain.operacoes.Vinculo
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A fronteira de dados da Equipe depois que ela saiu do Room (F6.2, ADR-0019 D2).
 *
 * O que estes casos protegem é a convivência entre a forma nova e a velha: **vínculos entram sem que os
 * campos legados saiam**, porque quem os lê (a Passagem) ainda não foi revitalizado. Um mapeamento que
 * "limpasse" o legado agora quebraria a emissão sem ninguém pedir.
 */
class FuncionarioDocumentoTest {

    private val ana = Funcionario(
        id = "func-1",
        descricaoNome = "Ana Ribeiro",
        agencia = "MATRIZ",
        lotacao = "PORTO_NORTE",
        cargo = Cargo.SUPERVISOR.name,
        email = "ana@fluviapp.com.br",
        vinculos = listOf(
            Vinculo("empresa-1", Cargo.SUPERVISOR),
            Vinculo("empresa-2", Cargo.AGENTE),
        ),
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

    /** Documento antigo, sem `vinculos`: continua sendo uma pessoa, com a lista vazia. */
    @Test
    fun `documento sem vinculos vira funcionario sem vinculo`() {
        val lido = documento(dados = mapOf("nome" to "Bruno", "agencia" to "MATRIZ")).toFuncionario()

        assertEquals("Bruno", lido.descricaoNome)
        assertTrue(lido.vinculos.isEmpty())
    }

    /**
     * **Vínculo ilegível some; a pessoa fica.** É a assimetria proposital em relação ao Porto: lá, a
     * referência quebrada invalida o documento inteiro (porto sem lugar não é porto); aqui, funcionário
     * sem vínculo é estado legítimo — é o pré-cadastro do §2.1 —, e perder o nome de quem existe por
     * causa de um vínculo corrompido seria o pior dos dois erros.
     */
    @Test
    fun `vinculo com cargo desconhecido e descartado sem levar a pessoa junto`() {
        val lido = documento(
            dados = mapOf(
                "nome" to "Ana",
                "vinculos" to listOf(
                    mapOf("empresaId" to "empresa-1", "cargo" to "CHEFAO"),
                    mapOf("empresaId" to "empresa-2", "cargo" to "AGENTE"),
                    mapOf("empresaId" to "", "cargo" to "AGENTE"),
                ),
            )
        ).toFuncionario()

        assertEquals("Ana", lido.descricaoNome)
        assertEquals(listOf(Vinculo("empresa-2", Cargo.AGENTE)), lido.vinculos)
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

    /** Agência é String livre desde o §8.1: nenhuma leitura pode colapsá-la num valor de enum. */
    @Test
    fun `agencia legada atravessa intacta`() {
        val lido = documento(dados = mapOf("nome" to "Carla", "agencia" to "AGENCIA HORIZONTE")).toFuncionario()

        assertEquals("AGENCIA HORIZONTE", lido.agencia)
    }

    // --- Escrita ---

    @Test
    fun `paraMapa nao grava o id e grava o vinculo como par empresa-cargo`() {
        val mapa = ana.paraMapa()

        assertFalse(mapa.containsKey("id"))
        assertEquals(
            listOf(
                mapOf("empresaId" to "empresa-1", "cargo" to "SUPERVISOR"),
                mapOf("empresaId" to "empresa-2", "cargo" to "AGENTE"),
            ),
            mapa["vinculos"],
        )
        // A atuação NÃO é gravada: ela é derivada do cargo (§6.1), e um campo ao lado poderia contradizê-lo.
        val primeiro = (mapa["vinculos"] as List<*>).first() as Map<*, *>
        assertFalse(primeiro.containsKey("atuacao"))
    }

    /**
     * `empresaIds` é **denormalização deliberada** — existe porque o Firestore não consulta campo de
     * dentro de elemento de array. O teste fixa o que impede o derivado de divergir: ele sai dos
     * vínculos, na mesma escrita, e não de um parâmetro que alguém possa preencher errado.
     */
    @Test
    fun `paraMapa deriva empresaIds dos vinculos`() {
        assertEquals(listOf("empresa-1", "empresa-2"), ana.paraMapa()["empresaIds"])
        assertEquals(emptyList<String>(), ana.copy(vinculos = emptyList()).paraMapa()["empresaIds"])
    }

    @Test
    fun `gravar e ler de volta devolve o mesmo funcionario`() {
        assertEquals(ana, documento(id = ana.id, dados = ana.paraMapa()).toFuncionario())
    }
}