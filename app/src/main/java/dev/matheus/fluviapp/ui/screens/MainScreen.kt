package dev.matheus.fluviapp.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.sampledata.listaBotoesMenuPassagensSample
import dev.matheus.fluviapp.sampledata.listaDadosDadosViagemHomeSampleCards
import dev.matheus.fluviapp.sampledata.listaMenuBotoesCategoriaSample
import dev.matheus.fluviapp.ui.components.contents.HomeContent
import dev.matheus.fluviapp.ui.components.contents.MenuOperacoes
import dev.matheus.fluviapp.ui.components.contents.MenuPassagem
import dev.matheus.fluviapp.ui.components.dialogs.UserDialog
import dev.matheus.fluviapp.ui.states.MainScreenState
import dev.matheus.fluviapp.ui.states.MainScreenUiState

@Composable
fun MainScreen(
    state: MainScreenUiState,
    onClickHome: () -> Unit = {},
    onClickMenuPassagens: () -> Unit = {},
    onClickMenuOperacoes: () -> Unit = {},
    onClickUsername: () -> Unit = {},
    onDismissUserDialog: () -> Unit = {},
    onClickDeslogar: () -> Unit = {},
    onClickAdicionarPassagem: (String) -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    CommonScreen(
        modifier = Modifier,
        titleTopContent = state.title,
        homeActive = state.homeActive,
        passagensActive = state.passagensActive,
        viagensActive = state.operacoesActive,
        isMainTopAppBar = true,
        titleTopAppBar = 0,
        userNameTopAppBar = state.userName,
        isShowBottomAppBar = true,
        isShowMenuViagens = state.isDiretorOuAdm,
        isShowRightIcon = false,
        hasRefresh = true,
        isRefreshing = state.isRefreshing,
        onClickHome = onClickHome,
        onClickMenuPassagens = onClickMenuPassagens,
        onClickMenuViagens = onClickMenuOperacoes,
        onClickUsername = onClickUsername,
        onRefresh = onRefresh,
        content = { modifier, titulo ->
            if (state.exibirUserDialog) {
                UserDialog(
                    modifier = modifier,
                    username = state.userName,
                    onClickDeslogar = onClickDeslogar,
                    onDismiss = onDismissUserDialog
                )
            }
            when (state.mainScreenState) {
                is MainScreenState.LOADING -> {
                    Box(
                        modifier = modifier.fillMaxSize()
                    ) {
                        CircularProgressIndicator(
                            modifier = modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                is MainScreenState.HOME -> {
                    HomeContent(
                        modifier = modifier,
                        titulo = titulo,
                        listaViagens = state.listaViagens,
                        onClickNovaPassagem = onClickAdicionarPassagem
                    )
                }

                is MainScreenState.PASSAGENS -> {
                    MenuPassagem(
                        modifier = modifier,
                        titulo = titulo,
                        listaBotoes = state.mainScreenState.listaBotoesMenus,
                    )
                }

                is MainScreenState.OPERACOES -> {
                    MenuOperacoes(
                        modifier = modifier,
                        listaMenu = state.mainScreenState.listaBotoesMenus
                    )
                }
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
        MainScreen(
            MainScreenUiState(
                userName = "Odair",
                isDiretorOuAdm = true,
                listaViagens = listaDadosDadosViagemHomeSampleCards,
                mainScreenState = MainScreenState.HOME
            )
        )
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPassagensPreview() {
        MainScreen(
            MainScreenUiState(
                userName = "Odair",
                isDiretorOuAdm = true,
                title = R.string.subtitle_menu_passagens,
                homeActive = false,
                passagensActive = true,
                mainScreenState = MainScreenState.PASSAGENS(
                    listaBotoesMenuPassagensSample
                )
            )
        )
}

@Preview(showBackground = true)
@Composable
private fun MainScreenViagensPreview() {
        MainScreen(
            MainScreenUiState(
                userName = "Odair",
                title = R.string.subtitle_menu_operacoes,
                isDiretorOuAdm = true,
                homeActive = false,
                operacoesActive = true,
                mainScreenState = MainScreenState.OPERACOES(
                    listaBotoesMenus = listaMenuBotoesCategoriaSample
                )
            )
        )
}

@Preview(showBackground = true)
@Composable
private fun MainScreenVaziaPreview() {
    MainScreen(
            MainScreenUiState(
                userName = "Odair",
                mainScreenState = MainScreenState.HOME
            )
        )
}

@Preview(showBackground = true)
@Composable
private fun MainScreenDialogPreview() {
        MainScreen(
            MainScreenUiState(
                userName = "Odair",
                exibirUserDialog = true,
                mainScreenState = MainScreenState.HOME
            )
        )
}
