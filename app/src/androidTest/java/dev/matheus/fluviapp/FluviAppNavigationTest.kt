package dev.matheus.fluviapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToLog
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

private const val TAG = "fluvi app"

/**
 * **Suspenso pela revitalização (ADR-0020).** Este é um teste ponta a ponta do app inteiro: sobe a
 * `MainActivity` com o grafo real do Hilt, entra pelo login e navega até "Nova Viagem" — dois domínios
 * (sessão e viagem) que ainda não foram refeitos, e um caminho de login que deixou de existir como está
 * desde que o provisionamento fechou (ADR-0015 §2.1: só e-mail e senha pré-cadastrados).
 *
 * É o `@Category(ForaDoEscopo)` da suíte instrumentada: aqui não há categoria para excluir, então o
 * marcador é o próprio `@Ignore` — **com a razão escrita**, porque um `@Ignore` sem motivo é indistinguível
 * de um teste abandonado.
 *
 * **Revitalizar a Flotilha não o traz de volta**, e é por isso que ele continua ignorado depois dela: a
 * jornada que ele descreve não passa por embarcação nenhuma — passa por login e por viagem. Ele volta
 * quando **essas** seções entrarem em `SECOES_REVITALIZADAS`, e voltará reescrito, porque a tela de login
 * que ele conhece já não é a que existe. Enquanto isso, quem cobre a apresentação são os testes de tela —
 * sem Hilt, sem Activity e sem login.
 */
@Ignore("Revitalização ADR-0020: vivas a Empresa e a Flotilha; este cobre login e viagem, que voltam com as seções delas.")
@HiltAndroidTest
class FluviAppNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule(MainActivity::class.java)

    @Before
    fun setupAppNavHost() {
        hiltRule.inject()
    }

    @Test
    fun appNavHost_verifyLoginScreen() {
        composeTestRule.onRoot().printToLog(TAG)

        composeTestRule
            .onNodeWithText("Entrar")
            .assertIsDisplayed()
    }

    @Test
    fun appNavHost_verifyFormNovaViagemScreen() {
        composeTestRule.onRoot().printToLog(TAG)

        composeTestRule
            .onNodeWithText("Entrar")
            .performClick()

        composeTestRule
            .onNodeWithText("Próximas Viagens")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Viagens")
            .performClick()

        composeTestRule
            .onNodeWithText("Menu Viagens")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Nova Viagem")
            .performClick()

        composeTestRule
            .onNodeWithText("Cadastrar Nova Viagem")
            .assertIsDisplayed()

    }
}