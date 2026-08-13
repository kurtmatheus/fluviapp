package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem

import dev.matheus.fluviapp.domain.documento.TipoDocumento
import dev.matheus.fluviapp.domain.passagem.Acomodacao
import dev.matheus.fluviapp.domain.passagem.CategoriaPassagem
import dev.matheus.fluviapp.domain.passagem.ClasseVeiculo
import dev.matheus.fluviapp.domain.passagem.FormaPagamento
import dev.matheus.fluviapp.domain.passagem.TipoPassagem
import dev.matheus.fluviapp.revitalizacao.ForaDoEscopo
import dev.matheus.fluviapp.ui.states.passagem.BilheteEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.ClienteEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.ErroDeEmissao
import dev.matheus.fluviapp.ui.states.passagem.LancamentoEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.PagamentoEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.ParticipanteEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.VeiculoEmEdicao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.categories.Category

/**
 * A validação da emissão **sem ViewModel e sem fake**: entra estado, saem erros.
 *
 * É o contraste com a validação que ela substitui — um helper que **escrevia no `UiState`** enquanto
 * validava, e por isso era monotônica: erro aceso não apagava sozinho, e testar exigia montar o ViewModel
 * inteiro para ler o efeito colateral.
 */
@Category(ForaDoEscopo::class)
class ValidacaoEmissaoTest {

    private val ana = ClienteEmEdicao(
        nome = "Ana Ribeiro",
        tipoDocumento = TipoDocumento.CPF,
        numeroDocumento = "529.982.247-25",
        dataNascimento = "30/01/1996",
    )

    private val emSuite = BilheteEmEdicao(acomodacao = Acomodacao.SUITE, tipo = TipoPassagem.INTEIRA)

    // --- Passo 2: pessoas ---

    @Test
    fun `titular completo basta`() {
        val erros = validarParticipante(emSuite, ParticipanteEmEdicao.DePassageiro(listOf(ana)))

        assertTrue(erros.isEmpty())
    }

    /** Documento inválido é **incompleto**: o CPF com dígito verificador errado nunca identificou ninguém. */
    @Test
    fun `titular com CPF invalido e incompleto`() {
        val invalido = ana.copy(numeroDocumento = "000.000.000-00")

        val erros = validarParticipante(emSuite, ParticipanteEmEdicao.DePassageiro(listOf(invalido)))

        assertEquals(setOf(ErroDeEmissao.TITULAR_INCOMPLETO), erros)
    }

    @Test
    fun `titular sem data de nascimento e incompleto`() {
        val semData = ana.copy(dataNascimento = "")

        val erros = validarParticipante(emSuite, ParticipanteEmEdicao.DePassageiro(listOf(semData)))

        assertEquals(setOf(ErroDeEmissao.TITULAR_INCOMPLETO), erros)
    }

    /** A linha que a tela oferece e o operador não usou não é erro — meia pessoa preenchida é. */
    @Test
    fun `acompanhante vazio nao e erro, acompanhante pela metade e`() {
        val vazio = ParticipanteEmEdicao.DePassageiro(listOf(ana, ClienteEmEdicao()))
        val metade = ParticipanteEmEdicao.DePassageiro(listOf(ana, ClienteEmEdicao(nome = "Bruno")))

        assertTrue(validarParticipante(emSuite, vazio).isEmpty())
        assertEquals(setOf(ErroDeEmissao.ACOMPANHANTE_INCOMPLETO), validarParticipante(emSuite, metade))
    }

    /** Dois "José da Silva" são duas pessoas; dois CPFs iguais são a mesma — a chave é a credencial. */
    @Test
    fun `mesma credencial duas vezes no mesmo bilhete e repeticao`() {
        val repetida = ParticipanteEmEdicao.DePassageiro(listOf(ana, ana.copy(nome = "Ana R.")))

        assertEquals(setOf(ErroDeEmissao.PESSOA_REPETIDA), validarParticipante(emSuite, repetida))
    }

