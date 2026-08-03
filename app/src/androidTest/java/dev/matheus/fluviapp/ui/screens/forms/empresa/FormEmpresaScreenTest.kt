package dev.matheus.fluviapp.ui.screens.forms.empresa

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.operacoes.Atuacao
import dev.matheus.fluviapp.ui.states.FormEmpresaUiState
import dev.matheus.fluviapp.ui.theme.FluviAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Teste de tela **mínimo**: a tela é montada direto, com um [FormEmpresaUiState] de mentira, numa
 * `ComponentActivity` vazia.
 *
 * O que fica de fora é o ponto: não há `@HiltAndroidTest` (que arrastaria Firestore, Auth e DataStore
 * para dentro do teste), não há `MainActivity`, não há login e não há navegação. O que se valida aqui é o
 * contrato da camada de apresentação — *dado este estado, isto aparece; dado este toque, isto é avisado* —
 * e é justamente o que o `@Preview` mostra sem conseguir afirmar.
 *
 * Vale porque `FormEmpresaScreen` é pura (recebe estado + lambdas). Toda tela que seguir o molde do
 * ADR-0006 é testável assim; a que precisar de Hilt para renderizar já denuncia um acoplamento.
 */
class FormEmpresaScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun texto(id: Int) = composeTestRule.activity.getString(id)

    private fun montarTela(
        uiState: FormEmpresaUiState = FormEmpresaUiState(),
        onAtuacaoToggle: (Atuacao) -> Unit = {},
    ) {
        composeTestRule.setContent {
            FluviAppTheme {
                FormEmpresaScreen(uiState = uiState, onAtuacaoToggle = onAtuacaoToggle)
            }
        }
    }

    /**
     * A linha de uma atuação, encontrada pelo próprio rótulo.
     *
     * Isto só funciona porque o `Row` de `AreaAtuacoes` é `toggleable`: ele funde caixa e texto num nó único
     * que tem, ao mesmo tempo, o rótulo e o estado. Antes disso os `Checkbox` eram nós anônimos e a única
     * forma de escolher um era pela posição — foi este teste que expôs o problema, que é de acessibilidade
     * antes de ser de teste.
     */
    private fun linhaDa(atuacao: Atuacao): SemanticsNodeInteraction =
        composeTestRule.onNodeWithText(atuacao.rotulo)

    @Test
    fun formEmpresa_estadoInicial_exibeFormularioVazio() {
        montarTela()

        composeTestRule.onNodeWithText(texto(R.string.subtitle_cadastrar_nova_empresa)).assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(R.string.label_nome_empresa)).assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(R.string.label_atuacoes)).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(R.string.btn_salvar)).performScrollTo().assertIsDisplayed()
    }

    /** ADR-0016 §5: a atuação dormente aparece desabilitada em vez de sumir. */
    @Test
    fun formEmpresa_atuacaoDormente_apareceDesabilitada() {
        montarTela()

        linhaDa(Atuacao.PORTUARIA_OPERACAO).performScrollTo().assertIsNotEnabled()
    }

    /** O toque na caixa avisa *qual* atuação — o estado é do ViewModel, não da tela. */
    @Test
    fun formEmpresa_aoTocarNaAtuacao_avisaQual() {
        val avisadas = mutableListOf<Atuacao>()
        montarTela(onAtuacaoToggle = { avisadas += it })

        linhaDa(Atuacao.TRANSPORTE).performScrollTo().performClick()

        assertEquals(listOf(Atuacao.TRANSPORTE), avisadas)
    }

    /** O erro de atuação é do estado, não de uma exceção: entra pelo `uiState` e sai como texto. */
    @Test
    fun formEmpresa_comErroDeAtuacao_exibeMensagem() {
        montarTela(FormEmpresaUiState(isAtuacoesError = true))

        composeTestRule.onNodeWithText(texto(R.string.error_selecione_opcao)).performScrollTo().assertIsDisplayed()
    }
}