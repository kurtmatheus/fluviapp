package dev.matheus.fluviapp.ui.states.passagem

import dev.matheus.fluviapp.domain.passagem.Acomodacao
import dev.matheus.fluviapp.domain.passagem.CategoriaPassagem
import dev.matheus.fluviapp.domain.passagem.ClasseVeiculo
import dev.matheus.fluviapp.domain.viagem.TipoEmbarcacao
import dev.matheus.fluviapp.domain.passagem.NaturezaVeiculo
import dev.matheus.fluviapp.domain.passagem.TipoPassagem
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * **O roteiro, sem tela e sem ViewModel** ([ADR-0029] D3).
 *
 * É o teste que a decisão de derivar o caminho tornou possível: comparar a lista esperada com a produzida
 * responde *"escolher gratuidade insere o passo do subtipo?"* em três linhas. Com uma sequência fixa e `if`
 * de categoria espalhado pela navegação, a mesma pergunta só se responderia navegando.
 */
class RoteiroDaEmissaoTest {

    private fun passageiro(
        acomodacao: Acomodacao? = null,
        tipo: TipoPassagem = TipoPassagem.INTEIRA,
        pessoas: Int = 1,
    ) = roteiroDe(
        BilheteEmEdicao(categoria = CategoriaPassagem.PASSAGEIRO, acomodacao = acomodacao, tipo = tipo),
        ParticipanteEmEdicao.DePassageiro(List(pessoas) { ClienteEmEdicao() }),
    )

    private fun veiculo(
        natureza: NaturezaVeiculo? = null,
        classe: ClasseVeiculo? = null,
        tipoEmbarcacao: TipoEmbarcacao? = null,
    ) = roteiroDe(
        BilheteEmEdicao(categoria = CategoriaPassagem.VEICULO),
        ParticipanteEmEdicao.DeVeiculo(VeiculoEmEdicao(natureza = natureza, classe = classe)),
        tipoEmbarcacao,
    )

    /** Enquanto a resposta não vem, o roteiro **para** — e é o que mantém "passo N de M" honesto. */
    @Test
    fun `sem acomodacao escolhida, o roteiro para no passo dela`() {
        assertEquals(
            listOf(PassoDaEmissao.Categoria, PassoDaEmissao.EscolhaDeAcomodacao),
            passageiro(acomodacao = null),
        )
    }

    /** Cinco desde que a tela de desfecho saiu: emitir leva **direto ao bilhete**, que é outro destino. */
    @Test
    fun `rede inteira - cinco passos`() {
        assertEquals(
            listOf(
                PassoDaEmissao.Categoria,
                PassoDaEmissao.EscolhaDeAcomodacao,
                PassoDaEmissao.EscolhaDeTipo,
                PassoDaEmissao.DadosDoCliente(0),
                PassoDaEmissao.Pagamento,
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

    /** Sem natureza escolhida, o roteiro para no passo dela — a classe nem se anuncia. */
    @Test
    fun `sem natureza escolhida, o roteiro do veiculo para no passo dela`() {
        assertEquals(listOf(PassoDaEmissao.Categoria, PassoDaEmissao.NaturezaDoVeiculo), veiculo())
    }

    /** Escolhida a natureza e faltando a classe, o subpasso aparece e o roteiro para nele. */
    @Test
    fun `escolher a natureza insere o subpasso da classe`() {
        assertEquals(
            listOf(
                PassoDaEmissao.Categoria,
                PassoDaEmissao.NaturezaDoVeiculo,
                PassoDaEmissao.ClasseDoVeiculo,
            ),
            veiculo(natureza = NaturezaVeiculo.REBOCADO),
        )
    }

    @Test
    fun `veiculo - natureza, classe, form, responsavel opcional e pagamento`() {
        assertEquals(
            listOf(
                PassoDaEmissao.Categoria,
                PassoDaEmissao.NaturezaDoVeiculo,
                PassoDaEmissao.ClasseDoVeiculo,
                PassoDaEmissao.DadosDoVeiculo,
                PassoDaEmissao.DadosDoCliente(indice = 0, opcional = true),
                PassoDaEmissao.Pagamento,
            ),
            veiculo(natureza = NaturezaVeiculo.REBOCADO, classe = ClasseVeiculo.CARRETA),
        )
    }

    /**
     * **O subpasso some quando tem uma resposta só** ([ADR-0031] D8).
     *
     * Num navio, a natureza automotor admite **uma** classe a bordo — o carro. Perguntar qual seria o que o
     * ADR-0029 chama de passo sem pergunta, e o roteiro simplesmente não o insere.
     */
    @Test
    fun `no navio a classe nao se pergunta - o subpasso se dissolve`() {
        assertEquals(
            listOf(
                PassoDaEmissao.Categoria,
                PassoDaEmissao.NaturezaDoVeiculo,
                PassoDaEmissao.DadosDoVeiculo,
                PassoDaEmissao.DadosDoCliente(indice = 0, opcional = true),
                PassoDaEmissao.Pagamento,
            ),
            veiculo(
                natureza = NaturezaVeiculo.AUTOMOTOR,
                classe = ClasseVeiculo.CARRO,
                tipoEmbarcacao = TipoEmbarcacao.NAVIO,
            ),
        )
    }

    /** Na balsa a mesma natureza tem sete classes, então a pergunta existe. */
    @Test
    fun `na balsa a mesma natureza mantem o subpasso`() {
        val naBalsa = veiculo(
            natureza = NaturezaVeiculo.AUTOMOTOR,
            classe = ClasseVeiculo.CARRO,
            tipoEmbarcacao = TipoEmbarcacao.FERRY_BOAT,
        )

        assertEquals(PassoDaEmissao.ClasseDoVeiculo, naBalsa[2])
    }

    /**
     * **A simetria do ADR-0029 quebrou, e a quebra foi aceita** (ADR-0031 D8): com a natureza inserida, o
     * cliente vai para o **quinto** passo no fluxo de veículo. O roteiro é derivado e mostra *"passo N de
     * M"*, então quem opera lê o contador em vez de contar passos.
     *
     * E ela **sobrevive onde o casco é estreito**: num navio a classe não se pergunta, e o cliente volta a
     * ser o quarto nos dois fluxos.
     */
    @Test
    fun `o cliente e o quinto no veiculo, e volta a ser o quarto quando a classe nao se pergunta`() {
        val naRede = passageiro(Acomodacao.REDE)
        val naBalsa = veiculo(NaturezaVeiculo.REBOCADO, ClasseVeiculo.CARRETA, TipoEmbarcacao.FERRY_BOAT)
        val noNavio = veiculo(NaturezaVeiculo.AUTOMOTOR, ClasseVeiculo.CARRO, TipoEmbarcacao.NAVIO)

        assertEquals(PassoDaEmissao.DadosDoCliente(0), naRede[3])
        assertEquals(PassoDaEmissao.DadosDoCliente(0, opcional = true), naBalsa[4])
        assertEquals(PassoDaEmissao.DadosDoCliente(0, opcional = true), noNavio[3])
    }

    /** E o **pagamento fecha** os dois fluxos: depois dele não há passo, há outro destino — o bilhete. */
    @Test
    fun `o pagamento fecha os dois fluxos`() {
        listOf(
            passageiro(Acomodacao.REDE),
            veiculo(NaturezaVeiculo.MOTOCICLO, ClasseVeiculo.MOTO),
        ).forEach { roteiro ->
            assertEquals(PassoDaEmissao.Pagamento, roteiro.last())
        }
    }
}
