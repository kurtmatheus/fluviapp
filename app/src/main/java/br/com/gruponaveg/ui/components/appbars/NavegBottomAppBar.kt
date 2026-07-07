package br.com.gruponaveg.ui.components.appbars

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.gruponaveg.R
import br.com.gruponaveg.ui.theme.NavyBlue

@Composable
fun NavegBottomAppBar(
    modifier: Modifier,
    homeActive: Boolean,
    passagensActive: Boolean,
    viagensActive: Boolean,
    isShowMenuViagens: Boolean,
    onClickHome: () -> Unit,
    onClickMenuPassagens: () -> Unit,
    onClickMenuViagens: () -> Unit
) {
    BottomAppBar(
        modifier = modifier,
        containerColor = NavyBlue,
        contentColor = White
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ButtonBottomAppBar(
                modifier = modifier,
                onClick = onClickHome,
                icone = R.drawable.ic_home,
                iconDescription = R.string.description_icon_home,
                tituloBotao = R.string.btn_menu_home,
                active = homeActive,
            )
            ButtonBottomAppBar(
                modifier = modifier,
                onClick = onClickMenuPassagens,
                icone = R.drawable.ic_bilhete,
                iconDescription = R.string.description_icon_passagens,
                tituloBotao = R.string.btn_menu_passagens,
                active = passagensActive,
            )
            if (isShowMenuViagens) {
                ButtonBottomAppBar(
                    modifier = modifier,
                    onClick = onClickMenuViagens,
                    icone = R.drawable.ic_oper_24,
                    iconDescription = R.string.description_icon_settings,
                    tituloBotao = R.string.btn_menu_operacoes,
                    active = viagensActive,
                )
            }
        }
    }
}

@Composable
private fun ButtonBottomAppBar(
    modifier: Modifier,
    onClick: () -> Unit,
    icone: Int,
    iconDescription: Int,
    tituloBotao: Int,
    active: Boolean
) {
    val background = if (active) modifier.background(color = White.copy(alpha = 0.1f))

    else modifier.clickable { onClick() }

    Box(
        modifier = background
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = modifier
                    .padding(10.dp, 10.dp)
            ) {
                VerticalDivider(
                    modifier = modifier
                        .fillMaxHeight()
                        .width(1.dp),
                    color = White
                )
            }
            Column(
                modifier = modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Icon(
                    painter = painterResource(id = icone),
                    contentDescription = stringResource(id = iconDescription)
                )
                Text(text = stringResource(id = tituloBotao))
            }
            Box(
                modifier = modifier
                    .padding(10.dp, 10.dp)
            ) {
                VerticalDivider(
                    modifier = modifier
                        .fillMaxHeight()
                        .width(1.dp),
                    color = White
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BottomAppBarPreview() {
    NavegBottomAppBar(
        modifier = Modifier,
        homeActive = true,
        passagensActive = false,
        viagensActive = false,
        isShowMenuViagens = true,
        onClickHome = {},
        onClickMenuPassagens = {},
        onClickMenuViagens = {}
    )
}