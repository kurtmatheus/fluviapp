package br.com.gruponaveg.ui.components.cards

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
import br.com.gruponaveg.R
import br.com.gruponaveg.model.screendata.DadosBalancoPassagem
import br.com.gruponaveg.sampledata.listaDadosBalancoPassagems
import br.com.gruponaveg.ui.components.contents.CommonRowDetalhamento
import br.com.gruponaveg.ui.components.texts.TextTitleWhiteItalic
import br.com.gruponaveg.ui.theme.NavegAppTheme

@Composable
fun BalancoNavioCard(
    modifier: Modifier,
    dadosBalancoPassagem: DadosBalancoPassagem,
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
                text = dadosBalancoPassagem.navio
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
                valor = dadosBalancoPassagem.preenchidasRedes
            )

            Column(
                modifier = modifier.padding(30.dp, 0.dp)
            ) {
                CommonRowDetalhamento(
                    modifier = modifier,
                    label = R.string.label_total_inteiras,
                    valor = dadosBalancoPassagem.preenchidasInteiras
                )
                CommonRowDetalhamento(
                    modifier = modifier,
                    label = R.string.label_total_meias,
                    valor = dadosBalancoPassagem.preenchidasMeias
                )
                CommonRowDetalhamento(
                    modifier = modifier,
                    label = R.string.label_total_gratuidade,
                    valor = dadosBalancoPassagem.preenchidasGratuidades
                )
            }

            CommonRowDetalhamento(
                modifier = modifier.padding(20.dp, 10.dp),
                label = R.string.label_veiculo_preview,
                valor = "${dadosBalancoPassagem.preenchidosVeiculo}/${dadosBalancoPassagem.capacidadeVeiculos}"
            )

            Column(
                modifier = modifier.padding(30.dp, 0.dp)
            ) {
                CommonRowDetalhamento(
                    modifier = modifier,
                    label = R.string.label_carros,
                    valor = dadosBalancoPassagem.totalCarros
                )
                CommonRowDetalhamento(
                    modifier = modifier,
                    label = R.string.label_motos,
                    valor = dadosBalancoPassagem.totalMotos
                )
                CommonRowDetalhamento(
                    modifier = modifier,
                    label = R.string.label_caminhoes,
                    valor = dadosBalancoPassagem.totalCaminhoes
                )
            }

            CommonRowDetalhamento(
                modifier = modifier.padding(20.dp, 10.dp),
                label = R.string.label_info_suites,
                valor = "${dadosBalancoPassagem.preenchidasSuitesGeral}/${dadosBalancoPassagem.capacidadeSuitesGeral}"
            )

            Column(
                modifier = modifier.padding(30.dp, 0.dp)
            ) {
                CommonRowDetalhamento(
                    modifier = modifier,
                    label = R.string.label_suites_2,
                    valor = "${dadosBalancoPassagem.preenchidasSuites2Pessoas}/${dadosBalancoPassagem.capacidadeSuites2Pessoas}"
                )
                CommonRowDetalhamento(
                    modifier = modifier,
                    label = R.string.label_suites_3,
                    valor = "${dadosBalancoPassagem.preenchidasSuites3Pessoas}/${dadosBalancoPassagem.capacidadeSuites3Pessoas}"
                )
            }

            CommonRowDetalhamento(
                modifier = modifier.padding(20.dp, 10.dp),
                label = R.string.label_info_camarotes,
                valor = "${dadosBalancoPassagem.preenchidosCamarotes}/${dadosBalancoPassagem.capacidadeCamarotes}"
            )

        }
    )
}

@Preview(showBackground = true)
@Composable
private fun BalancoNavioCardPreview() {
    NavegAppTheme {
        Box(
            modifier = Modifier.padding(10.dp)
        ) {
            BalancoNavioCard(
                modifier = Modifier,
                dadosBalancoPassagem = listaDadosBalancoPassagems.first()
            )
        }
    }
}