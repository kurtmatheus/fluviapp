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
import dev.matheus.fluviapp.model.screendata.SecaoMenu
import dev.matheus.fluviapp.sampledata.listaBotoesMenuPassagensSample
import dev.matheus.fluviapp.sampledata.listaDadosDadosViagemHomeSampleCards
import dev.matheus.fluviapp.ui.components.contents.HomeContent
import dev.matheus.fluviapp.ui.components.contents.MenuPassagem
import dev.matheus.fluviapp.ui.components.drawer.FluviMenuDrawer
import dev.matheus.fluviapp.ui.states.MainScreenState
import dev.matheus.fluviapp.ui.states.MainScreenUiState

@Composable
fun MainScreen(
    state: MainScreenUiState,
    onClickInicio: () -> Unit = {},
    onSelecionarSecao: (SecaoMenu) -> Unit = {},
    onClickDeslogar: () -> Unit = {},
    onClickAdicionarPassagem: (String) -> Unit = {},
    onRefresh: () -> Unit = {},
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
) {
    val estado = state.mainScreenState
    val naInicio = estado is MainScreenState.HOME || estado is MainScreenState.LOADING
    val tituloConteudo = when (estado) {
        is MainScreenState.SECAO -> estado.secao.titulo
        else -> R.string.subtitle_viagens_disponiveis
    }

    CommonScreen(
        modifier = Modifier,
        titleTopContent = tituloConteudo,
        isMainTopAppBar = true,
        titleTopAppBar = 0,
        userNameTopAppBar = state.userName,
        isShowBottomAppBar = true,
        isShowRightIcon = false,
        hasRefresh = true,
        isRefreshing = state.isRefreshing,
        inicioAtivo = naInicio,
        onClickInicio = onClickInicio,
        onRefresh = onRefresh,
        drawerContent = { fechar ->
            FluviMenuDrawer(
                userName = state.userName,
                secoes = state.secoesVisiveis,
                naInicio = naInicio,
                secaoAtual = (estado as? MainScreenState.SECAO)?.secao,
                isDarkTheme = isDarkTheme,
                onInicio = { onClickInicio(); fechar() },
                onSelecionar = { onSelecionarSecao(it); fechar() },
                onToggleTheme = onToggleTheme,
                onDeslogar = onClickDeslogar,
            )
        },
        content = { modifier, titulo ->
            when (estado) {
                is MainScreenState.LOADING -> Box(modifier = modifier.fillMaxSize()) {
                    CircularProgressIndicator(
                        modifier = modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                is MainScreenState.HOME -> HomeContent(
                    modifier = modifier,
                    titulo = titulo,
                    listaViagens = state.listaViagens,
                    onClickNovaPassagem = onClickAdicionarPassagem,
                )

                is MainScreenState.SECAO -> MenuPassagem(
                    modifier = modifier,
                    titulo = titulo,
                    listaBotoes = estado.acoes,
                )
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun MainScreenHomePreview() {
    MainScreen(
        MainScreenUiState(
            userName = "Odair",
            secoesVisiveis = SecaoMenu.entries,
            listaViagens = listaDadosDadosViagemHomeSampleCards,
            mainScreenState = MainScreenState.HOME,
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun MainScreenSecaoPreview() {
    MainScreen(
        MainScreenUiState(
            userName = "Odair",
            secoesVisiveis = SecaoMenu.entries,
            mainScreenState = MainScreenState.SECAO(SecaoMenu.PASSAGEM, listaBotoesMenuPassagensSample),
        )
    )
}
