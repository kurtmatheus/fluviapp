package dev.matheus.fluviapp.ui.components.cards

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.screendata.DadosContagemPassagem
import dev.matheus.fluviapp.sampledata.listaDadosContagemPassagems
import dev.matheus.fluviapp.ui.components.contents.CommonRowDetalhamento
import dev.matheus.fluviapp.ui.components.texts.TextTitleWhiteItalic
import dev.matheus.fluviapp.ui.theme.FluviAppTheme

@Composable
fun ContagemNavioCard(
    modifier: Modifier,
    dadosContagemPassagem: DadosContagemPassagem,
) {
    CommonExpandableCard(
        modifier = modifier,
        roundedCornerShape = true,
        contentHeader = { dropDownIcon ->
            Icon(
                modifier = Modifier
                    .height(25.dp),
                painter = painterResource(id = R.drawable.ic_navio_75),
                contentDescription = stringResource(id = R.string.description_icon_navio),
                tint = MaterialTheme.colorScheme.onPrimary
            )

            TextTitleWhiteItalic(
                text = dadosContagemPassagem.navio
            )

            Icon(
                modifier = Modifier
                    .height(25.dp),
                imageVector = dropDownIcon,
                contentDescription = stringResource(id = R.string.description_icon_expand),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        },
        contentExpand = {
            CommonRowDetalhamento(
                modifier = modifier.padding(20.dp, 10.dp),
                label = R.string.label_info_redes,
                valor = dadosContagemPassagem.preenchidasRedes
            )

            Column(
                modifier = modifier.padding(30.dp, 0.dp)
            ) {
                CommonRowDetalhamento(
                    modifier = modifier,
                    label = R.string.label_total_inteiras,
                    valor = dadosContagemPassagem.preenchidasInteiras
                )
                CommonRowDetalhamento(
                    modifier = modifier,
                    label = R.string.label_total_meias,
                    valor = dadosContagemPassagem.preenchidasMeias
                )
                CommonRowDetalhamento(
                    modifier = modifier,
                    label = R.string.label_total_gratuidade,
                    valor = dadosContagemPassagem.preenchidasGratuidades
                )
            }

            CommonRowDetalhamento(
                modifier = modifier.padding(20.dp, 10.dp),
                label = R.string.label_veiculo_preview,
                valor = "${dadosContagemPassagem.preenchidosVeiculo}/${dadosContagemPassagem.capacidadeVeiculos}"
            )

            Column(
                modifier = modifier.padding(30.dp, 0.dp)
            ) {
                CommonRowDetalhamento(
                    modifier = modifier,
                    label = R.string.label_carros,
                    valor = dadosContagemPassagem.totalCarros
                )
                CommonRowDetalhamento(
                    modifier = modifier,
                    label = R.string.label_motos,
                    valor = dadosContagemPassagem.totalMotos
                )
                CommonRowDetalhamento(
                    modifier = modifier,
                    label = R.string.label_caminhoes,
                    valor = dadosContagemPassagem.totalCaminhoes
                )
                CommonRowDetalhamento(
                    modifier = modifier,
                    label = R.string.label_carretas,
                    valor = dadosContagemPassagem.totalCarretas
                )
            }

            CommonRowDetalhamento(
                modifier = modifier.padding(20.dp, 10.dp),
                label = R.string.label_info_suites,
                valor = "${dadosContagemPassagem.preenchidasSuitesGeral}/${dadosContagemPassagem.capacidadeSuitesGeral}"
            )

            Column(
                modifier = modifier.padding(30.dp, 0.dp)
            ) {
                CommonRowDetalhamento(
                    modifier = modifier,
                    label = R.string.label_suites_2,
                    valor = "${dadosContagemPassagem.preenchidasSuites2Pessoas}/${dadosContagemPassagem.capacidadeSuites2Pessoas}"
                )
                CommonRowDetalhamento(
                    modifier = modifier,
                    label = R.string.label_suites_3,
                    valor = "${dadosContagemPassagem.preenchidasSuites3Pessoas}/${dadosContagemPassagem.capacidadeSuites3Pessoas}"
                )
            }

            CommonRowDetalhamento(
                modifier = modifier.padding(20.dp, 10.dp),
                label = R.string.label_info_camarotes,
                valor = "${dadosContagemPassagem.preenchidosCamarotes}/${dadosContagemPassagem.capacidadeCamarotes}"
            )

        }
    )
}

@Preview(showBackground = true)
@Composable
private fun ContagemNavioCardPreview() {
    FluviAppTheme {
        Box(
            modifier = Modifier.padding(10.dp)
        ) {
            ContagemNavioCard(
                modifier = Modifier,
                dadosContagemPassagem = listaDadosContagemPassagems.first()
            )
        }
    }
}