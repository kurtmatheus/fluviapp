package dev.matheus.fluviapp.ui.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.model.screendata.MenuBotoesCategoria
import dev.matheus.fluviapp.sampledata.listaMenuBotoesCategoriaSample
import dev.matheus.fluviapp.ui.components.texts.TextRegularWhiteItalic
import dev.matheus.fluviapp.ui.components.texts.TextTitleWhiteItalic
import dev.matheus.fluviapp.ui.theme.FluviAppTheme

@Composable
fun MenuOperacoesCard(
    modifier: Modifier,
    menuBotoesCategoria: MenuBotoesCategoria,
) {
    CommonExpandableCard(
        modifier = modifier,
        roundedCornerShape = false,
        contentHeader = { dropDownIcon ->
            Icon(
                modifier = modifier
                    .height(25.dp),
                painter = painterResource(id = menuBotoesCategoria.iconCategoria),
                contentDescription = stringResource(id = R.string.description_icon_navio),
                tint = MaterialTheme.colorScheme.onPrimary
            )

            TextTitleWhiteItalic(
                text = stringResource(menuBotoesCategoria.tituloCategoria)
            )

            Icon(
                modifier = modifier
                    .height(25.dp),
                imageVector = dropDownIcon,
                contentDescription = stringResource(id = R.string.description_icon_expand),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        },
        contentExpand = {
            LazyColumn(
                modifier = modifier.background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f))
            ) {
                items(menuBotoesCategoria.dadosBotoesMenus) {
                    Column {
                        Row(
                            modifier = modifier
                                .padding(5.dp)
                                .fillMaxWidth()
                                .clickable {
                                    it.onClick()
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                modifier = modifier
                                    .size(25.dp),
                                painter = painterResource(id = it.icon),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondary
                            )
                            Spacer(modifier = modifier.padding(5.dp))
                            TextRegularWhiteItalic(
                                modifier = modifier.padding(10.dp),
                                text = stringResource(id = it.title)
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSecondary)
                    }
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun BalancoNavioCardPreview() {
    FluviAppTheme {
        Box(
            modifier = Modifier.padding(10.dp)
        ) {
            MenuOperacoesCard(
                modifier = Modifier,
                menuBotoesCategoria = listaMenuBotoesCategoriaSample.first()
            )
        }
    }
}