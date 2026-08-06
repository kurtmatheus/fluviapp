package dev.matheus.fluviapp.ui.screens.forms.porto

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.states.PesquisaPortoUiState
import dev.matheus.fluviapp.ui.states.PortoResultado
import dev.matheus.fluviapp.ui.theme.FluviAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * A busca de portos. O que ela tem de próprio em relação às outras listas é a **segunda linha**: o
 * lugar, que veio de outra coleção e que é o que distingue homônimos.
 */
class ResultSearchPortoScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun texto(id: Int) = composeTestRule.activity.getString(id)

    private val valDeCaes = PortoResultado("1", "Porto de Val-de-Cães", "Belém/PA")
    private val parintins = PortoResultado("2", "Porto de Parintins", "Parintins/AM")

    /** A ordem dos cards — é dela que sai o índice do ícone de cada linha. */
    private val resultados = listOf(valDeCaes, parintins)

    private fun montar(
        uiState: PesquisaPortoUiState = PesquisaPortoUiState(resultados = resultados),
        onNomeChange: (String) -> Unit = {},
        onNavegaParaEditor: (String) -> Unit = {},
        onDeletar: (String) -> Unit = {},
    ) {
        composeTestRule.setContent {
            FluviAppTheme {
                ResultSearchPortoScreen(
                    uiState = uiState,
                    onNomeChange = onNomeChange,
                    onNavegaParaEditor = onNavegaParaEditor,
                    onDeletar = onDeletar,
                )
            }
        }
    }

    /** Por índice, pela mesma limitação de semântica anotada nas outras buscas. */
    private fun icone(descricao: Int, naLinhaDe: PortoResultado): SemanticsNodeInteraction =
        composeTestRule
            .onAllNodes(hasContentDescription(texto(descricao)))[resultados.indexOf(naLinhaDe)]

    @Test
    fun pesquisa_exibeNomeELocalidade() {
        montar()

        composeTestRule.onNodeWithText(texto(R.string.subtitle_pesquisar_portos)).assertIsDisplayed()
        composeTestRule.onNodeWithText(valDeCaes.nome).assertIsDisplayed()
        // A localidade é o que resolve homônimo — e chegou aqui pela junção, não por cópia no documento.
        composeTestRule.onNodeWithText(valDeCaes.rotuloLocalidade).assertIsDisplayed()
        composeTestRule.onNodeWithText(parintins.nome).assertIsDisplayed()
    }

    /** Referência que não resolveu: a linha existe **sem lugar**, e não com um lugar inventado. */
    @Test
    fun pesquisa_semLocalidadeResolvida_mostraSoONome() {
        val orfao = PortoResultado("9", "Porto Órfão", "")
        montar(uiState = PesquisaPortoUiState(resultados = listOf(orfao)))

        composeTestRule.onNodeWithText(orfao.nome).assertIsDisplayed()
    }

    @Test
    fun pesquisa_aoDigitar_avisaSemFiltrarSozinha() {
        val digitados = mutableListOf<String>()
        montar(onNomeChange = { digitados += it })

        composeTestRule.onNodeWithText(texto(R.string.label_nome_porto)).performTextInput("val")

        assertEquals(listOf("val"), digitados)
        composeTestRule.onNodeWithText(parintins.nome).assertIsDisplayed()
    }

    @Test
    fun pesquisa_semResultados_naoMostraCard() {
        montar(uiState = PesquisaPortoUiState(nome = "zzz"))

        composeTestRule.onNodeWithText(valDeCaes.nome).assertDoesNotExist()
    }

    @Test
    fun pesquisa_aoTocarEmEditar_levaOIdDaLinhaTocada() {
        val editados = mutableListOf<String>()
        montar(onNavegaParaEditor = { editados += it })

        icone(R.string.description_editar, naLinhaDe = parintins).performClick()

        assertEquals(listOf(parintins.id), editados)
    }

    @Test
    fun pesquisa_aoTocarEmExcluir_pedeConfirmacaoSemExcluir() {
        val excluidos = mutableListOf<String>()
        montar(onDeletar = { excluidos += it })

        icone(R.string.description_deletar, naLinhaDe = valDeCaes).performClick()

        composeTestRule.onNodeWithText(texto(R.string.msg_confirmar_exclusao)).assertIsDisplayed()
        assertTrue("excluiu antes de confirmar", excluidos.isEmpty())
    }

    @Test
    fun pesquisa_aoConfirmar_excluiOPortoDaLinha() {
        val excluidos = mutableListOf<String>()
        montar(onDeletar = { excluidos += it })

        icone(R.string.description_deletar, naLinhaDe = valDeCaes).performClick()
        composeTestRule.onNodeWithText(texto(R.string.btn_excluir)).performClick()

        assertEquals(listOf(valDeCaes.id), excluidos)
        composeTestRule.onNodeWithText(texto(R.string.msg_confirmar_exclusao)).assertDoesNotExist()
    }

    @Test
    fun pesquisa_aoCancelar_naoExcluiEFechaODialogo() {
        val excluidos = mutableListOf<String>()
        montar(onDeletar = { excluidos += it })

        icone(R.string.description_deletar, naLinhaDe = valDeCaes).performClick()
        composeTestRule.onNodeWithText(texto(R.string.btn_cancelar)).performClick()

        assertTrue("cancelar excluiu", excluidos.isEmpty())
        composeTestRule.onNodeWithText(valDeCaes.nome).assertIsDisplayed()
    }
}