package dev.matheus.fluviapp.ui.screens.forms.localidade

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.localidade.Uf
import dev.matheus.fluviapp.ui.states.BuscaIbge
import dev.matheus.fluviapp.ui.states.FormLocalidadeUiState
import dev.matheus.fluviapp.ui.theme.FluviAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * O cadastro de localidade em tela. Além do molde de sempre (estado fabricado, sem Hilt), aqui há uma
 * promessa que só a renderização cumpre: **a consulta ao IBGE é ajuda, não porteiro**. Os casos abaixo
 * fixam as três coisas que isso significa em pixels — o botão só se oferece quando faz sentido, o
 * insucesso é dito com a palavra certa, e nada disso impede o resto do formulário de existir.
 */
class FormLocalidadeScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun texto(id: Int) = composeTestRule.activity.getString(id)

    private fun montarTela(
        uiState: FormLocalidadeUiState = FormLocalidadeUiState(),
        onConsultarIbge: () -> Unit = {},
        onClickSalvar: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            FluviAppTheme {
                FormLocalidadeScreen(
                    uiState = uiState,
                    onConsultarIbge = onConsultarIbge,
                    onClickSalvar = onClickSalvar,
                )
            }
        }
    }

    @Test
    fun formLocalidade_estadoInicial_exibeOsTresCampos() {
        montarTela()

        composeTestRule.onNodeWithText(texto(R.string.subtitle_cadastrar_nova_localidade)).assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(R.string.label_codigo_ibge)).assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(R.string.label_municipio)).assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(R.string.label_uf)).assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(R.string.btn_salvar)).performScrollTo().assertIsDisplayed()
    }

    /** Consultar meio código gastaria rede para receber "não encontrado" e assustar quem está digitando. */
    @Test
    fun formLocalidade_comCodigoIncompleto_naoOfereceABusca() {
        montarTela(FormLocalidadeUiState(codigoIbge = "150"))

        composeTestRule.onNodeWithText(texto(R.string.btn_buscar_no_ibge)).assertIsNotEnabled()
    }

    @Test
    fun formLocalidade_comCodigoCompleto_ofereceABusca() {
        montarTela(FormLocalidadeUiState(codigoIbge = "1501402"))

        composeTestRule.onNodeWithText(texto(R.string.btn_buscar_no_ibge)).assertIsEnabled()
    }

    @Test
    fun formLocalidade_aoTocarEmBuscar_avisa() {
        var consultas = 0
        montarTela(FormLocalidadeUiState(codigoIbge = "1501402"), onConsultarIbge = { consultas++ })

        composeTestRule.onNodeWithText(texto(R.string.btn_buscar_no_ibge)).performClick()

        assertEquals(1, consultas)
    }

    /** Enquanto consulta, o botão não aceita um segundo toque — e a tela diz que está esperando. */
    @Test
    fun formLocalidade_consultando_avisaEBloqueiaONovoToque() {
        montarTela(FormLocalidadeUiState(codigoIbge = "1501402", buscaIbge = BuscaIbge.Consultando))

        composeTestRule.onNodeWithText(texto(R.string.msg_consultando_ibge)).assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(R.string.btn_buscar_no_ibge)).assertIsNotEnabled()
    }

    /**
     * As duas mensagens de insucesso são diferentes porque falam de coisas diferentes: uma acusa o
     * código, a outra a rede. Trocá-las seria culpar quem digitou por um problema de conexão.
     */
    @Test
    fun formLocalidade_naoEncontrado_falaDoCodigo() {
        montarTela(FormLocalidadeUiState(codigoIbge = "1501402", buscaIbge = BuscaIbge.NaoEncontrado))

        composeTestRule.onNodeWithText(texto(R.string.msg_ibge_nao_encontrado)).assertIsDisplayed()
    }

    @Test
    fun formLocalidade_indisponivel_mandaPreencherAMao() {
        montarTela(FormLocalidadeUiState(codigoIbge = "1501402", buscaIbge = BuscaIbge.Indisponivel))

        composeTestRule.onNodeWithText(texto(R.string.msg_ibge_indisponivel)).assertIsDisplayed()
        // E o formulário continua inteiro: salvar não depende do IBGE.
        composeTestRule.onNodeWithText(texto(R.string.btn_salvar)).performScrollTo().assertIsEnabled()
    }

    /** Preenchido pela consulta ou à mão, o que a tela mostra é o rótulo da UF, não a sigla crua. */
    @Test
    fun formLocalidade_comUfEscolhida_exibeORotulo() {
        montarTela(FormLocalidadeUiState(municipio = "Belém", uf = Uf.PA))

        composeTestRule.onNodeWithText(Uf.PA.rotulo()).assertIsDisplayed()
        composeTestRule.onNodeWithText("Belém").assertIsDisplayed()
    }

    /** O erro do código é do estado, e a mensagem explica a regra que não precisa de rede. */
    @Test
    fun formLocalidade_comErroDeCodigo_exibeAMensagem() {
        montarTela(FormLocalidadeUiState(isCodigoIbgeError = true))

        composeTestRule.onNodeWithText(texto(R.string.error_codigo_ibge_invalido)).assertIsDisplayed()
    }
}