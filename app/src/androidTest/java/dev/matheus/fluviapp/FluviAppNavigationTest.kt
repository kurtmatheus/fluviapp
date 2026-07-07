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
import org.junit.Rule
import org.junit.Test

private const val TAG = "fluvi app"

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