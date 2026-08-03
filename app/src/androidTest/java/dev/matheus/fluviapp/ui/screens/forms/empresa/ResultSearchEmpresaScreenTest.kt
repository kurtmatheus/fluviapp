package dev.matheus.fluviapp.ui.screens.forms.empresa

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.viagem.Empresa
import dev.matheus.fluviapp.ui.states.PesquisaEmpresaUiState
import dev.matheus.fluviapp.ui.theme.FluviAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * A outra metade do CRUD da Empresa em tela: **ler, editar e apagar**. O cadastro é coberto pelo
 * `FormEmpresaScreenTest`; aqui fica o que a lista promete — mostrar o que veio, filtrar pelo que se
 * digita, levar ao editor e não apagar nada sem confirmação.
 *
 * Mesmo molde: `ComponentActivity` vazia, estado fabricado, sem Hilt e sem Firestore. O que se afirma é o
 * contrato da tela — dado este estado, isto aparece; dado este toque, isto é avisado —, que é o que
 * sobrou para testar depois da decisão de tornar o repositório observável em vez de testável.
 */
class ResultSearchEmpresaScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun texto(id: Int) = composeTestRule.activity.getString(id)

    private val modelo = Empresa(
        id = "1",
        nome = "NAVEGA MODELO",
        razaoSocial = "Navega Modelo LTDA",
        cnpj = "00.000.000/0001-00",
        endereco = "Cais 1",
        telefone1 = "9999-0001",
        telefone2 = "",
    )
    private val ilha = modelo.copy(
        id = "2",
        nome = "TRANSPORTE ILHA",
        razaoSocial = "Transporte Ilha SA",
        cnpj = "11.111.111/0001-11",
    )

    /** A ordem dos cards na tela — é dela que sai o índice do ícone de cada linha. */
    private val resultados = listOf(modelo, ilha)

    private fun montar(
        uiState: PesquisaEmpresaUiState = PesquisaEmpresaUiState(resultados = resultados),
        onNomeChange: (String) -> Unit = {},
        onNavegaParaEditor: (String) -> Unit = {},
        onDeletar: (String) -> Unit = {},
    ) {
        composeTestRule.setContent {
            FluviAppTheme {
                ResultSearchEmpresaScreen(
                    uiState = uiState,
                    onNomeChange = onNomeChange,
                    onNavegaParaEditor = onNavegaParaEditor,
                    onDeletar = onDeletar,
                )
            }
        }
    }

    /**
     * O ícone de uma linha específica.
     *
     * Casar o ícone com o nome do card **não** funciona, e o motivo é de acessibilidade antes de ser de
     * teste: o `Row` do `CardResultEmpresa` não emite semântica, então na árvore os textos e os dois
     * `IconButton` são todos irmãos — não existe "a linha" para ser ancestral de nada, e
     * `hasAnyAncestor(hasText(nome))` não encontra nenhum nó. Some daí que os `contentDescription` são
     * genéricos ("Editar", "Deletar") e se repetem por card: o leitor de tela anuncia dois botões iguais
     * por empresa, sem dizer de qual empresa.
     *
     * Por ora o índice, que sai da mesma ordem que a tela renderiza ([resultados]).
     */
    private fun icone(descricao: Int, naLinhaDe: Empresa): SemanticsNodeInteraction =
        composeTestRule
            .onAllNodes(hasContentDescription(texto(descricao)))[resultados.indexOf(naLinhaDe)]

    // --- Ler ---

    @Test
    fun pesquisa_exibeOsResultadosComRazaoSocialECnpj() {
        montar()

        composeTestRule.onNodeWithText(texto(R.string.subtitle_pesquisar_empresas)).assertIsDisplayed()
        composeTestRule.onNodeWithText(modelo.nome).assertIsDisplayed()
        composeTestRule.onNodeWithText(modelo.razaoSocial).assertIsDisplayed()
        composeTestRule.onNodeWithText(modelo.cnpj).assertIsDisplayed()
        composeTestRule.onNodeWithText(ilha.nome).assertIsDisplayed()
    }

    /**
     * A lista é o que o estado manda, não o que a tela decide: quem filtra é o ViewModel (`startsWith`,
     * ignore case), e a tela só avisa o que foi digitado.
     */
    @Test
    fun pesquisa_aoDigitar_avisaOFiltroSemFiltrarSozinha() {
        val digitados = mutableListOf<String>()
        montar(onNomeChange = { digitados += it })

        composeTestRule.onNodeWithText(texto(R.string.label_nome_empresa)).performTextInput("nav")

        // Um evento, não três: o campo é controlado pelo `uiState`, que neste teste não muda — então cada
        // digitação parte do mesmo valor. Quem acumula é o ViewModel.
        assertEquals(listOf("nav"), digitados)
        composeTestRule.onNodeWithText(ilha.nome).assertIsDisplayed()
    }

    @Test
    fun pesquisa_semResultados_naoMostraCard() {
        montar(uiState = PesquisaEmpresaUiState(nome = "zzz", resultados = emptyList()))

        composeTestRule.onNodeWithText(modelo.nome).assertDoesNotExist()
        composeTestRule.onNodeWithText(texto(R.string.subtitle_pesquisar_empresas)).assertIsDisplayed()
    }

    // --- Editar ---

    @Test
    fun pesquisa_aoTocarEmEditar_levaOIdDaLinhaTocada() {
        val editados = mutableListOf<String>()
        montar(onNavegaParaEditor = { editados += it })

        icone(R.string.description_editar, naLinhaDe = ilha).performClick()

        assertEquals(listOf(ilha.id), editados)
    }

    // --- Apagar ---

    /** Exclusão é irreversível: o toque abre a confirmação e **não** apaga. */
    @Test
    fun pesquisa_aoTocarEmDeletar_pedeConfirmacaoSemApagar() {
        val apagados = mutableListOf<String>()
        montar(onDeletar = { apagados += it })

        icone(R.string.description_deletar, naLinhaDe = modelo).performClick()

        composeTestRule.onNodeWithText(texto(R.string.msg_confirmar_exclusao)).assertIsDisplayed()
        assertTrue("apagou antes de confirmar", apagados.isEmpty())
    }

    @Test
    fun pesquisa_aoConfirmar_apagaAEmpresaDaLinha() {
        val apagados = mutableListOf<String>()
        montar(onDeletar = { apagados += it })

        icone(R.string.description_deletar, naLinhaDe = modelo).performClick()
        composeTestRule.onNodeWithText(texto(R.string.btn_excluir)).performClick()

        assertEquals(listOf(modelo.id), apagados)
        composeTestRule.onNodeWithText(texto(R.string.msg_confirmar_exclusao)).assertDoesNotExist()
    }

    @Test
    fun pesquisa_aoCancelar_naoApagaEFechaODialogo() {
        val apagados = mutableListOf<String>()
        montar(onDeletar = { apagados += it })

        icone(R.string.description_deletar, naLinhaDe = modelo).performClick()
        composeTestRule.onNodeWithText(texto(R.string.btn_cancelar)).performClick()

        assertTrue("cancelar apagou", apagados.isEmpty())
        composeTestRule.onNodeWithText(texto(R.string.msg_confirmar_exclusao)).assertDoesNotExist()
        composeTestRule.onNodeWithText(modelo.nome).assertIsDisplayed()
    }
}