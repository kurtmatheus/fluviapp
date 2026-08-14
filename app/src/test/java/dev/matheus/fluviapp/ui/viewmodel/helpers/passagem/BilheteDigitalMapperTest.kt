package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem

import dev.matheus.fluviapp.domain.cliente.Cliente
import dev.matheus.fluviapp.domain.documento.TipoDocumento
import dev.matheus.fluviapp.domain.passagem.Acomodacao
import dev.matheus.fluviapp.domain.passagem.ClasseVeiculo
import dev.matheus.fluviapp.domain.passagem.FormaPagamento
import dev.matheus.fluviapp.domain.passagem.Lancamento
import dev.matheus.fluviapp.domain.passagem.MetadadosPassagem
import dev.matheus.fluviapp.domain.passagem.PassagemDePassageiro
import dev.matheus.fluviapp.domain.passagem.PassagemDeVeiculo
import dev.matheus.fluviapp.domain.passagem.StatusPassagem
import dev.matheus.fluviapp.domain.passagem.TipoGratuidade
import dev.matheus.fluviapp.domain.passagem.TipoPassagem
import dev.matheus.fluviapp.domain.veiculo.Veiculo
import dev.matheus.fluviapp.domain.viagem.OcorrenciaViagem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

/**
 * **O bilhete** — a segunda projeção da Passagem, e a que se distingue da primeira pelo destinatário.
 *
 * O caso que mais importa aqui é o do **documento completo**: ele é o oposto do que a conferência faz, e a
 * diferença é decisão ([ADR-0030] D1), não descuido. Um teste que aceitasse os dois formatos deixaria a
 * decisão livre para se perder na próxima refatoração.
 */
class BilheteDigitalMapperTest {

    private val ocorrencia = OcorrenciaViagem(viagemId = "viagem-1", data = LocalDate.of(2026, 8, 18))

    private val metadados = MetadadosPassagem(
        status = StatusPassagem.EMITIDA,
        funcionarioId = "func-1",
        agenciaId = "empresa-1",
        criadoEm = "2026-08-13T09:00:00",
        alteradoEm = "2026-08-13T09:00:00",
    )

    private val ana = Cliente(
        id = "CPF:52998224725",
        nome = "Ana Ribeiro",
        tipoDocumento = TipoDocumento.CPF,
        numeroDocumento = "52998224725",
        dataNascimento = LocalDate.of(1996, 1, 30),
    )

    private val bruno = Cliente(
        id = "CPF:11144477735",
        nome = "Bruno Costa",
        tipoDocumento = TipoDocumento.CPF,
        numeroDocumento = "11144477735",
        dataNascimento = LocalDate.of(1980, 5, 10),
    )

    private fun passageiro(
        clientes: List<String> = listOf(ana.id),
        acomodacao: Acomodacao = Acomodacao.REDE,
        tipo: TipoPassagem = TipoPassagem.INTEIRA,
        gratuidade: TipoGratuidade? = null,
    ) = PassagemDePassageiro(
        id = "pas-1",
        numero = "41",
        ocorrencia = ocorrencia,
        lancamentos = listOf(Lancamento("l1", FormaPagamento.PIX, BigDecimal("150.00"))),
        metadados = metadados,
        acomodacao = acomodacao,
        tipo = tipo,
        gratuidade = gratuidade,
        clientes = clientes,
    )

    private fun referencias(vararg clientes: Cliente) = ReferenciasDaPassagem(
        clientesPorId = clientes.associateBy { it.id },
        viagem = null,
        rota = null,
    )

    // --- O documento, que aqui é completo ---

    /**
     * **O oposto da conferência, de propósito** ([ADR-0030] D1): mascarar protege de quem está por perto, e o
     * bilhete vai para a mão de quem já sabe o próprio número.
     */
    @Test
    fun `o bilhete mostra o documento por inteiro`() {
        val bilhete = passageiro().paraBilhete(referencias(ana), agencia = "NAVEG")

        assertEquals("CPF 529.982.247-25", bilhete.passageiros.single().documento)
    }

    @Test
    fun `o QR carrega o id da passagem, que e o que o validador le`() {
        val bilhete = passageiro().paraBilhete(referencias(ana), agencia = "NAVEG")

        assertEquals("pas-1", bilhete.idPassagem)
        assertEquals("#41", bilhete.numero)
    }

