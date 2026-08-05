package dev.matheus.fluviapp.ui.screens.forms.embarcacao

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.states.EmbarcacaoResultado
import dev.matheus.fluviapp.ui.states.PesquisaEmbarcacaoUiState
import dev.matheus.fluviapp.ui.theme.FluviAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * A outra metade do CRUD da Flotilha em tela: **ler, editar e apagar**. O cadastro é coberto pelo
 * `FormEmbarcacaoScreenTest`; aqui fica o que a lista promete — mostrar o que veio, avisar o filtro, levar
 * ao editor e não apagar nada sem confirmação.
 *
 * Mesmo molde da busca de Empresa, com uma diferença de forma que vale registrar: o filtro daqui é
 * **empresa** (a Embarcação tem dono), enquanto o da Empresa é o próprio nome. Nos dois casos quem filtra
 * é o ViewModel — a tela só avisa o que foi escolhido.
 */
class ResultSearchEmbarcacaoScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun texto(id: Int) = composeTestRule.activity.getString(id)

    private val ferry = EmbarcacaoResultado("1", "F/B MODELO", "Ferry Boat", "NAVEGA MODELO")
    private val lancha = EmbarcacaoResultado("2", "LANCHA VELOZ", "Lancha", "TRANSPORTE ILHA")

    /** A ordem dos cards na tela — é dela que sai o índice do ícone de cada linha. */
    private val resultados = listOf(ferry, lancha)

    private fun montar(
        uiState: PesquisaEmbarcacaoUiState = PesquisaEmbarcacaoUiState(
            listaEmpresas = listOf("NAVEGA MODELO", "TRANSPORTE ILHA"),
            resultados = resultados,
        ),
        onEmpresaChange: (String) -> Unit = {},
        onNavegaParaEditor: (String) -> Unit = {},
        onDeletar: (String) -> Unit = {},
    ) {
        composeTestRule.setContent {
            FluviAppTheme {
                ResultSearchEmbarcacaoScreen(
                    uiState = uiState,
                    onEmpresaChange = onEmpresaChange,
                    onNavegaParaEditor = onNavegaParaEditor,
                    onDeletar = onDeletar,
                )
            }
        }
    }

    /**
     * O ícone de uma linha específica, pelo índice.
     *
     * Pela mesma razão da busca de Empresa: o `Row` do `CardResultEmbarcacao` não emite semântica, então
     * textos e `IconButton` são todos irmãos na árvore — não existe "a linha" para ser ancestral, e os
     * `contentDescription` ("Editar", "Deletar") repetem-se por card sem dizer de qual embarcação. É uma
     * limitação de acessibilidade antes de ser de teste, e está anotada nos dois lugares.
     */
    private fun icone(descricao: Int, naLinhaDe: EmbarcacaoResultado): SemanticsNodeInteraction =
        composeTestRule
            .onAllNodes(hasContentDescription(texto(descricao)))[resultados.indexOf(naLinhaDe)]

    // --- Ler ---

    @Test
    fun pesquisa_exibeOsResultadosComTipoEEmpresa() {
        montar()

        composeTestRule.onNodeWithText(texto(R.string.subtitle_pesquisar_embarcacoes)).assertIsDisplayed()
        composeTestRule.onNodeWithText(ferry.nome).assertIsDisplayed()
        composeTestRule.onNodeWithText(lancha.nome).assertIsDisplayed()
    }

    /**
     * Tipo e dono na mesma linha. O `·` não é enfeite: é o que distingue duas embarcações de nome parecido
     * numa frota, e é por ele que se procura na lista.
     */
    @Test
    fun pesquisa_cadaCardMostraOTipoAoLadoDaEmpresa() {
        montar()

        composeTestRule.onNodeWithText("${ferry.tipo} · ${ferry.empresaNome}").assertIsDisplayed()
        composeTestRule.onNodeWithText("${lancha.tipo} · ${lancha.empresaNome}").assertIsDisplayed()
    }

    /** Quem filtra é o ViewModel (por id da empresa); a tela só avisa o que foi digitado ou escolhido. */
    @Test
    fun pesquisa_aoDigitarNoFiltro_avisaSemFiltrarSozinha() {
        val escolhidas = mutableListOf<String>()
        montar(onEmpresaChange = { escolhidas += it })

        composeTestRule.onNodeWithText(texto(R.string.label_empresa)).performTextInput("NAV")

        // Um evento, não três: o campo é controlado pelo `uiState`, que neste teste não muda.
        assertEquals(listOf("NAV"), escolhidas)
        // E a lista continua inteira — filtrar não é atribuição da tela.
        composeTestRule.onNodeWithText(lancha.nome).assertIsDisplayed()
    }

    @Test
    fun pesquisa_semResultados_naoMostraCard() {
        montar(uiState = PesquisaEmbarcacaoUiState(empresa = "ZZZ", resultados = emptyList()))

        composeTestRule.onNodeWithText(ferry.nome).assertDoesNotExist()
        composeTestRule.onNodeWithText(texto(R.string.subtitle_pesquisar_embarcacoes)).assertIsDisplayed()
    }

    // --- Editar ---

    @Test
    fun pesquisa_aoTocarEmEditar_levaOIdDaLinhaTocada() {
        val editados = mutableListOf<String>()
        montar(onNavegaParaEditor = { editados += it })

        icone(R.string.description_editar, naLinhaDe = lancha).performClick()

        assertEquals(listOf(lancha.id), editados)
    }

    // --- Apagar ---

    /** Exclusão é irreversível: o toque abre a confirmação e **não** apaga. */
    @Test
    fun pesquisa_aoTocarEmDeletar_pedeConfirmacaoSemApagar() {
        val apagados = mutableListOf<String>()
        montar(onDeletar = { apagados += it })

        icone(R.string.description_deletar, naLinhaDe = ferry).performClick()

        composeTestRule.onNodeWithText(texto(R.string.msg_confirmar_exclusao)).assertIsDisplayed()
        assertTrue("apagou antes de confirmar", apagados.isEmpty())
    }

    @Test
    fun pesquisa_aoConfirmar_apagaAEmbarcacaoDaLinha() {
        val apagados = mutableListOf<String>()
        montar(onDeletar = { apagados += it })

        icone(R.string.description_deletar, naLinhaDe = ferry).performClick()
        composeTestRule.onNodeWithText(texto(R.string.btn_excluir)).performClick()

        assertEquals(listOf(ferry.id), apagados)
        composeTestRule.onNodeWithText(texto(R.string.msg_confirmar_exclusao)).assertDoesNotExist()
    }

    @Test
    fun pesquisa_aoCancelar_naoApagaEFechaODialogo() {
        val apagados = mutableListOf<String>()
        montar(onDeletar = { apagados += it })

        icone(R.string.description_deletar, naLinhaDe = ferry).performClick()
        composeTestRule.onNodeWithText(texto(R.string.btn_cancelar)).performClick()

        assertTrue("cancelar apagou", apagados.isEmpty())
        composeTestRule.onNodeWithText(texto(R.string.msg_confirmar_exclusao)).assertDoesNotExist()
        composeTestRule.onNodeWithText(ferry.nome).assertIsDisplayed()
    }
}