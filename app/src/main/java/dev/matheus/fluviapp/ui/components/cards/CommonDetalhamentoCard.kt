package dev.matheus.fluviapp.ui.components.cards

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import dev.matheus.fluviapp.ui.components.texts.FluviWordmark
import dev.matheus.fluviapp.sampledata.dadosPassagemSample
import dev.matheus.fluviapp.ui.components.contents.DetalhamentoPassagemContent
import dev.matheus.fluviapp.ui.states.passagem.DetalhesPassagemState
import dev.matheus.fluviapp.ui.theme.HeaderNavy
import dev.matheus.fluviapp.ui.theme.SteelTeal

@Composable
fun CommonDetalhamentoCard(
    modifier: Modifier,
    content: @Composable (Modifier) -> Unit = {}
) {
    Box(
        modifier = modifier
    ) {
        FluviWordmark(
            modifier = modifier.matchParentSize(),
            fontSize = 56.sp,
            alpha = 0.3f,
            // Marca d'água sobre papel claro: gradiente escuro (o default claro sumia). Alpha baixo
            // mantém sutil, mas agora visível.
            fluviColor = SteelTeal,
            appGradient = listOf(SteelTeal, HeaderNavy, SteelTeal),
            strokeWidth = 3f,
        )

        content(modifier)
    }
}


@Preview(showBackground = true)
@Composable
private fun DetalhesPassagemCardPreview() {
    CommonDetalhamentoCard(
        modifier = Modifier
    ) {
        DetalhamentoPassagemContent(
            modifier = it,
            state = DetalhesPassagemState(
                dadosPassagem = dadosPassagemSample,
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DetalhesPassagemCardComVeiculoPreview() {
    CommonDetalhamentoCard(
        modifier = Modifier)
    {
        DetalhamentoPassagemContent(
            modifier = it,
            state = DetalhesPassagemState(
                dadosPassagem = dadosPassagemSample,
                isShowAreaVeiculo = true
            )
        )
    }
}