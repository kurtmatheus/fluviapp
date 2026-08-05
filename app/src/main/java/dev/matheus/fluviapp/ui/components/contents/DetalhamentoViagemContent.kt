package dev.matheus.fluviapp.ui.components.contents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.screendata.DadosViagemCard
import dev.matheus.fluviapp.sampledata.listaDadosDadosViagemHomeSampleCards
import dev.matheus.fluviapp.ui.components.texts.FluviWordmark
import dev.matheus.fluviapp.ui.components.texts.TextRegularBrownItalic
import dev.matheus.fluviapp.ui.theme.HeaderNavy
import dev.matheus.fluviapp.ui.theme.SteelTeal

@Composable
fun DetalhamentoViagemContent(
    modifier: Modifier,
    dados: DadosViagemCard,
) {
    Column(
        modifier = modifier
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FluviWordmark(
                modifier = modifier.height(65.dp),
                fontSize = 26.sp,
                // Superfície clara (papel): gradiente escuro navy/teal p/ legibilidade — o default
                // (teal→amarelo→ciano) é claro e some sobre fundo claro.
                fluviColor = SteelTeal,
                appGradient = listOf(SteelTeal, HeaderNavy, SteelTeal),
                strokeWidth = 3f,
            )

            TextRegularBrownItalic(
                text = stringResource(id = R.string.card_title_detalhes_viagem)
            )

        }

        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CommonRowDetalhamento(
                modifier = modifier,
                label = R.string.label_embarcacao,
                valor = dados.embarcacao
            )

            CommonRowDetalhamento(
                modifier = modifier,
                label = R.string.label_trecho_origem,
                valor = dados.origem
            )

            CommonRowDetalhamento(
                modifier = modifier,
                label = R.string.label_trecho_destino,
                valor = dados.destino
            )
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onBackground
            )

//            Column(
//                modifier = modifier.padding(5.dp, 0.dp)
//            ) {
//                CommonRowDetalhamento(
//                    modifier = modifier,
//                    label = R.string.label_total_inteiras,
//                    dado = dados.preenchidasInteiras
//                )
//                CommonRowDetalhamento(
//                    modifier = modifier,
//                    label = R.string.label_total_meias,
//                    dado = dados.preenchidasMeias
//                )
//                CommonRowDetalhamento(
//                    modifier = modifier,
//                    label = R.string.label_total_gratuidade,
//                    dado = dados.preenchidasGratuidades
//                )
//            }

            CommonRowDetalhamento(
                modifier = modifier,
                label = R.string.label_capac_veiculos,
                valor = dados.capacidadeVeiculos
            )

//            Column(
//                modifier = modifier.padding(5.dp, 0.dp)
//            ) {
//                CommonRowDetalhamento(
//                    modifier = modifier,
//                    label = R.string.label_carros,
//                    dado = dados.totalCarros
//                )
//
//                CommonRowDetalhamento(
//                    modifier = modifier,
//                    label = R.string.label_motos,
//                    dado = dados.totalMotos
//                )
//
//                CommonRowDetalhamento(
//                    modifier = modifier,
//                    label = R.string.label_caminhoes,
//                    dado = dados.totalCaminhoes
//                )
//            }

            CommonRowDetalhamento(
                modifier = modifier,
                label = R.string.label_capac_suite,
                valor = dados.capacidadeSuites
            )

            Column(
                modifier = modifier.padding(5.dp, 0.dp)
            ) {
                CommonRowDetalhamento(
                    modifier = modifier,
                    label = R.string.label_suites_2,
                    valor = dados.capacidadeSuites2Pessoas
                )

                CommonRowDetalhamento(
                    modifier = modifier,
                    label = R.string.label_suites_3,
                    valor = dados.capacidadeSuites3Pessoas
                )
            }

            CommonRowDetalhamento(
                modifier = modifier,
                label = R.string.label_capac_camarotes,
                valor = dados.capacidadeCamarotes
            )

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onBackground
            )

        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DetalhamentoPassagemContentPreview() {
    DetalhamentoViagemContent(
        modifier = Modifier,
        dados = listaDadosDadosViagemHomeSampleCards[3]
    )
}
