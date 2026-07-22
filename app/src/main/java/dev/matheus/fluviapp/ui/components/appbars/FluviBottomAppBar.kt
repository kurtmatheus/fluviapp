package dev.matheus.fluviapp.ui.components.appbars

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R

/**
 * Deslocamento vertical do FAB de embarque para ele **protrair** acima da borda da barra (metade fora,
 * metade encaixada). O FAB é renderizado no slot `floatingActionButton` do Scaffold com
 * `FabPosition.Center` (que por padrão o coloca *acima* da barra); este offset o empurra para baixo até
 * atravessar a borda superior. Ajustável — é o único valor "de pixel" da composição.
 */
val EMBARQUE_FAB_STRADDLE = 34.dp

/**
 * Bottom bar (ADR-0012 Fase 5): três lugares — Início · **Embarque** · Menu. O embarque é a ação de
 * rotina da doca, promovida ao centro como **FAB protruso** (renderizado à parte, no slot
 * `floatingActionButton` do Scaffold via [FabEmbarque]); aqui a barra reserva o lugar central só com o
 * rótulo, sob o FAB. A barra é `secondary` (HeaderNavy nos dois temas); a seleção usa pílula Material 3
 * (ícone/label tingidos de acento), aposentando o retângulo a 12%. A barra não navega sozinha: dispara
 * os callbacks que o NavHost pluga na rota de embarque.
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
            // Lugar central: só o rótulo, sob o FAB protruso (que o Scaffold desenha por cima).
            RotuloEmbarqueCentro(onClick = onClickEmbarque)
            ItemBottomAppBar(
                onClick = onClickMenu,
                icone = Icons.Default.Menu,
                titulo = R.string.btn_menu,
                ativo = false,
            )
        }
    }
}

/**
 * FAB de embarque — ação central de acento, para o slot `floatingActionButton` do Scaffold com
 * `FabPosition.Center`. Protrai acima da barra via [EMBARQUE_FAB_STRADDLE]. O acento segue
 * `colorScheme.primary` (SteelTeal no claro, AquaAccent no escuro) e o ícone usa `onPrimary` —
 * contraste garantido pelo tema em ambos, sem cor fora do `colorScheme` (decisão de design da Fase 5).
 */
@Composable
fun FabEmbarque(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.offset(y = EMBARQUE_FAB_STRADDLE),
        shape = RoundedCornerShape(18.dp),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Icon(
            imageVector = Icons.Default.QrCodeScanner,
            contentDescription = stringResource(id = R.string.btn_embarque),
        )
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
 * Lugar central da barra: reserva a área do ícone (ocupada pelo FAB protruso, desenhado por cima) e
 * mostra só o rótulo, alinhado aos labels laterais. Também clicável (mesma ação do FAB).
 */
@Composable
private fun RotuloEmbarqueCentro(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Vão do ícone (24dp): o FAB protruso ocupa este espaço visualmente, por cima da barra.
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(id = R.string.btn_embarque),
            color = MaterialTheme.colorScheme.onSecondary,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

/**
 * Preview do conjunto barra + FAB protruso, replicando o empilhamento do Scaffold (FAB por cima,
 * centralizado). Não é o layout de produção (isso mora no CommonScaffold), só valida o visual.
 */
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun BottomAppBarComFabPreview() {
    dev.matheus.fluviapp.ui.theme.FluviAppTheme {
        Box(modifier = Modifier.fillMaxWidth()) {
            FluviBottomAppBar(
                modifier = Modifier,
                inicioAtivo = true,
                onClickInicio = {},
                onClickEmbarque = {},
                onClickMenu = {},
            )
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                FabEmbarque(onClick = {})
            }
        }
    }
}