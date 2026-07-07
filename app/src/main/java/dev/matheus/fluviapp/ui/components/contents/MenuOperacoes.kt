package dev.matheus.fluviapp.ui.components.contents

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.model.screendata.MenuBotoesCategoria
import dev.matheus.fluviapp.sampledata.listaMenuBotoesCategoriaSample
import dev.matheus.fluviapp.ui.components.cards.MenuOperacoesCard
import dev.matheus.fluviapp.ui.screens.MainScreen
import dev.matheus.fluviapp.ui.states.MainScreenState
import dev.matheus.fluviapp.ui.states.MainScreenUiState
import dev.matheus.fluviapp.ui.theme.FluviAppTheme

@Composable
fun MenuOperacoes(
    modifier: Modifier,
    listaMenu: List<MenuBotoesCategoria> = emptyList()
) {
    Column(
        modifier = modifier
    ) {
        listaMenu.forEach {
            MenuOperacoesCard(modifier = modifier, menuBotoesCategoria = it)
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun MainScreenOperacoesPreview() {
    FluviAppTheme {
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
}

