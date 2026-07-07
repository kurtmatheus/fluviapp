package dev.matheus.fluviapp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.components.appbars.FluviBottomAppBar
import dev.matheus.fluviapp.ui.components.appbars.FluviTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonScaffold(
    modifier: Modifier,
    homeActive: Boolean,
    passagensActive: Boolean,
    viagensActive: Boolean,
    isMainTopAppBar: Boolean,
    userNameTopAppBar: String,
    titleTopAppBar: Int,
    isShowBottomAppBar: Boolean,
    isShowMenuViagens: Boolean,
    isShowRightIcon: Boolean,
    rightIcon: ImageVector = Icons.Filled.Search,
    hasRefresh: Boolean,
    isRefreshing: Boolean,
    onClickHome: () -> Unit = {},
    onClickMenuPassagens: () -> Unit = {},
    onClickMenuViagens: () -> Unit = {},
    onClickUsername: () -> Unit = {},
    onClickVoltar: () -> Unit = {},
    onClickRightIcon: () -> Unit = {},
    onRefresh: () -> Unit = {},
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
                onClickUsername = onClickUsername,
                onClickNavigationIcon = onClickVoltar,
                onClickRightIcon = onClickRightIcon
            )
        },
        bottomBar = {
            if (isShowBottomAppBar) FluviBottomAppBar(
                modifier = modifier,
                homeActive = homeActive,
                passagensActive = passagensActive,
                viagensActive = viagensActive,
                isShowMenuViagens = isShowMenuViagens,
                onClickHome = onClickHome,
                onClickMenuPassagens = onClickMenuPassagens,
                onClickMenuViagens = onClickMenuViagens,
            )
        }
    ) {
        if (hasRefresh) {
            PullToRefreshBox(
                modifier = modifier
                    .padding(it)
                    .fillMaxSize()
                    .navigationBarsPadding(),
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
            ) {
                content()
            }
        } else {
            Box(
                modifier = modifier
                    .padding(it)
                    .fillMaxSize()
                    .navigationBarsPadding()
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
        homeActive = true,
        passagensActive = false,
        viagensActive = false,
        isMainTopAppBar = true,
        userNameTopAppBar = "Odair",
        titleTopAppBar = 0,
        isShowBottomAppBar = true,
        isShowMenuViagens = true,
        isShowRightIcon = false,
        hasRefresh = false,
        isRefreshing = false,
        content = {
            Text(
                modifier = Modifier.padding(20.dp),
                text = "Conteudo",
            )
        },
        rightIcon = Icons.Filled.Search
    )
}

@Preview(showBackground = true)
@Composable
private fun ScaffoldGenericoAlternatePreview() {
    CommonScaffold(
        modifier = Modifier,
        homeActive = true,
        passagensActive = false,
        viagensActive = false,
        isMainTopAppBar = false,
        userNameTopAppBar = "",
        titleTopAppBar = R.string.title_top_passagem,
        isShowBottomAppBar = false,
        isShowMenuViagens = true,
        isShowRightIcon = true,
        hasRefresh = false,
        isRefreshing = false,
        content = {
            Text(
                modifier = Modifier.padding(20.dp),
                text = "Conteudo",
            )
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun ScaffoldGenericoAlternateComPullRefreshPreview() {
    CommonScaffold(
        modifier = Modifier,
        homeActive = true,
        passagensActive = false,
        viagensActive = false,
        isMainTopAppBar = false,
        userNameTopAppBar = "",
        titleTopAppBar = R.string.title_top_passagem,
        isShowBottomAppBar = false,
        isShowMenuViagens = true,
        isShowRightIcon = true,
        rightIcon = Icons.Filled.Search,
        hasRefresh = true,
        isRefreshing = true,
        content = {
            Text(
                modifier = Modifier.padding(20.dp),
                text = "Conteudo",
            )
        }
    )
}