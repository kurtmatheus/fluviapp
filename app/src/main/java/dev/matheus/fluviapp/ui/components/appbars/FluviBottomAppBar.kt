package dev.matheus.fluviapp.ui.components.appbars

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.theme.FluviAppTheme

/**
 * Bottom bar (ADR-0012 Fase 5): três lugares — Início · **Embarque** · Menu. O embarque é a ação de
 * rotina da doca, promovida ao centro como botão de acento elevado (`colorScheme.primary`) com o
 * ícone de scanner — a única cor de acento da barra. A barra é `secondary` (HeaderNavy nos dois temas);
 * a seleção usa pílula Material 3 (ícone/label tingidos de acento), aposentando o retângulo a 12%.
 * A barra não navega sozinha: dispara os callbacks que o NavHost pluga na rota de embarque.
 */
@Composable
fun FluviBottomAppBar(
    modifier: Modifier,
    inicioAtivo: Boolean,
    onClickInicio: () -> Unit,
    onClickEmbarque: () -> Unit,
    onClickMenu: () -> Unit,
) {
    BottomAppBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ItemBottomAppBar(
                onClick = onClickInicio,
                icone = Icons.Default.Home,
                titulo = R.string.btn_menu_inicio,
                ativo = inicioAtivo,
            )
            FabEmbarque(onClick = onClickEmbarque)
            ItemBottomAppBar(
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
    onClick: () -> Unit,
    icone: ImageVector,
    titulo: Int,
    ativo: Boolean,
) {
    // Seleção Material 3: pílula arredondada com ícone e label tingidos de acento (primary).
    val corConteudo = if (ativo) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSecondary
    }
    val fundoPilula = if (ativo) {
        Modifier.background(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            shape = RoundedCornerShape(percent = 50),
        )
    } else {
        Modifier
    }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .clickable { onClick() }
            .then(fundoPilula)
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icone,
            contentDescription = stringResource(id = titulo),
            tint = corConteudo,
        )
        Text(
            text = stringResource(id = titulo),
            color = corConteudo,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

/**
 * Ação central de embarque: botão de acento elevado. O acento segue `colorScheme.primary` (SteelTeal
 * no tema claro, AquaAccent no escuro) e o ícone usa `onPrimary` — contraste garantido pelo tema em
 * ambos, sem cor fora do `colorScheme` (decisão de design da Fase 5).
 */
@Composable
private fun FabEmbarque(onClick: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shadowElevation = 6.dp,
            modifier = Modifier.size(52.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = stringResource(id = R.string.btn_embarque),
                )
            }
        }
        Text(
            text = stringResource(id = R.string.btn_embarque),
            color = MaterialTheme.colorScheme.onSecondary,
            style = MaterialTheme.typography.labelSmall,
        )
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
            onClickEmbarque = {},
            onClickMenu = {},
        )
    }
}