package br.com.gruponaveg.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import br.com.gruponaveg.R
import br.com.gruponaveg.ui.components.CommonScaffold
import br.com.gruponaveg.ui.components.contents.CommonTopRow

@Composable
fun CommonScreen(
    modifier: Modifier,
    titleTopContent: Int,
    homeActive: Boolean,
    passagensActive: Boolean,
    viagensActive: Boolean,
    isMainTopAppBar: Boolean,
    titleTopAppBar: Int,
    userNameTopAppBar: String,
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
    content: @Composable (Modifier, Int) -> Unit,
) {
    CommonScaffold(
        modifier = modifier,
        homeActive = homeActive,
        passagensActive = passagensActive,
        viagensActive = viagensActive,
        isMainTopAppBar = isMainTopAppBar,
        userNameTopAppBar = userNameTopAppBar,
        titleTopAppBar = titleTopAppBar,
        isShowBottomAppBar = isShowBottomAppBar,
        isShowMenuViagens = isShowMenuViagens,
        isShowRightIcon = isShowRightIcon,
        rightIcon = rightIcon,
        hasRefresh = hasRefresh,
        isRefreshing = isRefreshing,
        onClickHome = onClickHome,
        onClickMenuPassagens = onClickMenuPassagens,
        onClickMenuViagens = onClickMenuViagens,
        onClickUsername = onClickUsername,
        onClickVoltar = onClickVoltar,
        onClickRightIcon = onClickRightIcon,
        onRefresh = onRefresh,
        content = {
            content(modifier, titleTopContent)
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun MenuCommonScreenPreview() {
        CommonScreen(
            modifier = Modifier,
            titleTopContent = R.string.subtitle_viagens_disponiveis,
            homeActive = true,
            passagensActive = false,
            viagensActive = false,
            isMainTopAppBar = true,
            titleTopAppBar = 0,
            userNameTopAppBar = "Odair",
            isShowBottomAppBar = true,
            isShowMenuViagens = true,
            isShowRightIcon = false,
            hasRefresh = false,
            isRefreshing = false,
            content = { modifier, title ->
                CommonTopRow(modifier = modifier, titulo = title)
            },
        )
}

@Preview(showBackground = true)
@Composable
private fun FormCommonScreenPreview() {
        CommonScreen(
            modifier = Modifier,
            titleTopContent = R.string.subtitle_viagens_disponiveis,
            homeActive = true,
            passagensActive = false,
            viagensActive = false,
            isMainTopAppBar = false,
            titleTopAppBar = R.string.title_top_passagem,
            userNameTopAppBar = "",
            isShowBottomAppBar = false,
            isShowMenuViagens = true,
            isShowRightIcon = false,
            hasRefresh = false,
            isRefreshing = false,
            content = { modifier, title ->
                CommonTopRow(modifier = modifier, titulo = title)
            }
        )
}