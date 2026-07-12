package dev.matheus.fluviapp.ui.components.drawer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.model.screendata.DadosBotoesMenus
import dev.matheus.fluviapp.model.screendata.SecaoMenu
import dev.matheus.fluviapp.ui.components.texts.TextTitleBrownItalic

/**
 * Menu lateral (drawer, à direita). Tudo vive aqui — nada troca o conteúdo da Main Screen:
 * - cabeçalho com foto/ícone + nome;
 * - Início + seções como **sub-menus expansíveis (acordeão)**; expandir mostra as ações
 *   (cadastrar/pesquisar), que navegam direto;
 * - rodapé com alternância de tema e sair.
 *
 * Ancoragem à direita e gesto de arrastar são do ModalNavigationDrawer que o hospeda.
 */
@Composable
fun FluviMenuDrawer(
    userName: String,
    secoes: List<SecaoMenu>,
    acoesPorSecao: Map<SecaoMenu, List<DadosBotoesMenus>>,
    isDarkTheme: Boolean,
    onInicio: () -> Unit,
    onNavegar: (DadosBotoesMenus) -> Unit,
    onToggleTheme: () -> Unit,
    onDeslogar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expandida by remember { mutableStateOf<SecaoMenu?>(null) }

    ModalDrawerSheet(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(40.dp),
                painter = painterResource(id = R.drawable.ic_user_75),
                contentDescription = stringResource(id = R.string.description_icon_user),
            )
            TextTitleBrownItalic(text = userName)
        }
        HorizontalDivider()

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            NavigationDrawerItem(
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                icon = { Icon(imageVector = Icons.Default.Home, contentDescription = null) },
                label = { Text(stringResource(R.string.btn_menu_inicio)) },
                selected = false,
                onClick = onInicio,
            )

            secoes.forEach { secao ->
                val aberta = expandida == secao
                NavigationDrawerItem(
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    icon = {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(id = secao.icone),
                            contentDescription = null,
                        )
                    },
                    label = { Text(stringResource(secao.titulo)) },
                    badge = {
                        Icon(
                            imageVector = if (aberta) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                        )
                    },
                    selected = aberta,
                    onClick = { expandida = if (aberta) null else secao },
                )

                AnimatedVisibility(visible = aberta) {
                    Column {
                        acoesPorSecao[secao].orEmpty().forEach { acao ->
                            NavigationDrawerItem(
                                modifier = Modifier.padding(
                                    PaddingValues(start = 24.dp, end = 12.dp),
                                ),
                                icon = {
                                    Icon(
                                        modifier = Modifier.size(24.dp),
                                        painter = painterResource(id = acao.icon),
                                        contentDescription = null,
                                    )
                                },
                                label = { Text(stringResource(acao.title)) },
                                selected = false,
                                onClick = { onNavegar(acao) },
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        NavigationDrawerItem(
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            icon = {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                    contentDescription = null,
                )
            },
            label = { Text(stringResource(R.string.label_tema_escuro)) },
            badge = { Switch(checked = isDarkTheme, onCheckedChange = { onToggleTheme() }) },
            selected = false,
            onClick = onToggleTheme,
        )
        NavigationDrawerItem(
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            icon = { Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
            label = { Text(stringResource(R.string.btn_deslogar)) },
            selected = false,
            onClick = onDeslogar,
        )
    }
}
