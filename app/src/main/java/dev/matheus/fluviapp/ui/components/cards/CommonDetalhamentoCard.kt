package dev.matheus.fluviapp.ui.components.cards

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.sampledata.dadosPassagemSample
import dev.matheus.fluviapp.ui.components.contents.DetalhamentoPassagemContent
import dev.matheus.fluviapp.ui.states.passagem.DetalhesPassagemState

@Composable
fun CommonDetalhamentoCard(
    modifier: Modifier,
    content: @Composable (Modifier) -> Unit = {}
) {
    Box(
        modifier = modifier
    ) {
        Image(
            modifier = modifier.matchParentSize(),
            painter = painterResource(R.drawable.logo2),
            contentDescription = stringResource(R.string.description_logo_1),
            contentScale = ContentScale.Fit,
            alpha = 0.3f
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