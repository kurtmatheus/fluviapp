package dev.matheus.fluviapp.domain.cliente

import dev.matheus.fluviapp.domain.documento.TipoDocumento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * O `Cliente` como entidade de pool (ADR-0018 D2/D3, ADR-0023 D5).
 *
 * O que se cobra aqui é a **chave natural**: ela é o documento apresentado, e é ela que decide se a entrada já
 * existe antes de criar outra. Duas consequências que o ADR aceitou de propósito e que os casos abaixo fixam: a
 * mesma pessoa com CPF numa agência e RG noutra vira **duas entradas**, e o **telefone não entra na chave** —
 * dois telefones não fazem duas pessoas.
 *
 * **De volta ao escopo na F9.6**: o portador acendeu (ver `AcomodacaoTest`).
 */
class ClienteTest {

    private fun cliente(
        tipo: TipoDocumento = TipoDocumento.CPF,
        numero: String = "52998224725",
        telefone: String? = null,
        agencias: Set<String> = emptySet(),
    ) = Cliente(
        nome = "Maria da Silva",
        tipoDocumento = tipo,
        numeroDocumento = numero,
        dataNascimento = LocalDate.of(1990, 5, 12),
        telefone = telefone,
        agenciaIds = agencias,
    )

    @Test
    fun `chave natural e o tipo mais o numero do documento`() {
        assertEquals("CPF:52998224725", cliente().chaveNatural)
    }

    /** A grafia do documento não deve criar duas pessoas: a chave normaliza a pontuação. */
    @Test
    fun `pontuacao do documento nao muda a chave`() {
        assertEquals(cliente(numero = "52998224725").chaveNatural, cliente(numero = "529.982.247-25").chaveNatural)
    }

    /** Duplicidade aceita por decisão (D2): a chave é a credencial, não a pessoa. */
    @Test
    fun `mesma pessoa com documentos diferentes tem chaves diferentes`() {
        val porCpf = cliente(tipo = TipoDocumento.CPF, numero = "52998224725")
        val porRg = cliente(tipo = TipoDocumento.RG, numero = "1234567")

        assertNotEquals(porCpf.chaveNatural, porRg.chaveNatural)
    }

    @Test
    fun `telefone nao participa da chave`() {
        assertEquals(
            cliente(telefone = null).chaveNatural,
            cliente(telefone = "91999990000").chaveNatural,
        )
    }

    /** É o que decide entre *criar* e *assinar* (ADR-0018 D3). */
    @Test
    fun `assinadoPor diz se a agencia ja atendeu`() {
        val atendido = cliente(agencias = setOf("emp_1", "emp_2"))

        assertTrue(atendido.assinadoPor("emp_1"))
        assertFalse(atendido.assinadoPor("emp_9"))
        assertFalse(cliente().assinadoPor("emp_1"))
    }
}
