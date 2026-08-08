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
 * O painel da revitalização, em tela: **existem a Empresa, a Flotilha e as Localidades**, e mais nada.
 *
 * O menu não é fabricado à mão aqui — vem de `secoesDoMenu(ADM)` e `acoesPorSecao`, as mesmas funções de
 * domínio que a Main Screen usa em produção. É o que faz este teste valer como emenda entre as camadas:
 * se alguém revitalizar uma seção no domínio, é aqui que a tela é cobrada; e se o recorte vazar, é aqui
 * que aparece — sem Firestore, sem sessão e sem login.
 *
 * A emenda cobrou: quando a Flotilha entrou em `SECOES_REVITALIZADAS`, este teste ficou **vermelho**
 * afirmando que ela não deveria aparecer. Foi o combinado funcionando — a lista viva do domínio e a tela
 * discordaram em voz alta, em vez de a seção nova entrar sem ninguém perceber.
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
        composeTestRule.onNodeWithText(texto(SecaoMenu.EMBARCACAO.titulo)).assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(SecaoMenu.LOCALIDADE.titulo)).assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(SecaoMenu.PORTO.titulo)).assertIsDisplayed()
        // A seção da plataforma que faltava (F6.6): quem acessa o app e com que papel.
        composeTestRule.onNodeWithText(texto(SecaoMenu.USUARIOS.titulo)).assertIsDisplayed()

        composeTestRule.onNodeWithText(texto(SecaoMenu.VIAGEM.titulo)).assertDoesNotExist()
        composeTestRule.onNodeWithText(texto(SecaoMenu.PASSAGEM.titulo)).assertDoesNotExist()
        // **A Equipe é da empresa** (F6.6): o `ADM` não abre o quadro de pessoal de ninguém.
        composeTestRule.onNodeWithText(texto(SecaoMenu.EQUIPE.titulo)).assertDoesNotExist()
    }

    /**
     * A seção abre e navega — para **cada** seção viva.
     *
     * As ações esperadas não são digitadas aqui: vêm de `AcaoMenu.de(secao)`, a mesma função que o menu
     * usa. É o que faz a cobrança acompanhar o domínio sozinha — acrescentar uma ação a uma seção passa a
     * ser exigida em tela sem que este arquivo mude, que é o oposto de uma lista repetida por seção.
     */
    private fun expandeENavega(secao: SecaoMenu, acaoTocada: AcaoMenu) {
        val navegadas = mutableListOf<AcaoMenu>()
        montarMenu(onNavegar = { navegadas += it })

        composeTestRule.onNodeWithText(texto(secao.titulo)).performClick()

        AcaoMenu.de(secao).forEach {
            composeTestRule.onNodeWithText(texto(it.titulo)).assertIsDisplayed()
        }

        composeTestRule.onNodeWithText(texto(acaoTocada.titulo)).performClick()

        assertEquals(listOf(acaoTocada), navegadas)
    }

    /** O corte não deixou a Empresa inacessível junto com o resto. */
    @Test
    fun menu_expandeEmpresa_eNavegaPelaAcao() =
        expandeENavega(SecaoMenu.EMPRESA, AcaoMenu.EMPRESA_NOVA)

    @Test
    fun menu_expandeFlotilha_eNavegaPelaAcao() =
        expandeENavega(SecaoMenu.EMBARCACAO, AcaoMenu.EMBARCACAO_PESQUISAR)

    @Test
    fun menu_expandeLocalidades_eNavegaPelaAcao() =
        expandeENavega(SecaoMenu.LOCALIDADE, AcaoMenu.LOCALIDADE_NOVA)

    @Test
    fun menu_expandePortos_eNavegaPelaAcao() =
        expandeENavega(SecaoMenu.PORTO, AcaoMenu.PORTO_PESQUISAR)

    @Test
    fun menu_expandeUsuarios_eNavegaPelaAcao() =
        expandeENavega(SecaoMenu.USUARIOS, AcaoMenu.USUARIO_NOVO)
}