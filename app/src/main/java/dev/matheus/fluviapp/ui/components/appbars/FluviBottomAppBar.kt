package dev.matheus.fluviapp.ui.components.appbars

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R

/**
 * Deslocamento vertical do FAB de embarque para ele **protrair** acima da borda da barra (metade fora,
 * metade encaixada). O FAB é renderizado no slot `floatingActionButton` do Scaffold com
 * `FabPosition.Center` (que por padrão o coloca *acima* da barra); este offset o empurra para baixo até
 * atravessar a borda superior. Ajustável — é o único valor "de pixel" da composição.
 */
val EMBARQUE_FAB_STRADDLE = 55.dp

/**
 * Bottom bar: **um lugar só, o embarque**.
 *
 * O ADR-0012 Fase 5 desenhou três — Início · Embarque · Menu —, e os dois das pontas perderam o sentido
 * quando a barra voltou ao painel, cada um pelo seu motivo:
 *
 * - **Início** era navegação para a tela onde a barra aparece. A barra existe no painel, e o painel *é* o
 *   início: o botão levava de onde se está para onde se está;
 * - **Menu** duplicava o gesto que já abre o menu lateral — o avatar na top bar —, e o duplicava pior:
 *   no tablet, onde o drawer é permanente, ele já nascia inócuo (era o que o `mostrarMenu` remendava).
 *
 * O que sobra não é uma barra de navegação com um item: é o **pedestal do FAB**. O embarque é a ação de
 * rotina da doca, promovida ao centro como FAB protruso (renderizado à parte, no slot
 * `floatingActionButton` do Scaffold via [FabEmbarque]); a barra reserva o lugar sob ele e escreve o
 * rótulo, porque um FAB sozinho diz *toque aqui* sem dizer para quê.
 *
 * A barra é `secondary` (HeaderNavy nos dois temas) e não navega sozinha: dispara o callback que o
 * NavHost pluga na rota de embarque.
 */
@Composable
fun FluviBottomAppBar(
    modifier: Modifier,
    onClickEmbarque: () -> Unit,
) {
    BottomAppBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RotuloEmbarqueCentro(onClick = onClickEmbarque)
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

/**
 * O lugar da barra: reserva a área do ícone (ocupada pelo FAB protruso, desenhado por cima) e mostra só o
 * rótulo. Também clicável, com a mesma ação do FAB — o alvo de toque é a coluna inteira, e não só o
 * círculo, o que importa mais agora que ele é o único da barra.
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
            FluviBottomAppBar(modifier = Modifier, onClickEmbarque = {})
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                FabEmbarque(onClick = {})
            }
        }
    }
}