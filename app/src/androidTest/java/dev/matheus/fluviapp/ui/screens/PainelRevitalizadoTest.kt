
package dev.matheus.fluviapp.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.operacoes.Usuario.Papel
import dev.matheus.fluviapp.domain.screendata.AcaoMenu
import dev.matheus.fluviapp.domain.screendata.SecaoMenu
import dev.matheus.fluviapp.domain.screendata.acoesPorSecao
import dev.matheus.fluviapp.domain.screendata.secoesDoMenu
import dev.matheus.fluviapp.ui.components.drawer.FluviMenuDrawer
import dev.matheus.fluviapp.ui.states.MainScreenState
import dev.matheus.fluviapp.ui.states.MainScreenUiState
import dev.matheus.fluviapp.ui.theme.FluviAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * O painel da revitalização, em tela: **só a Empresa existe** (ADR-0020).
 *
 * O menu não é fabricado à mão aqui — vem de `secoesDoMenu(ADM)` e `acoesPorSecao`, as mesmas funções de
 * domínio que a Main Screen usa em produção. É o que faz este teste valer como emenda entre as camadas:
 * se alguém revitalizar uma seção no domínio, é aqui que a tela é cobrada; e se o recorte vazar, é aqui
 * que aparece — sem Firestore, sem sessão e sem login.
 */
class PainelRevitalizadoTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun texto(id: Int) = composeTestRule.activity.getString(id)

    /** O menu de quem mais enxerga no app: se nem para o ADM vazou, não vazou para ninguém. */
    private val secoesDoAdm = secoesDoMenu(Papel.ADM.name)

    private fun montarPainel() {
        composeTestRule.setContent {
            FluviAppTheme {
                MainScreen(
                    state = MainScreenUiState(
                        userName = "Odair",
                        secoesVisiveis = secoesDoAdm,
                        mainScreenState = MainScreenState.HOME,
                    ),
                    acoesPorSecao = acoesPorSecao(secoesDoAdm),
                )
            }
        }
    }

    private fun montarMenu(onNavegar: (AcaoMenu) -> Unit = {}) {
        composeTestRule.setContent {
            FluviAppTheme {
                FluviMenuDrawer(
                    userName = "Odair",
                    secoes = secoesDoAdm,
                    acoesPorSecao = acoesPorSecao(secoesDoAdm),
                    isDarkTheme = false,
                    onInicio = {},
                    onNavegar = onNavegar,
                    onToggleTheme = {},
                    onDeslogar = {},
                )
            }
        }
    }

    // --- O painel ---

    @Test
    fun painel_semSecaoAberta_convidaAAbrirOMenu() {
        montarPainel()

        composeTestRule.onNodeWithText(texto(R.string.msg_painel_sem_secao)).assertIsDisplayed()
    }

    /**
     * O que saiu do painel junto com os domínios que o alimentavam: a lista de próximas viagens e o
     * embarque (que lê o QR de uma passagem). Um app recém-implementado não teria nem um nem outro.
     */
    @Test
    fun painel_naoOfereceOQueNaoFoiRevitalizado() {
        montarPainel()

        composeTestRule.onNodeWithText(texto(R.string.subtitle_viagens_disponiveis)).assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription(texto(R.string.btn_embarque)).assertDoesNotExist()
    }

    // --- O menu ---

    @Test
    fun menu_mostraApenasAsSecoesRevitalizadas() {
        montarMenu()

        composeTestRule.onNodeWithText(texto(SecaoMenu.EMPRESA.titulo)).assertIsDisplayed()

        composeTestRule.onNodeWithText(texto(SecaoMenu.NAVIO.titulo)).assertDoesNotExist()
        composeTestRule.onNodeWithText(texto(SecaoMenu.VIAGEM.titulo)).assertDoesNotExist()
        composeTestRule.onNodeWithText(texto(SecaoMenu.PASSAGEM.titulo)).assertDoesNotExist()
        composeTestRule.onNodeWithText(texto(SecaoMenu.EQUIPE.titulo)).assertDoesNotExist()
    }

    /** A seção viva abre e navega: o corte não deixou a Empresa inacessível junto com o resto. */
    @Test
    fun menu_expandeEmpresa_eNavegaPelaAcao() {
        val navegadas = mutableListOf<AcaoMenu>()
        montarMenu(onNavegar = { navegadas += it })

        composeTestRule.onNodeWithText(texto(SecaoMenu.EMPRESA.titulo)).performClick()

        composeTestRule.onNodeWithText(texto(AcaoMenu.EMPRESA_NOVA.titulo)).assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(AcaoMenu.EMPRESA_PESQUISAR.titulo)).assertIsDisplayed()

        composeTestRule.onNodeWithText(texto(AcaoMenu.EMPRESA_NOVA.titulo)).performClick()

        assertEquals(listOf(AcaoMenu.EMPRESA_NOVA), navegadas)
    }
}