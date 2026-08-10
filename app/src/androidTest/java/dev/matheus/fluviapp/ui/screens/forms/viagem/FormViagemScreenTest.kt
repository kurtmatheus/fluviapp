package dev.matheus.fluviapp.ui.screens.forms.viagem

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.viagem.DIAS_DA_SEMANA
import dev.matheus.fluviapp.domain.viagem.mascararHora
import dev.matheus.fluviapp.domain.viagem.rotulo
import dev.matheus.fluviapp.ui.states.EmbarcacaoOpcao
import dev.matheus.fluviapp.ui.states.FormViagemUiState
import dev.matheus.fluviapp.ui.states.RotaOpcao
import dev.matheus.fluviapp.ui.theme.FluviAppTheme
import org.junit.Rule
import org.junit.Test

/**
 * O formulário de viagem em tela — e o caso que motivou este arquivo existir.
 *
 * **O teclado numérico do Android não tem `:`.** O campo de hora pedia `HH:mm` e oferecia um teclado onde
 * o separador não existe: não era atrito, era um campo que não se conseguia preencher. O achado veio de
 * homologação, e não de teste — nenhuma camada mentia, cada uma estava certa isoladamente. É o tipo de
 * defeito que só aparece com dedo na tela, e por isso a cobertura dele mora aqui.
 */
class FormViagemScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun texto(id: Int) = composeTestRule.activity.getString(id)

    private val rotaOpcao = RotaOpcao("r1", "Porto A · Belém/PA → Porto B · Parintins/AM")

    /**
     * Monta a tela com o **mesmo caminho do ViewModel** para a hora: o `onHoraChange` real aplica
     * `mascararHora`, e replicá-lo aqui é o que faz este teste medir a digitação, e não uma tela que
     * aceita qualquer texto.
     */
    private fun montar() {
        composeTestRule.setContent {
            FluviAppTheme {
                var state by remember {
                    mutableStateOf(
                        FormViagemUiState(
                            rotas = listOf(rotaOpcao),
                            embarcacoes = listOf(EmbarcacaoOpcao("e1", "F/B Modelo")),
                            diasDaSemana = DIAS_DA_SEMANA.map { it.rotulo },
                        )
                    )
                }

                FormViagemScreen(
                    uiState = state,
                    onHoraChange = { state = state.copy(hora = mascararHora(it)) },
                )
            }
        }
    }

    /** Quatro dígitos, e o `:` aparece sozinho — que é o que o teclado numérico permite digitar. */
    @Test
    fun hora_digitadaSoComNumeros_ganhaOSeparador() {
        montar()

        composeTestRule.onNodeWithText(texto(R.string.label_hora_partida)).performTextInput("1830")

        composeTestRule.onNodeWithText("18:30").assertIsDisplayed()
    }

    @Test
    fun hora_incompleta_naoInventaSeparador() {
        montar()

        composeTestRule.onNodeWithText(texto(R.string.label_hora_partida)).performTextInput("18")

        composeTestRule.onNodeWithText("18").assertIsDisplayed()
    }

    /** A tela diz que não há edição — quem procurar por ela precisa saber o que fazer no lugar. */
    @Test
    fun form_avisaQueAViagemNaoSeEdita() {
        montar()

        composeTestRule.onNodeWithText(texto(R.string.msg_viagem_imutavel)).assertIsDisplayed()
    }

    /** Sem concessão não há o que oferecer, e a tela manda ao lugar certo em vez de mostrar vazio. */
    @Test
    fun semConcessao_mostraORecadoEEscondeOsCampos() {
        composeTestRule.setContent {
            FluviAppTheme { FormViagemScreen(uiState = FormViagemUiState(semConcessao = true)) }
        }

        composeTestRule.onNodeWithText(texto(R.string.msg_viagem_sem_concessao)).assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(R.string.label_hora_partida)).assertDoesNotExist()
    }
}
