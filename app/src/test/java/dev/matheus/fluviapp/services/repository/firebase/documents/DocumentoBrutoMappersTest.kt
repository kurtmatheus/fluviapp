package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.domain.viagem.Empresa
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Trava a desserialização manual Map→domínio (§10 Nível 2 — substitui o `toObject` do Firestore).
 * É a área de risco (coerção de tipos): número chega como `Number`, campo ausente vira default.
 *
 * **Recortado à entidade viva** (ADR-0020): os casos de navio, funcionário, contador e viagem saíram —
 * a mecânica que eles exercitavam é a mesma, e voltam com as entidades deles, contra os mappers que a
 * revitalização tiver deixado. O que fica é a fronteira da **Empresa**, nos dois sentidos: `Map` →
 * domínio na leitura, domínio → `Map` na escrita (ADR-0019 D2).
 */
class DocumentoBrutoMappersTest {

    private val empresa = Empresa(
        id = "emp-1",
        nome = "NAVEGAÇÃO CENTRAL",
        razaoSocial = "NAVEGAÇÃO CENTRAL LTDA",
        cnpj = "38394453000112",
        endereco = "Av. Beira Rio, 100",
        telefone1 = "91999990000",
        telefone2 = "91888880000",
    )

    private fun documento(id: String = "emp-1", dados: Map<String, Any?>) = DocumentoBruto(id, dados)

    // --- Leitura: Map -> domínio ---

    @Test
    fun `toEmpresa le os seis campos do documento`() {
        assertEquals(empresa, documento(dados = empresa.paraMapa()).toEmpresa())
    }

    /**
     * A identidade vem do **nome do documento**, nunca do corpo. Se alguém gravar um campo `id`
     * divergente, é o do documento que vale — é ele que endereça a subcoleção de atuações.
     */
    @Test
    fun `toEmpresa tira o id do documento e ignora um campo id no corpo`() {
        val lida = documento(
            id = "o-verdadeiro",
            dados = empresa.paraMapa() + ("id" to "o-impostor"),
        ).toEmpresa()

        assertEquals("o-verdadeiro", lida.id)
    }

    /** Documento gravado antes de um campo existir: vira vazio, não quebra a leitura. */
    @Test
    fun `toEmpresa com campos ausentes devolve vazio em vez de falhar`() {
        val lida = documento(dados = mapOf("nome" to "SÓ O NOME")).toEmpresa()

        assertEquals("SÓ O NOME", lida.nome)
        assertEquals("", lida.razaoSocial)
        assertEquals("", lida.cnpj)
        assertEquals("", lida.endereco)
        assertEquals("", lida.telefone1)
        assertEquals("", lida.telefone2)
    }

    /**
     * Coerção defensiva: um CNPJ gravado como número (o Firestore devolveria `Long`) não vira texto por
     * acidente nem estoura — vira vazio, e o campo aparece em branco na tela. O `texto()` é fail-closed
     * de propósito: melhor o vazio visível que o valor inventado.
     */
    @Test
    fun `toEmpresa com tipo errado devolve vazio em vez de coagir`() {
        val lida = documento(dados = mapOf("nome" to 42L, "cnpj" to 38394453000112L)).toEmpresa()

        assertEquals("", lida.nome)
        assertEquals("", lida.cnpj)
    }

    // --- Escrita: domínio -> Map ---

    /** O id é o nome do documento, não um campo dele — duplicá-lo criaria duas fontes para a identidade. */
    @Test
    fun `paraMapa nao grava o id`() {
        assertFalse(empresa.paraMapa().containsKey("id"))
    }

    @Test
    fun `paraMapa grava exatamente os seis campos`() {
        assertEquals(
            setOf("nome", "razaoSocial", "cnpj", "endereco", "telefone1", "telefone2"),
            empresa.paraMapa().keys,
        )
    }

    /**
     * A propriedade que fecha a fronteira: gravar e ler de volta devolve a mesma empresa. Se alguém
     * acrescentar um campo ao domínio e esquecer de um dos dois lados, é aqui que quebra.
     */
    @Test
    fun `gravar e ler de volta devolve a mesma empresa`() {
        assertEquals(empresa, documento(id = empresa.id, dados = empresa.paraMapa()).toEmpresa())
    }
}