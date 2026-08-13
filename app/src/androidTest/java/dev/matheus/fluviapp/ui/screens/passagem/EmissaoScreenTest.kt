package dev.matheus.fluviapp.ui.screens.passagem

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import dev.matheus.fluviapp.domain.documento.TipoDocumento
import dev.matheus.fluviapp.extensions.formataParaMoedaBrasileira
import dev.matheus.fluviapp.domain.passagem.Acomodacao
import dev.matheus.fluviapp.domain.passagem.CategoriaPassagem
import dev.matheus.fluviapp.domain.passagem.ClasseVeiculo
import dev.matheus.fluviapp.domain.passagem.FormaPagamento
import dev.matheus.fluviapp.domain.passagem.TipoPassagem
import dev.matheus.fluviapp.ui.screens.passagem.emissao.EmissaoScreen
import dev.matheus.fluviapp.ui.states.passagem.BilheteEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.CabecalhoDaViagem
import dev.matheus.fluviapp.ui.states.passagem.ClienteEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.ConfirmacaoDaEmissao
import dev.matheus.fluviapp.ui.states.passagem.EmissaoUiState
import dev.matheus.fluviapp.ui.states.passagem.LancamentoConferido
import dev.matheus.fluviapp.ui.states.passagem.LancamentoEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.PagamentoEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.ParticipanteEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.PessoaConferida
import dev.matheus.fluviapp.ui.states.passagem.VeiculoEmEdicao
import dev.matheus.fluviapp.ui.theme.FluviAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal

/**
 * **A emissão em tela** (F9.5) — a rede que os dois defeitos da F8 ensinaram a exigir: eles atravessaram
 * todas as suítes de JVM e morreram só no aparelho.
 *
 * O que só a renderização prova aqui:
 *
 * - **o roteiro vira tela**: cada passo mostra a pergunta dele, e a escolha anterior decide qual é a
 *   próxima — é a diferença entre a lista derivada estar certa e o `when` da tela concordar com ela;
 * - **o toque escolhe e avança** (não há botão de confirmar nos passos de escolha);
 * - **o formulário do veículo se rearranja pela classe** — carreta sem modelo, moto com cilindrada;
 * - **a conferência não é passo**: ela substitui o conteúdo sem mexer na trilha.
 */
class EmissaoScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val cabecalho = CabecalhoDaViagem(
        travessia = "Porto de Val-de-Cães · Belém/PA → Porto de Parintins · Parintins/AM",
        partida = "Terça-feira, 18/08 · 18:00",
        embarcacao = "F/B Modelo",
    )

    private fun montarTela(
        state: EmissaoUiState,
        onEscolherCategoria: (CategoriaPassagem) -> Unit = {},
        onEscolherAcomodacao: (Acomodacao) -> Unit = {},
        onEscolherTipo: (TipoPassagem) -> Unit = {},
        onEscolherQuantidade: (Int) -> Unit = {},
        onEscolherClasse: (ClasseVeiculo) -> Unit = {},
        onPreencherPessoa: (Int, ClienteEmEdicao) -> Unit = { _, _ -> },
        onAvancar: () -> Unit = {},
        onPular: () -> Unit = {},
        onConfirmarEmissao: () -> Unit = {},
        onRevisar: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            FluviAppTheme {
                EmissaoScreen(
                    state = state,
                    onEscolherCategoria = onEscolherCategoria,
                    onEscolherAcomodacao = onEscolherAcomodacao,
                    onEscolherTipo = onEscolherTipo,
                    onEscolherQuantidade = onEscolherQuantidade,
                    onEscolherClasse = onEscolherClasse,
                    onPreencherPessoa = onPreencherPessoa,
                    onAvancar = onAvancar,
                    onPular = onPular,
                    onConfirmarEmissao = onConfirmarEmissao,
                    onRevisar = onRevisar,
                )
            }
        }
    }

    // --- Passo 1: a categoria, e o cabeçalho que acompanha ---

    @Test
    fun passo1_mostraAsDuasCategoriasEOCabecalhoDaSaida() {
        montarTela(EmissaoUiState(cabecalho = cabecalho))

        composeTestRule.onNodeWithText("O que vai embarcar?").assertIsDisplayed()
        composeTestRule.onNodeWithText(CategoriaPassagem.PASSAGEIRO.rotulo).assertIsDisplayed()
        composeTestRule.onNodeWithText(CategoriaPassagem.VEICULO.rotulo).assertIsDisplayed()
        // O cabeçalho é persistente, e a data **não é campo**: ela veio da saída escolhida.
        composeTestRule.onNodeWithText("Terça-feira, 18/08 · 18:00 · F/B Modelo").assertIsDisplayed()
    }

    /** O toque **é** a resposta: não há "avançar" nos passos de escolha. */
    @Test
    fun passo1_tocarNaCategoriaEscolhe() {
        var escolhida: CategoriaPassagem? = null
        montarTela(EmissaoUiState(cabecalho = cabecalho), onEscolherCategoria = { escolhida = it })

        composeTestRule.onNodeWithText(CategoriaPassagem.VEICULO.rotulo).performClick()

        assertEquals(CategoriaPassagem.VEICULO, escolhida)
    }

    // --- Passo 2 e 3: o que o domínio deixa escolher ---

    @Test
    fun passo2_mostraAsTresAcomodacoesComAOcupacao() {
        montarTela(EmissaoUiState(cabecalho = cabecalho, indiceDoPasso = 1))

        composeTestRule.onNodeWithText("Onde o passageiro viaja?").assertIsDisplayed()
        Acomodacao.entries.forEach {
            composeTestRule.onNodeWithText(it.rotulo).assertIsDisplayed()
        }
        // A ocupação aparece porque é o que distingue rede de suíte na hora de vender. "até 3" aparece
        // **duas vezes** de propósito — suíte e camarote têm a mesma capacidade —, então a asserção conta
        // em vez de exigir nó único.
        composeTestRule.onNodeWithText("1 pessoa").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("até 3").assertCountEquals(2)
    }

    /**
     * **A regra na tela**: a rede admite os três tipos; suíte e camarote, só inteira — então lá o passo nem
     * existe. Aqui se prova a metade visível: os três aparecem na rede.
     */
    @Test
    fun passo3_naRedeOsTresTiposAparecem() {
        montarTela(
            EmissaoUiState(
                cabecalho = cabecalho,
                indiceDoPasso = 2,
                bilhete = BilheteEmEdicao(acomodacao = Acomodacao.REDE),
            ),
        )

        composeTestRule.onNodeWithText("Qual o tipo do bilhete?").assertIsDisplayed()
        TipoPassagem.entries.forEach {
            composeTestRule.onNodeWithText(it.rotulo()).assertIsDisplayed()
        }
    }

    @Test
    fun passo3_naSuiteSePerguntaQuantasPessoas() {
        montarTela(
            EmissaoUiState(
                cabecalho = cabecalho,
                indiceDoPasso = 2,
                bilhete = BilheteEmEdicao(acomodacao = Acomodacao.SUITE),
            ),
        )

        composeTestRule.onNodeWithText("Quantas pessoas?").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 pessoa").assertIsDisplayed()
        composeTestRule.onNodeWithText("3 pessoas").assertIsDisplayed()
    }

    // --- Passo 4: os formulários ---

    @Test
    fun passo4_oFormularioDePessoaPedeNomeDocumentoENascimento() {
        val estado = EmissaoUiState(
            cabecalho = cabecalho,
            indiceDoPasso = 3,
            bilhete = BilheteEmEdicao(acomodacao = Acomodacao.REDE),
        )
        montarTela(estado)

        composeTestRule.onNodeWithText("Quem viaja?").assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(dev.matheus.fluviapp.R.string.label_nome_passageiro))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(dev.matheus.fluviapp.R.string.label_tipo_documento))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(dev.matheus.fluviapp.R.string.label_data_nascimento))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun passo4_digitarONomeChegaAoViewModel() {
        var digitado: ClienteEmEdicao? = null
        montarTela(
            EmissaoUiState(
                cabecalho = cabecalho,
                indiceDoPasso = 3,
                bilhete = BilheteEmEdicao(acomodacao = Acomodacao.REDE),
            ),
            onPreencherPessoa = { _, pessoa -> digitado = pessoa },
        )

        composeTestRule.onNodeWithText(texto(dev.matheus.fluviapp.R.string.label_nome_passageiro))
            .performTextInput("Ana")

        assertEquals("Ana", digitado?.nome)
    }

    /**
     * **O formulário se rearranja pela classe** (ADR-0023 D4): carreta *já é* o modelo, então o campo não
     * existe — e campo que não existe é melhor que campo desabilitado.
     */
    @Test
    fun passo3Veiculo_carretaNaoPedeModeloNemCilindrada() {
        montarTela(estadoDeVeiculo(ClasseVeiculo.CARRETA))

        composeTestRule.onNodeWithText(texto(dev.matheus.fluviapp.R.string.label_placa_veículo))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(dev.matheus.fluviapp.R.string.label_modelo_veiculo))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(texto(dev.matheus.fluviapp.R.string.label_cilindrada))
            .assertDoesNotExist()
    }

    @Test
    fun passo3Veiculo_motoPedeModeloECilindrada() {
        montarTela(estadoDeVeiculo(ClasseVeiculo.MOTO))

        composeTestRule.onNodeWithText(texto(dev.matheus.fluviapp.R.string.label_modelo_veiculo))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(dev.matheus.fluviapp.R.string.label_cilindrada))
            .performScrollTo()
            .assertIsDisplayed()
    }

    /** O passo do responsável é o **4 do outro fluxo**, e traz o "Pular": não nomear ninguém é o normal. */
    @Test
    fun passo4Veiculo_oResponsavelPodeSerPulado() {
        var pulou = false
        montarTela(
            EmissaoUiState(
                cabecalho = cabecalho,
                indiceDoPasso = 3,
                bilhete = BilheteEmEdicao(categoria = CategoriaPassagem.VEICULO),
                participante = ParticipanteEmEdicao.DeVeiculo(
                    VeiculoEmEdicao(placa = "ABC1D23", classe = ClasseVeiculo.CARRETA),
                ),
            ),
            onPular = { pulou = true },
        )

        composeTestRule.onNodeWithText("Quem retira o veículo? (opcional)").assertIsDisplayed()
        // A barra de ação vive **fora** da área rolável, fixa no rodapé: não há para onde rolar até ela, e
        // é isso que garante que o botão está sempre à mão num formulário longo.
        composeTestRule.onNodeWithText("Pular").performClick()

        assertEquals(true, pulou)
    }

    // --- Passo 5: o pagamento ---

    @Test
    fun passo5_marcarAFormaAbreOCampoDeValor() {
        val comPix = PagamentoEmEdicao(lancamentos = listOf(LancamentoEmEdicao(FormaPagamento.PIX, "50,00")))
        montarTela(
            EmissaoUiState(
                cabecalho = cabecalho,
                indiceDoPasso = 4,
                bilhete = BilheteEmEdicao(acomodacao = Acomodacao.REDE),
                pagamento = comPix,
            ),
        )

        composeTestRule.onNodeWithText("Como foi pago?").assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(dev.matheus.fluviapp.R.string.label_valor_recebido))
            .assertIsDisplayed()
        // O total é **exibido**, nunca digitado: ele é a soma das linhas — e fica na barra fixa, porque é o
        // número que se confere contra o dinheiro na mão.
        //
        // O esperado é montado pela **mesma função** que a tela usa, e não escrito à mão: a formatação
        // brasileira separa "R$" do número com **espaço não separável** (U+00A0), invisível no código e
        // suficiente para o `onNodeWithText` não casar. Foi o que o aparelho cobrou aqui.
        composeTestRule.onNodeWithText("Total: ${BigDecimal("50.00").formataParaMoedaBrasileira()}")
            .assertIsDisplayed()
    }

    // --- A conferência, que não é passo ---

    @Test
    fun conferencia_mostraOsDadosEOsDoisGestos() {
        var confirmou = false
        var revisou = false
        montarTela(
            EmissaoUiState(
                cabecalho = cabecalho,
                indiceDoPasso = 4,
                confirmacao = confirmacao(),
            ),
            onConfirmarEmissao = { confirmou = true },
            onRevisar = { revisou = true },
        )

        composeTestRule.onNodeWithText("Conferência").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ana Ribeiro").assertIsDisplayed()
        // **Mascarado**: o detalhamento fica aberto de frente para a fila, e documento inteiro numa tela
        // parada é o que se decora de relance ou sai numa foto (LGPD).
        composeTestRule.onNodeWithText("CPF ###.###.247-25").assertIsDisplayed()
        // Duas ocorrências e as duas são certas: o lançamento e o total. Somar R$ 150,00 uma vez só daria
        // o mesmo número, e é justamente essa coincidência que o caso não deve tratar como ambiguidade.
        composeTestRule.onAllNodesWithText("R$ 150,00").assertCountEquals(2)

        composeTestRule.onNodeWithText("Corrigir").performClick()
        composeTestRule.onNodeWithText("Emitir").performClick()

        assertEquals(true, revisou)
        assertEquals(true, confirmou)
    }

    /** A gratuidade precisa **dizer** que não houve pagamento — vazio pareceria dado perdido. */
    @Test
    fun conferencia_gratuidadeAnunciaAAusenciaDePagamento() {
        montarTela(
            EmissaoUiState(
                cabecalho = cabecalho,
                indiceDoPasso = 4,
                confirmacao = confirmacao(lancamentos = emptyList(), total = "R$ 0,00"),
            ),
        )

        composeTestRule.onNodeWithText("Sem pagamento (gratuidade)").assertIsDisplayed()
    }

    private fun estadoDeVeiculo(classe: ClasseVeiculo) = EmissaoUiState(
        cabecalho = cabecalho,
        indiceDoPasso = 2,
        bilhete = BilheteEmEdicao(categoria = CategoriaPassagem.VEICULO),
        participante = ParticipanteEmEdicao.DeVeiculo(VeiculoEmEdicao(classe = classe)),
    )

    private fun confirmacao(
        lancamentos: List<LancamentoConferido> = listOf(LancamentoConferido("Dinheiro", "R$ 150,00")),
        total: String = "R$ 150,00",
    ) = ConfirmacaoDaEmissao(
        cabecalho = cabecalho,
        bilhete = "Rede",
        pessoas = listOf(
            PessoaConferida(
                papel = "Passageiro",
                nome = "Ana Ribeiro",
                // O documento chega **mascarado** ao DTO — quem o oculta é o mapper, não a tela.
                documento = "${TipoDocumento.CPF.rotulo} ${TipoDocumento.CPF.mascarar("52998224725")}",
                nascimento = "30/01/1996",
            ),
        ),
        lancamentos = lancamentos,
        total = total,
        observacao = null,
        agencia = "NAVEG",
    )

    private fun texto(id: Int) = composeTestRule.activity.getString(id)
}