package dev.matheus.fluviapp.ui.screens.forms.localidade

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.states.LocalidadeResultado
import dev.matheus.fluviapp.ui.states.PesquisaLocalidadeUiState
import dev.matheus.fluviapp.ui.theme.FluviAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * A busca de localidades. O gesto de excluir é o mesmo das outras seções — o que muda é o que acontece
 * depois dele, e isso é do repositório: aqui só se afirma que **nada some sem confirmação**.
 */
class ResultSearchLocalidadeScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun texto(id: Int) = composeTestRule.activity.getString(id)

    private val belem = LocalidadeResultado("1", "Belém/PA", "1501402")
    private val parintins = LocalidadeResultado("2", "Parintins/AM", "1303205")

    /** A ordem dos cards — é dela que sai o índice do ícone de cada linha. */
    private val resultados = listOf(belem, parintins)

    private fun montar(
        uiState: PesquisaLocalidadeUiState = PesquisaLocalidadeUiState(resultados = resultados),
        onMunicipioChange: (String) -> Unit = {},
        onNavegaParaEditor: (String) -> Unit = {},
        onDeletar: (String) -> Unit = {},
    ) {
        composeTestRule.setContent {
            FluviAppTheme {
                ResultSearchLocalidadeScreen(
                    uiState = uiState,
                    onMunicipioChange = onMunicipioChange,
                    onNavegaParaEditor = onNavegaParaEditor,
                    onDeletar = onDeletar,
                )
            }
        }
    }

    /** Por índice, pela mesma limitação de semântica anotada nas buscas de Empresa e Flotilha. */
    private fun icone(descricao: Int, naLinhaDe: LocalidadeResultado): SemanticsNodeInteraction =
        composeTestRule
            .onAllNodes(hasContentDescription(texto(descricao)))[resultados.indexOf(naLinhaDe)]

    @Test
    fun pesquisa_exibeRotuloECodigo() {
        montar()

        composeTestRule.onNodeWithText(texto(R.string.subtitle_pesquisar_localidades)).assertIsDisplayed()
        composeTestRule.onNodeWithText(belem.rotulo).assertIsDisplayed()
        // O código é a chave natural: é por ele que se distingue duas homônimas de estados diferentes.
        composeTestRule.onNodeWithText(belem.codigoIbge).assertIsDisplayed()
        composeTestRule.onNodeWithText(parintins.rotulo).assertIsDisplayed()
    }

    @Test
    fun pesquisa_aoDigitar_avisaSemFiltrarSozinha() {
        val digitados = mutableListOf<String>()
        montar(onMunicipioChange = { digitados += it })

        composeTestRule.onNodeWithText(texto(R.string.label_municipio)).performTextInput("bel")

        assertEquals(listOf("bel"), digitados)
        composeTestRule.onNodeWithText(parintins.rotulo).assertIsDisplayed()
    }

    @Test
    fun pesquisa_semResultados_naoMostraCard() {
        montar(uiState = PesquisaLocalidadeUiState(municipio = "zzz"))

        composeTestRule.onNodeWithText(belem.rotulo).assertDoesNotExist()
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
        val excluidas = mutableListOf<String>()
        montar(onDeletar = { excluidas += it })

        icone(R.string.description_deletar, naLinhaDe = belem).performClick()

        composeTestRule.onNodeWithText(texto(R.string.msg_confirmar_exclusao)).assertIsDisplayed()
        assertTrue("excluiu antes de confirmar", excluidas.isEmpty())
    }

    @Test
    fun pesquisa_aoConfirmar_excluiALocalidadeDaLinha() {
        val excluidas = mutableListOf<String>()
        montar(onDeletar = { excluidas += it })

        icone(R.string.description_deletar, naLinhaDe = belem).performClick()
        composeTestRule.onNodeWithText(texto(R.string.btn_excluir)).performClick()

        assertEquals(listOf(belem.id), excluidas)
        composeTestRule.onNodeWithText(texto(R.string.msg_confirmar_exclusao)).assertDoesNotExist()
    }

    @Test
    fun pesquisa_aoCancelar_naoExcluiEFechaODialogo() {
        val excluidas = mutableListOf<String>()
        montar(onDeletar = { excluidas += it })

        icone(R.string.description_deletar, naLinhaDe = belem).performClick()
        composeTestRule.onNodeWithText(texto(R.string.btn_cancelar)).performClick()

        assertTrue("cancelar excluiu", excluidas.isEmpty())
        composeTestRule.onNodeWithText(belem.rotulo).assertIsDisplayed()
    }
}