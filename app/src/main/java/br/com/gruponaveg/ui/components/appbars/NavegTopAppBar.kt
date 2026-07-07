package br.com.gruponaveg.ui.components.appbars

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.gruponaveg.R
import br.com.gruponaveg.ui.components.texts.TextRegularWhiteItalic
import br.com.gruponaveg.ui.components.texts.TextTitleWhiteItalic
import br.com.gruponaveg.ui.theme.NavyBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavegTopAppBar(
    modifier: Modifier,
    isMainTopAppBar: Boolean,
    userName: String = "",
    title: Int = 0,
    isShowRightIcon: Boolean,
    rightIcon: ImageVector,
    onClickUsername: () -> Unit = {},
    onClickNavigationIcon: () -> Unit = {},
    onClickRightIcon: () -> Unit = {}
) {
    if (isMainTopAppBar) {
        TopAppBar(
            title = {
                Image(
                    painter = painterResource(id = R.drawable.logo1),
                    contentDescription = stringResource(R.string.description_logo_1)
                )
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = NavyBlue,
                titleContentColor = Color.White,
                actionIconContentColor = Color.White
            ),
            actions = {
                Row(
                    modifier = modifier
                        .clickable { onClickUsername() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                        TextRegularWhiteItalic(text = userName)
                        Icon(
                            modifier = modifier.height(50.dp),
                            painter = painterResource(id = R.drawable.ic_user_75),
                            contentDescription = stringResource(id = R.string.description_icon_user)
                        )
                }
            }
        )
    } else {
        CenterAlignedTopAppBar(
            navigationIcon = {
                IconButton(onClick = onClickNavigationIcon) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.btn_icon_voltar),
                        tint = Color.White
                    )
                }
            },
            title = {
                TextTitleWhiteItalic(text = stringResource(id = title))
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = NavyBlue,
                titleContentColor = Color.White
            ), actions = {
                if(isShowRightIcon) {
                    IconButton(onClick = onClickRightIcon) {
                        Icon(
                            imageVector = rightIcon,
                            contentDescription = stringResource(id = R.string.description_right_icon),
                            tint = Color.White
                        )
                    }
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainNavegTopAppBarPreview() {
    NavegTopAppBar(
        modifier = Modifier,
        isMainTopAppBar = true,
        isShowRightIcon = false,
        userName = "Odair",
        rightIcon = Icons.Filled.Search
    )
}

@Preview(showBackground = true)
@Composable
private fun AlternateNavegTopAppBarPreview() {
    NavegTopAppBar(
        modifier = Modifier,
        isMainTopAppBar = false,
        title = R.string.subtitle_nova_passagem,
        isShowRightIcon = true,
        rightIcon = Icons.Filled.Search
    )
}