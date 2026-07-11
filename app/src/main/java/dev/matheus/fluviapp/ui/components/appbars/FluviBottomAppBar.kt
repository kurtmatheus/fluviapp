package dev.matheus.fluviapp.ui.components.appbars

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.theme.FluviAppTheme

/**
 * Bottom bar remodelada: dois itens — Início e Menu (abre o drawer lateral). Temada via
 * MaterialTheme (segue claro/escuro), aposentando o NavyBlue/White fixos do modelo antigo.
 */
@Composable
fun FluviBottomAppBar(
    modifier: Modifier,
    inicioAtivo: Boolean,
    onClickInicio: () -> Unit,
    onClickMenu: () -> Unit,
) {
    BottomAppBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary,
    ) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ItemBottomAppBar(
                modifier = modifier,
                onClick = onClickInicio,
                icone = Icons.Default.Home,
                titulo = R.string.btn_menu_inicio,
                ativo = inicioAtivo,
            )
            ItemBottomAppBar(
                modifier = modifier,
                onClick = onClickMenu,
                icone = Icons.Default.Menu,
                titulo = R.string.btn_menu,
                ativo = false,
            )
        }
    }
}

@Composable
private fun ItemBottomAppBar(
    modifier: Modifier,
    onClick: () -> Unit,
    icone: ImageVector,
    titulo: Int,
    ativo: Boolean,
) {
    val fundo = if (ativo) {
        modifier.background(MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.12f))
    } else {
        modifier
    }
    Column(
        modifier = fundo
            .clickable { onClick() }
            .padding(horizontal = 32.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(imageVector = icone, contentDescription = stringResource(id = titulo))
        Text(text = stringResource(id = titulo))
    }
}

@Preview(showBackground = true)
@Composable
private fun BottomAppBarPreview() {
    FluviAppTheme {
        FluviBottomAppBar(
            modifier = Modifier,
            inicioAtivo = true,
            onClickInicio = {},
            onClickMenu = {},
        )
    }
}
