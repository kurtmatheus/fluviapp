package dev.matheus.fluviapp.ui.screens.forms.porto

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.states.ErroNomePorto
import dev.matheus.fluviapp.ui.states.FormPortoUiState
import dev.matheus.fluviapp.ui.states.LocalidadeOpcao
import dev.matheus.fluviapp.ui.theme.FluviAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * O cadastro de porto em tela. Além do molde de sempre (estado fabricado, sem Hilt), aqui há duas
 * promessas que só a renderização cumpre: **a localidade se escolhe, não se digita**, e o campo do nome
 * sabe dizer *qual* dos dois erros é o dele.
 */
class FormPortoScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun texto(id: Int) = composeTestRule.activity.getString(id)

    private val opcoes = listOf(
        LocalidadeOpcao("belem", "Belém/PA"),
        LocalidadeOpcao("parintins", "Parintins/AM"),
    )

    private fun montarTela(
        uiState: FormPortoUiState = FormPortoUiState(localidades = opcoes),
        onNomeChange: (String) -> Unit = {},
        onLocalidadeChange: (String) -> Unit = {},
        onClickSalvar: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            FluviAppTheme {
                FormPortoScreen(
                    uiState = uiState,
                    onNomeChange = onNomeChange,
                    onLocalidadeChange = onLocalidadeChange,
                    onClickSalvar = onClickSalvar,
                )
            }
        }
    }

    @Test
    fun formPorto_estadoInicial_exibeOsDoisCampos() {
        montarTela()

        composeTestRule.onNodeWithText(texto(R.string.subtitle_cadastrar_novo_porto)).assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(R.string.label_nome_porto)).assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(R.string.label_localidade)).assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(R.string.btn_salvar)).performScrollTo().assertIsEnabled()
    }

    @Test
    fun formPorto_aoDigitarONome_avisaSemDecidirNada() {
        val digitados = mutableListOf<String>()
        montarTela(onNomeChange = { digitados += it })

        composeTestRule.onNodeWithText(texto(R.string.label_nome_porto)).performTextInput("Porto Central")

        assertEquals(listOf("Porto Central"), digitados)
    }

    /** A localidade escolhida aparece pelo **rótulo**, resolvido do id — a tela não vê o id em momento algum. */
    @Test
    fun formPorto_comLocalidadeEscolhida_exibeORotulo() {
        montarTela(FormPortoUiState(localidadeId = "belem", localidades = opcoes))

        composeTestRule.onNodeWithText("Belém/PA").assertIsDisplayed()
    }

    /** Lista fechada: o dropdown oferece o que existe, e devolver o rótulo escolhido é tudo o que a tela faz. */
    @Test
    fun formPorto_aoAbrirODropdown_ofereceAsLocalidadesEDevolveORotulo() {
        val escolhidas = mutableListOf<String>()
        montarTela(onLocalidadeChange = { escolhidas += it })

        composeTestRule.onNodeWithText(texto(R.string.label_localidade)).performClick()
        composeTestRule.onNodeWithText("Parintins/AM").performClick()

        assertEquals(listOf("Parintins/AM"), escolhidas)
    }

    @Test
    fun formPorto_semNome_cobraOCampoObrigatorio() {
        montarTela(FormPortoUiState(erroNome = ErroNomePorto.OBRIGATORIO, localidades = opcoes))

        composeTestRule.onNodeWithText(texto(R.string.error_camp_obrig)).assertIsDisplayed()
    }

    /**
     * **As duas queixas do mesmo campo são diferentes.** "Campo obrigatório" para um nome duplicado
     * mandaria preencher o que acabou de ser preenchido; a mensagem certa diz onde está o conflito.
     */
    @Test
    fun formPorto_nomeDuplicado_dizQueJaExisteNestaLocalidade() {
        montarTela(
            FormPortoUiState(
                nome = "Porto Central",
                erroNome = ErroNomePorto.DUPLICADO,
                localidadeId = "belem",
                localidades = opcoes,
            )
        )

        composeTestRule.onNodeWithText(texto(R.string.error_porto_duplicado)).assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(R.string.error_camp_obrig)).assertDoesNotExist()
    }

    @Test
    fun formPorto_aoTocarEmSalvar_avisa() {
        var salvamentos = 0
        montarTela(onClickSalvar = { salvamentos++ })

        composeTestRule.onNodeWithText(texto(R.string.btn_salvar)).performScrollTo().performClick()

        assertEquals(1, salvamentos)
    }
}
