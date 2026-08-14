package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.domain.cliente.Cliente
import dev.matheus.fluviapp.domain.documento.TipoDocumento
import dev.matheus.fluviapp.domain.passagem.ClasseVeiculo
import dev.matheus.fluviapp.domain.veiculo.Veiculo
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * A fronteira dos **dois pools** (F9.3, [ADR-0018] D2/D3/D5).
 *
 * O que estes casos protegem, além do direito de recusa que todo codec tem, é a **canonicidade da chave**:
 * o pool inteiro se apoia na ideia de que a mesma credencial produz o mesmo id, e é isso que faz *criar-ou-
 * assinar* encontrar quem existe sem poder lê-lo. Um `529.982.247-25` que não normalize para `52998224725`
 * cria uma segunda pessoa em silêncio — e duplicata de pool é o defeito que nenhuma tela mostra.
 */
class PoolDocumentosTest {

    private val ana = Cliente(
        id = "CPF:52998224725",
        nome = "Ana Ribeiro",
        tipoDocumento = TipoDocumento.CPF,
        numeroDocumento = "52998224725",
        dataNascimento = LocalDate.of(1996, 1, 30),
        telefone = "(91) 98888-0000",
        agenciaIds = setOf("empresa-1"),
    )

    private val moto = Veiculo(
        id = "ABC1D23",
        placa = "ABC1D23",
        tipo = ClasseVeiculo.MOTO,
        modelo = "Fan 150",
        cor = "Vermelho",
        cilindrada = 150,
        agenciaIds = setOf("empresa-1"),
    )

    private fun documento(id: String, dados: Map<String, Any?>) = DocumentoBruto(id, dados)

    // --- Cliente: ida e volta ---

    @Test
    fun `cliente atravessa e volta igual`() {
        assertEquals(ana, documento(ana.id, ana.paraMapa()).toCliente())
    }

    @Test
    fun `cliente sem telefone atravessa e volta sem telefone`() {
        val semTelefone = ana.copy(telefone = null)

        assertEquals(semTelefone, documento(semTelefone.id, semTelefone.paraMapa()).toCliente())
    }

    /** O documento vai **canônico** para o Firestore: é sobre ele que a chave natural se calcula. */
    @Test
    fun `o documento e gravado sem pontuacao`() {
        val comPontuacao = ana.copy(numeroDocumento = "529.982.247-25")

        assertEquals("52998224725", comPontuacao.paraMapa()["numeroDocumento"])
        assertEquals(ana.chaveNatural, comPontuacao.chaveNatural)
    }

    @Test
    fun `a data de nascimento e gravada em ISO`() {
        assertEquals("1996-01-30", ana.paraMapa()["dataNascimento"])
    }

    @Test
    fun `as assinaturas viram lista, porque o Firestore nao tem conjunto`() {
        assertEquals(listOf("empresa-1"), ana.paraMapa()["agenciaIds"])
    }

    // --- Cliente: as recusas ---

    @Test
    fun `cliente sem tipo de documento nao vira cliente`() {
        assertNull(documento(ana.id, ana.paraMapa() + ("tipoDocumento" to "")).toCliente())
    }

    @Test
    fun `tipo de documento desconhecido nao vira CPF por omissao`() {
        assertNull(documento(ana.id, ana.paraMapa() + ("tipoDocumento" to "TITULO_ELEITOR")).toCliente())
    }

    /** *"Não existe criança sem documento nesse negócio"* — entrada sem credencial não se reaproveita. */
    @Test
    fun `cliente sem numero de documento nao vira cliente`() {
        assertNull(documento(ana.id, ana.paraMapa() + ("numeroDocumento" to "")).toCliente())
    }

    /** É o nascimento que decide a gratuidade de criança: em branco, a regra erraria em silêncio. */
    @Test
    fun `cliente com nascimento ilegivel nao vira cliente`() {
        assertNull(documento(ana.id, ana.paraMapa() + ("dataNascimento" to "30/01/1996")).toCliente())
    }

    /** Nome ruim é cadastro malfeito, que alguém corrige — não é referência quebrada. */
    @Test
    fun `cliente sem nome continua sendo cliente`() {
        val lido = documento(ana.id, ana.paraMapa() + ("nome" to "")).toCliente()

        assertEquals("", lido?.nome)
        assertEquals(ana.chaveNatural, lido?.chaveNatural)
    }

    @Test
    fun `cliente sem assinatura nenhuma e lido com o conjunto vazio`() {
        val lido = documento(ana.id, ana.paraMapa() - "agenciaIds").toCliente()

        assertTrue(lido!!.agenciaIds.isEmpty())
        assertTrue(!lido.assinadoPor("empresa-1"))
    }

    // --- Veículo: ida e volta ---

    @Test
    fun `veiculo atravessa e volta igual`() {
        assertEquals(moto, documento(moto.id, moto.paraMapa()).toVeiculo())
    }

    @Test
    fun `veiculo cujo tipo ja e o modelo atravessa sem modelo`() {
        val carreta = Veiculo(
            id = "XYZ9A88",
            placa = "XYZ9A88",
            tipo = ClasseVeiculo.CARRETA,
            cor = "Branco",
            agenciaIds = setOf("empresa-1"),
        )

        assertEquals(carreta, documento(carreta.id, carreta.paraMapa()).toVeiculo())
    }

    /** A placa é chave, e chave que admite duas grafias não é chave. */
    @Test
    fun `a placa e gravada canonica - sem separador e em caixa alta`() {
        assertEquals("ABC1D23", moto.copy(placa = "abc-1d23").paraMapa()["placa"])
        assertEquals("ABC1D23", placaCanonica(" abc 1d23 "))
    }

    // --- Veículo: as recusas, e o que NÃO se recusa ---

    @Test
    fun `veiculo sem placa nao vira veiculo`() {
        assertNull(documento(moto.id, moto.paraMapa() + ("placa" to "")).toVeiculo())
    }

    @Test
    fun `classe desconhecida nao vira carro por omissao`() {
        assertNull(documento(moto.id, moto.paraMapa() + ("tipo" to "TRICICLO")).toVeiculo())
    }

    /**
     * Entrada **incompleta** não é entrada ilegível: quem responde por ela é `pendencias()`, que diz qual
     * campo falta. Recusar aqui esconderia do operador um veículo que ele consegue completar.
     */
    @Test
    fun `moto sem cilindrada e lida, e a pendencia e que fala`() {
        val lida = documento(moto.id, moto.paraMapa() - "cilindrada").toVeiculo()

        assertEquals(setOf(Veiculo.Pendencia.CILINDRADA), lida!!.pendencias())
    }

    @Test
    fun `cilindrada zero e lida como ausente, e nao como zero`() {
        val lida = documento(moto.id, moto.paraMapa() + ("cilindrada" to 0)).toVeiculo()

        assertNull(lida!!.cilindrada)
    }
}
