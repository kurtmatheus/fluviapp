package dev.matheus.fluviapp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.components.appbars.FabEmbarque
import dev.matheus.fluviapp.ui.components.appbars.FluviBottomAppBar
import dev.matheus.fluviapp.ui.components.appbars.FluviTopAppBar
import kotlinx.coroutines.launch

/** Largura mínima (dp) para tratar a tela como tablet — aí o menu lateral fica permanente (sempre aberto). */
private const val LARGURA_MINIMA_TABLET_DP = 600

@Composable
fun CommonScaffold(
    modifier: Modifier,
    isMainTopAppBar: Boolean,
    userNameTopAppBar: String,
    titleTopAppBar: Int,
    isShowBottomAppBar: Boolean,
    isShowRightIcon: Boolean,
    hasRefresh: Boolean,
    isRefreshing: Boolean,
    rightIcon: ImageVector = Icons.Filled.Search,
    inicioAtivo: Boolean = false,
    onClickInicio: () -> Unit = {},
    onClickEmbarque: () -> Unit = {},
    onClickVoltar: () -> Unit = {},
    onClickRightIcon: () -> Unit = {},
    onRefresh: () -> Unit = {},
    /**
     * Conteúdo do menu lateral (drawer à **esquerda**; **permanente/sempre aberto** em tablet). Recebe um
     * `fechar` para colapsar após seleção — no-op no modo permanente (não fecha).
     */
    drawerContent: (@Composable (fechar: () -> Unit) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (drawerContent == null) {
        ScaffoldConteudo(
            modifier = modifier,
            isMainTopAppBar = isMainTopAppBar,
            userNameTopAppBar = userNameTopAppBar,
            titleTopAppBar = titleTopAppBar,
            isShowBottomAppBar = isShowBottomAppBar,
            isShowRightIcon = isShowRightIcon,
            hasRefresh = hasRefresh,
            isRefreshing = isRefreshing,
            rightIcon = rightIcon,
            inicioAtivo = inicioAtivo,
            onClickInicio = onClickInicio,
            onClickEmbarque = onClickEmbarque,
            onClickMenu = {},
            onClickVoltar = onClickVoltar,
            onClickRightIcon = onClickRightIcon,
            onRefresh = onRefresh,
            content = content,
        )
        return
    }

    // Tablet: menu lateral PERMANENTE (sempre aberto), à esquerda. O botão de menu vira inócuo (já aberto).
    if (LocalConfiguration.current.screenWidthDp >= LARGURA_MINIMA_TABLET_DP) {
        PermanentNavigationDrawer(
            drawerContent = { drawerContent {} },
        ) {
            ScaffoldConteudo(
                modifier = modifier,
                isMainTopAppBar = isMainTopAppBar,
                userNameTopAppBar = userNameTopAppBar,
                titleTopAppBar = titleTopAppBar,
                isShowBottomAppBar = isShowBottomAppBar,
                isShowRightIcon = isShowRightIcon,
                hasRefresh = hasRefresh,
                isRefreshing = isRefreshing,
                rightIcon = rightIcon,
                inicioAtivo = inicioAtivo,
                onClickInicio = onClickInicio,
                onClickEmbarque = onClickEmbarque,
                onClickMenu = {},
                onClickVoltar = onClickVoltar,
                onClickRightIcon = onClickRightIcon,
                onRefresh = onRefresh,
                mostrarMenuNaBarra = false,
                content = content,
            )
        }
        return
    }

    // Celular: menu lateral MODAL, abrindo da ESQUERDA (padrão do ModalNavigationDrawer). O botão/gesto
    // abre; a seleção fecha.
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val abrirDrawer: () -> Unit = { scope.launch { drawerState.open() } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { drawerContent { scope.launch { drawerState.close() } } },
    ) {
        ScaffoldConteudo(
            modifier = modifier,
            isMainTopAppBar = isMainTopAppBar,
            userNameTopAppBar = userNameTopAppBar,
            titleTopAppBar = titleTopAppBar,
            isShowBottomAppBar = isShowBottomAppBar,
            isShowRightIcon = isShowRightIcon,
            hasRefresh = hasRefresh,
            isRefreshing = isRefreshing,
            rightIcon = rightIcon,
            inicioAtivo = inicioAtivo,
            onClickInicio = onClickInicio,
            onClickEmbarque = onClickEmbarque,
            onClickMenu = abrirDrawer,
            onClickVoltar = onClickVoltar,
            onClickRightIcon = onClickRightIcon,
            onRefresh = onRefresh,
            content = content,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScaffoldConteudo(
    modifier: Modifier,
    isMainTopAppBar: Boolean,
    userNameTopAppBar: String,
    titleTopAppBar: Int,
    isShowBottomAppBar: Boolean,
    isShowRightIcon: Boolean,
    hasRefresh: Boolean,
    isRefreshing: Boolean,
    rightIcon: ImageVector,
    inicioAtivo: Boolean,
    onClickInicio: () -> Unit,
    onClickEmbarque: () -> Unit,
    onClickMenu: () -> Unit,
    onClickVoltar: () -> Unit,
    onClickRightIcon: () -> Unit,
    onRefresh: () -> Unit,
    mostrarMenuNaBarra: Boolean = true,
    content: @Composable () -> Unit,
) {
    val pullToRefreshState = rememberPullToRefreshState()
    Scaffold(
        topBar = {
            FluviTopAppBar(
                modifier = modifier,
                isMainTopAppBar = isMainTopAppBar,
                title = titleTopAppBar,
                isShowRightIcon = isShowRightIcon,
                rightIcon = rightIcon,
                userName = userNameTopAppBar,
                // O avatar (top bar principal) abre o menu lateral — não há mais dialog de usuário.
                onClickUsername = onClickMenu,
                onClickNavigationIcon = onClickVoltar,
                onClickRightIcon = onClickRightIcon,
            )
        },
        bottomBar = {
            if (isShowBottomAppBar) FluviBottomAppBar(
                modifier = modifier,
                inicioAtivo = inicioAtivo,
                onClickInicio = onClickInicio,
                onClickEmbarque = onClickEmbarque,
                onClickMenu = onClickMenu,
                mostrarMenu = mostrarMenuNaBarra,
            )
        },
        // FAB de embarque protruso, ancorado ao centro sobre a barra (só onde a barra aparece).
        floatingActionButton = {
            if (isShowBottomAppBar) FabEmbarque(onClick = onClickEmbarque)
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) {
        if (hasRefresh) {
            PullToRefreshBox(
                modifier = modifier.padding(it).fillMaxSize().navigationBarsPadding(),
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
            ) {
                content()
            }
        } else {
            Box(
                modifier = modifier.padding(it).fillMaxSize().navigationBarsPadding(),
            ) {
                content()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScaffoldGenericoPreview() {
    CommonScaffold(
        modifier = Modifier,
        isMainTopAppBar = true,
        userNameTopAppBar = "Odair",
        titleTopAppBar = 0,
        isShowBottomAppBar = true,
        isShowRightIcon = false,
        hasRefresh = false,
        isRefreshing = false,
        inicioAtivo = true,
        content = { Text(modifier = Modifier.padding(20.dp), text = "Conteudo") },
    )
}

@Preview(showBackground = true)
@Composable
private fun ScaffoldFormPreview() {
    CommonScaffold(
        modifier = Modifier,
        isMainTopAppBar = false,
        userNameTopAppBar = "",
        titleTopAppBar = R.string.title_top_passagem,
        isShowBottomAppBar = false,
        isShowRightIcon = true,
        hasRefresh = false,
        isRefreshing = false,
        content = { Text(modifier = Modifier.padding(20.dp), text = "Conteudo") },
    )
}
