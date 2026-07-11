package dev.matheus.fluviapp.ui.components.drawer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.DarkMode
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.model.screendata.SecaoMenu
import dev.matheus.fluviapp.ui.components.texts.TextTitleBrownItalic

/**
 * Conteúdo do menu lateral (drawer, à direita). Concentra:
 * - cabeçalho com foto/ícone + nome do usuário;
 * - navegação: Início + seções visíveis ao cargo;
 * - rodapé com alternância de tema e sair (funcionalidades que viviam no antigo UserDialog).
 *
 * A ancoragem à direita e o gesto de arrastar são do ModalNavigationDrawer que o hospeda.
 */
@Composable
fun FluviMenuDrawer(
    userName: String,
    secoes: List<SecaoMenu>,
    naInicio: Boolean,
    secaoAtual: SecaoMenu?,
    isDarkTheme: Boolean,
    onInicio: () -> Unit,
    onSelecionar: (SecaoMenu) -> Unit,
    onToggleTheme: () -> Unit,
    onDeslogar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalDrawerSheet(modifier = modifier) {
        // Cabeçalho: foto/ícone + nome.
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

        // Navegação (rolável, ocupa o meio; empurra o rodapé p/ baixo).
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            NavigationDrawerItem(
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                icon = { Icon(imageVector = Icons.Default.Home, contentDescription = null) },
                label = { Text(stringResource(R.string.btn_menu_inicio)) },
                selected = naInicio,
                onClick = onInicio,
            )
            secoes.forEach { secao ->
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
                    selected = !naInicio && secao == secaoAtual,
                    onClick = { onSelecionar(secao) },
                )
            }
        }

        HorizontalDivider()

        // Rodapé: tema + sair.
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
