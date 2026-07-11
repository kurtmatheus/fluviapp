package dev.matheus.fluviapp.ui.screens.forms

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.components.contents.CommonTopRow
import dev.matheus.fluviapp.ui.screens.CommonScreen

@Composable
fun CommonScreenNoBottom(
    titleTopAppBar: Int,
    titleTopContent: Int,
    isShowRightIcon: Boolean,
    rightIcon: ImageVector = Icons.Filled.Search,
    hasRefresh: Boolean,
    isRefreshing: Boolean,
    onClickVoltar: () -> Unit = {},
    onClickRightIcon: () -> Unit = {},
    onRefresh: () -> Unit = {},
    content: @Composable (Modifier, Int) -> Unit,
) {
    CommonScreen(
        modifier = Modifier,
        titleTopContent = titleTopContent,
        isMainTopAppBar = false,
        titleTopAppBar = titleTopAppBar,
        userNameTopAppBar = "",
        isShowBottomAppBar = false,
        isShowRightIcon = isShowRightIcon,
        rightIcon = rightIcon,
        hasRefresh = hasRefresh,
        isRefreshing = isRefreshing,
        onClickRightIcon = onClickRightIcon,
        onClickVoltar = onClickVoltar,
        onRefresh = onRefresh
    ) { modifier, titulo ->
        content(modifier, titulo)
    }

}

@Preview
@Composable
private fun CommonFormScreenPreview() {
        CommonScreenNoBottom(
            titleTopAppBar = R.string.subtitle_nova_passagem,
            titleTopContent = R.string.subtitle_menu_operacoes,
            isShowRightIcon = false,
            hasRefresh = false,
            isRefreshing = false,
            content = { modifier, title ->
                CommonTopRow(modifier = modifier, titulo = title)
            }
        )
}

@Preview
@Composable
private fun CommonFormScreenComIconPreview() {
    CommonScreenNoBottom(
        titleTopAppBar = R.string.subtitle_nova_passagem,
        titleTopContent = R.string.subtitle_nova_passagem,
        isShowRightIcon = true,
        hasRefresh = false,
        isRefreshing = false,
        content = { modifier, title ->
            CommonTopRow(modifier = modifier, titulo = title)
        }
    )
}