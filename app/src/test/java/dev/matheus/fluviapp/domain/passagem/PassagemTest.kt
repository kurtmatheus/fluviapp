package dev.matheus.fluviapp.domain.passagem

import dev.matheus.fluviapp.domain.viagem.OcorrenciaViagem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

/**
 * O agregado como **tipo fechado por categoria** (ADR-0023 D1).
 *
 * Metade destes casos não testa cálculo: testa **o que deixou de ser escrevível**. A outra metade cobre as
 * pendências — a forma que o agregado usa para dizer *qual* regra falhou, em vez de um booleano que obrigaria
 * quem chama a repetir a regra para descobrir o motivo.
 *
 * **De volta ao escopo na F9.6**: o portador (a seção `PASSAGEM`) acendeu, e o agregado passou a ser escrito
 * de verdade pela emissão. A régua é a que o `TipoEmbarcacaoTest` escreveu.
 */
class PassagemTest {

    private val ocorrencia = OcorrenciaViagem("v1", LocalDate.of(2026, 8, 18))

    private val metadados = MetadadosPassagem(
        status = StatusPassagem.EMITIDA,
        funcionarioId = "uid_9",
        agenciaId = "emp_3",
        criadoEm = "2026-08-11T14:32:00",
        alteradoEm = "2026-08-11T14:32:00",
    )

    private val lancamentos = listOf(Lancamento("l1", FormaPagamento.PIX, BigDecimal("50.00")))

    private fun passageiro(
        acomodacao: Acomodacao = Acomodacao.REDE,
        tipo: TipoPassagem = TipoPassagem.INTEIRA,
        gratuidade: TipoGratuidade? = null,
        clientes: List<String> = listOf("cli_1"),
    ) = PassagemDePassageiro(
        numero = "001234",
        ocorrencia = ocorrencia,
        lancamentos = lancamentos,
        metadados = metadados,
        acomodacao = acomodacao,
        tipo = tipo,
        gratuidade = gratuidade,
        clientes = clientes,
    )

    private fun veiculo(veiculoId: String = "vei_44", responsavel: String? = null) = PassagemDeVeiculo(
        numero = "001235",
        ocorrencia = ocorrencia,
        lancamentos = lancamentos,
        metadados = metadados,
        veiculoId = veiculoId,
        responsavelRetirada = responsavel,
    )

    // --- A categoria é do tipo, não de um campo que alguém preenche ---

    @Test
    fun `a categoria vem do tipo`() {
        assertEquals(CategoriaPassagem.PASSAGEIRO, passageiro().categoria)
        assertEquals(CategoriaPassagem.VEICULO, veiculo().categoria)
    }

    /**
     * O `when` exaustivo é o mecanismo que o ADR-0023 D1 comprou: quando a carga entrar, é ele que vai acusar
     * cada lugar a decidir algo sobre ela. Este caso existe para que o mecanismo esteja exercitado.
     */
    @Test
    fun `um when sobre a passagem cobre as duas categorias`() {
        val passagens: List<Passagem> = listOf(passageiro(), veiculo())

        val descricoes = passagens.map { passagem ->
            when (passagem) {
                is PassagemDePassageiro -> "pessoa em ${passagem.acomodacao.rotulo}"
                is PassagemDeVeiculo -> "veiculo ${passagem.veiculoId}"
            }
        }

        assertEquals(listOf("pessoa em Rede", "veiculo vei_44"), descricoes)
    }

    // --- O comum é comum ---

    @Test
    fun `as duas categorias carregam a mesma ocorrencia e o mesmo total`() {
        val passagens: List<Passagem> = listOf(passageiro(), veiculo())

        passagens.forEach {
            assertEquals("v1", it.ocorrencia.viagemId)
            assertEquals("2026-08-18", it.ocorrencia.dataIso)
            assertEquals(BigDecimal("50.00"), it.lancamentos.total)
        }
    }

    /** Nada de nome: o agregado guarda id, e o nome se resolve por referência (D8). */
    @Test
    fun `os metadados guardam ids, e o carimbo de embarque nasce ausente`() {
        val passagem = passageiro()

        assertEquals("uid_9", passagem.metadados.funcionarioId)
        assertEquals("emp_3", passagem.metadados.agenciaId)
        assertNull(passagem.metadados.embarque)
    }

    @Test
    fun `carimbo de embarque existe inteiro quando existe`() {
        val embarcada = passageiro().copy(
            metadados = metadados.copy(
                status = StatusPassagem.EMBARCADA,
                embarque = CarimboEmbarque(porId = "uid_7", em = "2026-08-18T18:05:00"),
            ),
        )

        assertEquals("uid_7", embarcada.metadados.embarque?.porId)
        assertEquals("2026-08-18T18:05:00", embarcada.metadados.embarque?.em)
    }

