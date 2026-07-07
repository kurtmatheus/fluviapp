package br.com.gruponaveg.ui.components.contents

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import br.com.gruponaveg.R
import br.com.gruponaveg.model.screendata.MenuBotoesCategoria
import br.com.gruponaveg.sampledata.listaMenuBotoesCategoriaSample
import br.com.gruponaveg.ui.components.cards.MenuOperacoesCard
import br.com.gruponaveg.ui.screens.MainScreen
import br.com.gruponaveg.ui.states.MainScreenState
import br.com.gruponaveg.ui.states.MainScreenUiState
import br.com.gruponaveg.ui.theme.NavegAppTheme

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
    NavegAppTheme {
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

