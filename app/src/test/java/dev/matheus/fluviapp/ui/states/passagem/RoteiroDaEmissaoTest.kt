package dev.matheus.fluviapp.ui.states.passagem

import dev.matheus.fluviapp.domain.passagem.Acomodacao
import dev.matheus.fluviapp.domain.passagem.CategoriaPassagem
import dev.matheus.fluviapp.domain.passagem.ClasseVeiculo
import dev.matheus.fluviapp.domain.passagem.TipoPassagem
import dev.matheus.fluviapp.revitalizacao.ForaDoEscopo
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.experimental.categories.Category

/**
 * **O roteiro, sem tela e sem ViewModel** ([ADR-0029] D3).
 *
 * É o teste que a decisão de derivar o caminho tornou possível: comparar a lista esperada com a produzida
 * responde *"escolher gratuidade insere o passo do subtipo?"* em três linhas. Com uma sequência fixa e `if`
 * de categoria espalhado pela navegação, a mesma pergunta só se responderia navegando.
 */
@Category(ForaDoEscopo::class)
class RoteiroDaEmissaoTest {

    private fun passageiro(
        acomodacao: Acomodacao? = null,
        tipo: TipoPassagem = TipoPassagem.INTEIRA,
        pessoas: Int = 1,
    ) = roteiroDe(
        BilheteEmEdicao(categoria = CategoriaPassagem.PASSAGEIRO, acomodacao = acomodacao, tipo = tipo),
        ParticipanteEmEdicao.DePassageiro(List(pessoas) { ClienteEmEdicao() }),
    )

    private fun veiculo(classe: ClasseVeiculo? = null) = roteiroDe(
        BilheteEmEdicao(categoria = CategoriaPassagem.VEICULO),
        ParticipanteEmEdicao.DeVeiculo(VeiculoEmEdicao(classe = classe)),
    )

    /** Enquanto a resposta não vem, o roteiro **para** — e é o que mantém "passo N de M" honesto. */
    @Test
    fun `sem acomodacao escolhida, o roteiro para no passo dela`() {
        assertEquals(
            listOf(PassoDaEmissao.Categoria, PassoDaEmissao.EscolhaDeAcomodacao),
            passageiro(acomodacao = null),
        )
    }

    @Test
    fun `rede inteira - seis passos`() {
        assertEquals(
            listOf(
                PassoDaEmissao.Categoria,
                PassoDaEmissao.EscolhaDeAcomodacao,
                PassoDaEmissao.EscolhaDeTipo,
                PassoDaEmissao.DadosDoCliente(0),
                PassoDaEmissao.Pagamento,
                PassoDaEmissao.Desfecho,
            ),
            passageiro(Acomodacao.REDE),
        )
    }

    /** É assim que "gratuidade sem subtipo" deixa de ser estado a validar e passa a ser inalcançável. */
    @Test
    fun `escolher gratuidade insere o passo do subtipo`() {
        val comGratuidade = passageiro(Acomodacao.REDE, tipo = TipoPassagem.GRATUIDADE)

        assertEquals(PassoDaEmissao.EscolhaDeGratuidade, comGratuidade[3])
        assertEquals(passageiro(Acomodacao.REDE).size + 1, comGratuidade.size)
    }

    /** Na rede não há quantidade a perguntar: ela vende **uma** pessoa por bilhete. */
    @Test
    fun `a rede nao pergunta quantidade`() {
        assertEquals(false, PassoDaEmissao.QuantidadeDePessoas in passageiro(Acomodacao.REDE))
    }

    /** E na suíte não há tipo a perguntar: ela é sempre inteira. */
    @Test
    fun `a suite pergunta quantidade e nao pergunta tipo`() {
        val naSuite = passageiro(Acomodacao.SUITE)

        assertEquals(PassoDaEmissao.QuantidadeDePessoas, naSuite[2])
        assertEquals(false, PassoDaEmissao.EscolhaDeTipo in naSuite)
    }

    /** Três pessoas são **três formulários** — a resposta do passo 3.2 desenha o passo 4. */
    @Test
    fun `cada pessoa contada vira um formulario`() {
        val paraTres = passageiro(Acomodacao.CAMAROTE, pessoas = 3)

        assertEquals(
            listOf(
                PassoDaEmissao.DadosDoCliente(0),
                PassoDaEmissao.DadosDoCliente(1),
                PassoDaEmissao.DadosDoCliente(2),
            ),
            paraTres.filterIsInstance<PassoDaEmissao.DadosDoCliente>(),
        )
    }

    // --- Veículo ---

    @Test
    fun `sem classe escolhida, o roteiro do veiculo para no passo dela`() {
        assertEquals(listOf(PassoDaEmissao.Categoria, PassoDaEmissao.ClasseDoVeiculo), veiculo(classe = null))
    }

    @Test
    fun `veiculo - classe, form, responsavel opcional, pagamento e desfecho`() {
        assertEquals(
            listOf(
                PassoDaEmissao.Categoria,
                PassoDaEmissao.ClasseDoVeiculo,
                PassoDaEmissao.DadosDoVeiculo,
                PassoDaEmissao.DadosDoCliente(indice = 0, opcional = true),
                PassoDaEmissao.Pagamento,
                PassoDaEmissao.Desfecho,
            ),
            veiculo(ClasseVeiculo.CARRETA),
        )
    }

    /**
     * **O cliente é o passo 4 nos dois fluxos** — decisão de desenho do analista: quem opera aprende uma
     * sequência só, e a diferença entre os fluxos fica nos passos 2 e 3, que é onde ela é real.
     */
    @Test
    fun `o cliente e o quarto passo nos dois fluxos`() {
        val naRede = passageiro(Acomodacao.REDE)
        val comCarreta = veiculo(ClasseVeiculo.CARRETA)

        assertEquals(PassoDaEmissao.DadosDoCliente(0), naRede[3])
        assertEquals(PassoDaEmissao.DadosDoCliente(0, opcional = true), comCarreta[3])
    }

    /** E o pagamento e o desfecho fecham os dois, na mesma posição. */
    @Test
    fun `pagamento e desfecho fecham os dois fluxos`() {
        listOf(passageiro(Acomodacao.REDE), veiculo(ClasseVeiculo.MOTO)).forEach { roteiro ->
            assertEquals(PassoDaEmissao.Pagamento, roteiro[roteiro.lastIndex - 1])
            assertEquals(PassoDaEmissao.Desfecho, roteiro.last())
        }
    }
}