    // --- Quem viaja ---

    @Test
    fun `um passageiro so nao ganha papel de titular`() {
        val bilhete = passageiro().paraBilhete(referencias(ana), agencia = "NAVEG")

        assertEquals("Passageiro", bilhete.passageiros.single().papel)
    }

    @Test
    fun `com acompanhante, o primeiro vira titular`() {
        val comDois = passageiro(clientes = listOf(ana.id, bruno.id), acomodacao = Acomodacao.SUITE)

        val bilhete = comDois.paraBilhete(referencias(ana, bruno), agencia = "NAVEG")

        assertEquals(listOf("Titular", "Acompanhante"), bilhete.passageiros.map { it.papel })
        assertEquals("Suíte · 2 pessoas", bilhete.bilhete)
    }

    /**
     * Pessoa que o pool não devolveu vira **linha sem nome**, e não linha ausente: um bilhete de suíte para
     * três com dois nomes seria lido como bilhete de dois.
     */
    @Test
    fun `pessoa que o pool nao entregou aparece sem nome, e nao some`() {
        val comDois = passageiro(clientes = listOf(ana.id, bruno.id), acomodacao = Acomodacao.SUITE)

        val bilhete = comDois.paraBilhete(referencias(ana), agencia = "NAVEG")

        assertEquals(2, bilhete.passageiros.size)
        assertEquals("", bilhete.passageiros[1].nome)
        assertEquals("", bilhete.passageiros[1].documento)
    }

    // --- O que se fiscaliza ---

    @Test
    fun `gratuidade aparece por extenso`() {
        val gratuita = passageiro(tipo = TipoPassagem.GRATUIDADE, gratuidade = TipoGratuidade.IDOSO)

        val bilhete = gratuita.paraBilhete(referencias(ana), agencia = "NAVEG")

        assertEquals("Idoso", bilhete.gratuidade)
        assertTrue(bilhete.bilhete.contains("Gratuidade"))
    }

    @Test
    fun `passagem inteira nao anuncia gratuidade nenhuma`() {
        assertNull(passageiro().paraBilhete(referencias(ana), agencia = "NAVEG").gratuidade)
    }

    /** O total é a **soma dos lançamentos** — não há campo de total no agregado (ADR-0024 D4). */
    @Test
    fun `o total vem dos lancamentos`() {
        val bilhete = passageiro().paraBilhete(referencias(ana), agencia = "NAVEG")

        assertTrue(bilhete.total.contains("150"))
    }

    // --- Veículo ---

    @Test
    fun `bilhete de veiculo traz placa e classe, e dispensa passageiro`() {
        val moto = Veiculo(id = "ABC1D23", placa = "ABC1D23", tipo = ClasseVeiculo.MOTO, cilindrada = 150)
        val bilheteDeVeiculo = PassagemDeVeiculo(
            id = "pas-2",
            numero = "42",
            ocorrencia = ocorrencia,
            lancamentos = emptyList(),
            metadados = metadados,
            veiculoId = moto.id,
        )

        val bilhete = bilheteDeVeiculo.paraBilhete(
            ReferenciasDaPassagem(veiculosPorId = mapOf(moto.id to moto)),
            agencia = "NAVEG",
        )

        assertEquals("ABC1D23", bilhete.veiculo?.placa)
        assertEquals("150 cc", bilhete.veiculo?.cilindrada)
        assertTrue(bilhete.passageiros.isEmpty())
    }

    @Test
    fun `responsavel pela retirada aparece nomeado`() {
        val moto = Veiculo(id = "ABC1D23", placa = "ABC1D23", tipo = ClasseVeiculo.MOTO, cilindrada = 150)
        val comResponsavel = PassagemDeVeiculo(
            id = "pas-2",
            numero = "42",
            ocorrencia = ocorrencia,
            lancamentos = emptyList(),
            metadados = metadados,
            veiculoId = moto.id,
            responsavelRetirada = ana.id,
        )

        val bilhete = comResponsavel.paraBilhete(
            ReferenciasDaPassagem(clientesPorId = mapOf(ana.id to ana), veiculosPorId = mapOf(moto.id to moto)),
            agencia = "NAVEG",
        )

        assertEquals("Responsável pela retirada", bilhete.passageiros.single().papel)
        assertEquals("Ana Ribeiro", bilhete.passageiros.single().nome)
    }
}
