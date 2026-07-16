package dev.matheus.fluviapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.model.screendata.DadosBotoesMenus
import dev.matheus.fluviapp.model.screendata.SecaoMenu
import dev.matheus.fluviapp.sampledata.listaDadosDadosViagemHomeSampleCards
import dev.matheus.fluviapp.ui.components.contents.HomeContent
import dev.matheus.fluviapp.ui.components.drawer.FluviMenuDrawer
import dev.matheus.fluviapp.ui.states.MainScreenState
import dev.matheus.fluviapp.ui.states.MainScreenUiState

@Composable
fun MainScreen(
    state: MainScreenUiState,
    acoesPorSecao: Map<SecaoMenu, List<DadosBotoesMenus>> = emptyMap(),
    onClickInicio: () -> Unit = {},
    onClickDeslogar: () -> Unit = {},
    onClickAdicionarPassagem: (String) -> Unit = {},
    onRefresh: () -> Unit = {},
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
) {
    val estado = state.mainScreenState

    CommonScreen(
        modifier = Modifier,
        titleTopContent = R.string.subtitle_viagens_disponiveis,
        isMainTopAppBar = true,
        titleTopAppBar = 0,
        userNameTopAppBar = state.userName,
        isShowBottomAppBar = true,
        isShowRightIcon = false,
        hasRefresh = true,
        isRefreshing = state.isRefreshing,
        inicioAtivo = true,
        onClickInicio = onClickInicio,
        onRefresh = onRefresh,
        drawerContent = { fechar ->
            FluviMenuDrawer(
                userName = state.userName,
                secoes = state.secoesVisiveis,
                acoesPorSecao = acoesPorSecao,
                isDarkTheme = isDarkTheme,
                onInicio = { onClickInicio(); fechar() },
                onNavegar = { acao -> acao.onClick(); fechar() },
                onToggleTheme = onToggleTheme,
                onDeslogar = onClickDeslogar,
            )
        },
        content = { modifier, titulo ->
            Column(modifier = Modifier.fillMaxSize()) {
                // Banner offline-first (D4): não-bloqueante, sobre os dados do cache. Some quando um
                // snapshot do servidor chega (EstadoSincronizacao.reportarSucesso via RegistroSincronizacao).
                if (state.sincronizacaoComErro) BannerSincronizacaoOffline()

                when (estado) {
                    is MainScreenState.LOADING -> Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    is MainScreenState.HOME -> HomeContent(
                        modifier = modifier,
                        titulo = titulo,
                        listaViagens = state.listaViagens,
                        onClickNovaPassagem = onClickAdicionarPassagem,
                    )
                }
            }
        },
    )
}

@Composable
private fun BannerSincronizacaoOffline() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.msg_sincronizacao_offline),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Preview(name = "Home", showBackground = true)
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

@Preview(name = "Home offline (banner)", showBackground = true)
@Composable
private fun MainScreenOfflinePreview() {
    MainScreen(
        MainScreenUiState(
            userName = "Odair",
            secoesVisiveis = SecaoMenu.entries,
            listaViagens = listaDadosDadosViagemHomeSampleCards,
            sincronizacaoComErro = true,
            mainScreenState = MainScreenState.HOME,
        )
    )
}
