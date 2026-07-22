package dev.matheus.fluviapp.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.components.CommonScaffold
import dev.matheus.fluviapp.ui.components.contents.CommonTopRow

@Composable
fun CommonScreen(
    modifier: Modifier,
    titleTopContent: Int,
    isMainTopAppBar: Boolean,
    titleTopAppBar: Int,
    userNameTopAppBar: String,
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
    drawerContent: (@Composable (fechar: () -> Unit) -> Unit)? = null,
    content: @Composable (Modifier, Int) -> Unit,
) {
    CommonScaffold(
        modifier = modifier,
        isMainTopAppBar = isMainTopAppBar,
        userNameTopAppBar = userNameTopAppBar,
        titleTopAppBar = titleTopAppBar,
        isShowBottomAppBar = isShowBottomAppBar,
        isShowRightIcon = isShowRightIcon,
        rightIcon = rightIcon,
        hasRefresh = hasRefresh,
        isRefreshing = isRefreshing,
        inicioAtivo = inicioAtivo,
        onClickInicio = onClickInicio,
        onClickEmbarque = onClickEmbarque,
        onClickVoltar = onClickVoltar,
        onClickRightIcon = onClickRightIcon,
        onRefresh = onRefresh,
        drawerContent = drawerContent,
        content = { content(modifier, titleTopContent) },
    )
}

@Preview(showBackground = true)
@Composable
private fun MenuCommonScreenPreview() {
    CommonScreen(
        modifier = Modifier,
        titleTopContent = R.string.subtitle_viagens_disponiveis,
        isMainTopAppBar = true,
        titleTopAppBar = 0,
        userNameTopAppBar = "Odair",
        isShowBottomAppBar = true,
        isShowRightIcon = false,
        hasRefresh = false,
        isRefreshing = false,
        inicioAtivo = true,
        content = { modifier, title -> CommonTopRow(modifier = modifier, titulo = title) },
    )
}

@Preview(showBackground = true)
@Composable
private fun FormCommonScreenPreview() {
    CommonScreen(
        modifier = Modifier,
        titleTopContent = R.string.subtitle_viagens_disponiveis,
        isMainTopAppBar = false,
        titleTopAppBar = R.string.title_top_passagem,
        userNameTopAppBar = "",
        isShowBottomAppBar = false,
        isShowRightIcon = false,
        hasRefresh = false,
        isRefreshing = false,
        content = { modifier, title -> CommonTopRow(modifier = modifier, titulo = title) },
    )
}