    @Test
    fun `mais pessoas do que a acomodacao admite`() {
        val naRede = BilheteEmEdicao(acomodacao = Acomodacao.REDE)
        val duas = ParticipanteEmEdicao.DePassageiro(
            listOf(ana, ana.copy(nome = "Bruno", numeroDocumento = "111.444.777-35")),
        )

        assertTrue(ErroDeEmissao.EXCEDE_OCUPACAO in validarParticipante(naRede, duas))
    }

    // --- Passo 2: veículo ---

    /** O que falta é o **tipo** quem diz: carreta já é o modelo, e só moto tem cilindrada. */
    @Test
    fun `carreta nao pede modelo`() {
        val carreta = ParticipanteEmEdicao.DeVeiculo(
            VeiculoEmEdicao(placa = "XYZ9A88", classe = ClasseVeiculo.CARRETA),
        )

        assertTrue(validarParticipante(BilheteEmEdicao(categoria = CategoriaPassagem.VEICULO), carreta).isEmpty())
    }

    @Test
    fun `van sem modelo e moto sem cilindrada sao cobradas`() {
        val bilhete = BilheteEmEdicao(categoria = CategoriaPassagem.VEICULO)
        val van = ParticipanteEmEdicao.DeVeiculo(VeiculoEmEdicao(placa = "AAA1B11", classe = ClasseVeiculo.VAN))
        val moto = ParticipanteEmEdicao.DeVeiculo(
            VeiculoEmEdicao(placa = "AAA1B11", classe = ClasseVeiculo.MOTO, modelo = "Fan"),
        )

        assertEquals(setOf(ErroDeEmissao.VEICULO_SEM_MODELO), validarParticipante(bilhete, van))
        assertEquals(setOf(ErroDeEmissao.VEICULO_SEM_CILINDRADA), validarParticipante(bilhete, moto))
    }

    /** Ausente é a forma normal ([ADR-0028] D3); pela metade é erro. */
    @Test
    fun `responsavel ausente passa, responsavel incompleto nao`() {
        val bilhete = BilheteEmEdicao(categoria = CategoriaPassagem.VEICULO)
        val veiculo = VeiculoEmEdicao(placa = "XYZ9A88", classe = ClasseVeiculo.CARRETA)

        val semNinguem = ParticipanteEmEdicao.DeVeiculo(veiculo, responsavel = null)
        val pelaMetade = ParticipanteEmEdicao.DeVeiculo(veiculo, responsavel = ClienteEmEdicao(nome = "João"))

        assertTrue(validarParticipante(bilhete, semNinguem).isEmpty())
        assertEquals(setOf(ErroDeEmissao.RESPONSAVEL_INCOMPLETO), validarParticipante(bilhete, pelaMetade))
    }

    // --- Passo 3: pagamento ---

    @Test
    fun `bilhete pago exige lancamento`() {
        val erros = validarPagamento(BilheteEmEdicao(acomodacao = Acomodacao.REDE), PagamentoEmEdicao())

        assertEquals(setOf(ErroDeEmissao.SEM_PAGAMENTO), erros)
    }

    /** Gratuidade é tarifa zero por lei, não pagamento de zero — cobrar dela seria cobrar de quem não paga. */
    @Test
    fun `gratuidade nao exige lancamento`() {
        val gratuito = BilheteEmEdicao(acomodacao = Acomodacao.REDE, tipo = TipoPassagem.GRATUIDADE)

        assertTrue(validarPagamento(gratuito, PagamentoEmEdicao()).isEmpty())
    }

    @Test
    fun `valor que nao vira numero e recusado`() {
        val pagamento = PagamentoEmEdicao(listOf(LancamentoEmEdicao(FormaPagamento.PIX, "cem reais")))

        assertTrue(ErroDeEmissao.VALOR_INVALIDO in validarPagamento(BilheteEmEdicao(), pagamento))
    }

    @Test
    fun `o total e a soma dos lancamentos, e nao um campo`() {
        val pagamento = PagamentoEmEdicao(
            listOf(
                LancamentoEmEdicao(FormaPagamento.PIX, "100,00"),
                LancamentoEmEdicao(FormaPagamento.DINHEIRO, "50,50"),
            ),
        )

        assertEquals("150.50", pagamento.total.toPlainString())
    }
}