    // --- Passageiro: titular, ocupação e tipo tarifário ---

    @Test
    fun `o titular e a posicao zero`() {
        val trio = passageiro(Acomodacao.SUITE, clientes = listOf("cli_1", "cli_2", "cli_3"))

        assertEquals("cli_1", trio.titularId)
        assertEquals(listOf("cli_2", "cli_3"), trio.acompanhantesIds)
    }

    @Test
    fun `passagem de rede com um cliente inteira e coerente`() {
        assertTrue(passageiro().coerente)
    }

    @Test
    fun `sem cliente nao ha titular`() {
        val vazia = passageiro(clientes = emptyList())

        assertNull(vazia.titularId)
        assertTrue(PassagemDePassageiro.Pendencia.SEM_TITULAR in vazia.pendencias())
    }

    /** A rede é um cliente por bilhete: dois na mesma rede não é meio-preenchido, é excesso. */
    @Test
    fun `dois clientes numa rede excedem a ocupacao`() {
        val excedida = passageiro(clientes = listOf("cli_1", "cli_2"))

        assertEquals(setOf(PassagemDePassageiro.Pendencia.EXCEDE_OCUPACAO), excedida.pendencias())
    }

    @Test
    fun `suite aceita ate tres e recusa o quarto`() {
        assertTrue(passageiro(Acomodacao.SUITE, clientes = listOf("c1", "c2", "c3")).coerente)
        assertTrue(
            PassagemDePassageiro.Pendencia.EXCEDE_OCUPACAO in
                passageiro(Acomodacao.SUITE, clientes = listOf("c1", "c2", "c3", "c4")).pendencias(),
        )
    }

    /** O mesmo cliente duas vezes no bilhete é erro de digitação, não um acompanhante. */
    @Test
    fun `cliente repetido e pendencia`() {
        val repetido = passageiro(Acomodacao.CAMAROTE, clientes = listOf("cli_1", "cli_1"))

        assertTrue(PassagemDePassageiro.Pendencia.CLIENTE_REPETIDO in repetido.pendencias())
    }

    /** Meia numa suíte: a regra mora na acomodação, e o agregado só a consulta. */
    @Test
    fun `meia numa suite e tipo nao admitido`() {
        val meiaSuite = passageiro(Acomodacao.SUITE, tipo = TipoPassagem.MEIA)

        assertEquals(setOf(PassagemDePassageiro.Pendencia.TIPO_NAO_ADMITIDO), meiaSuite.pendencias())
    }

    @Test
    fun `gratuidade na rede e admitida`() {
        val gratuita = passageiro(Acomodacao.REDE, TipoPassagem.GRATUIDADE, TipoGratuidade.IDOSO)

        assertTrue(gratuita.coerente)
    }

    /**
     * **A lacuna que a F9.1 deixou e o [ADR-0028] D2 fechou**: gravar "gratuidade" sem dizer qual produzia um
     * rótulo que não serve à fiscalização e sobre o qual a cota do ADR-0013 §8 não tem o que contar.
     */
    @Test
    fun `gratuidade sem subtipo e incoerente`() {
        val semSubtipo = passageiro(Acomodacao.REDE, tipo = TipoPassagem.GRATUIDADE)

        assertEquals(
            setOf(PassagemDePassageiro.Pendencia.GRATUIDADE_SEM_SUBTIPO),
            semSubtipo.pendencias(),
        )
    }

    /** E o inverso, que é a outra metade do par: subtipo pendurado em bilhete que não é gratuito. */
    @Test
    fun `subtipo em passagem inteira e incoerente`() {
        val inteiraComSubtipo = passageiro(Acomodacao.REDE, TipoPassagem.INTEIRA, TipoGratuidade.PCD)

        assertEquals(
            setOf(PassagemDePassageiro.Pendencia.SUBTIPO_SEM_GRATUIDADE),
            inteiraComSubtipo.pendencias(),
        )
    }

    // --- Veículo ---

    @Test
    fun `veiculo sem responsavel pela retirada e a forma normal`() {
        val semResponsavel = veiculo(responsavel = null)

        assertTrue(semResponsavel.coerente)
        assertNull(semResponsavel.responsavelRetirada)
    }

    @Test
    fun `veiculo com responsavel guarda o id do cliente`() {
        assertEquals("cli_7", veiculo(responsavel = "cli_7").responsavelRetirada)
    }

    @Test
    fun `passagem de veiculo sem veiculo e pendencia`() {
        assertEquals(setOf(PassagemDeVeiculo.Pendencia.SEM_VEICULO), veiculo(veiculoId = " ").pendencias())
        assertFalse(veiculo(veiculoId = "").coerente)
    }
}
