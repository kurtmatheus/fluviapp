package br.com.gruponaveg.ui.components.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.gruponaveg.R
import br.com.gruponaveg.model.screendata.DadosPassagem
import br.com.gruponaveg.sampledata.dadosPassagemSample
import br.com.gruponaveg.sampledata.dadosPassagemVeiculoSample
import br.com.gruponaveg.ui.components.texts.TextBoldNavyBlue
import br.com.gruponaveg.ui.components.texts.TextBoldWhiteItalic
import br.com.gruponaveg.ui.components.texts.TextRegularWhite
import br.com.gruponaveg.ui.components.texts.TextTitleWhiteItalic

@Composable
fun PassagemPreviewCard(
    modifier: Modifier,
    dadosPassagem: DadosPassagem,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .padding(20.dp)
            .fillMaxWidth()
            .height(200.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            disabledContainerColor = MaterialTheme.colorScheme.secondary
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(20.dp, 0.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(5.dp,0.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextTitleWhiteItalic(
                    text = dadosPassagem.navio
                )
                Card(
                    modifier = modifier
                        .widthIn(min = 50.dp),
                    shape = RoundedCornerShape(15)
                ) {
                    TextBoldNavyBlue(
                        modifier = modifier
                            .padding(5.dp)
                            .align(Alignment.CenterHorizontally),
                        text = "#${dadosPassagem.numero}"
                    )
                }
            }
            if (dadosPassagem.ehVeiculo) {
                RowPassagemPreview(
                    modifier = modifier,
                    icon = R.drawable.ic_carro_75,
                    contentDescription = R.string.description_veiculo,
                    labelTop = R.string.label_veiculo_preview,
                    textTop = dadosPassagem.modeloVeiculo,
                    labelMid = R.string.label_placa_preview,
                    textMid = dadosPassagem.placaVeiculo,
                    labelBot = R.string.label_cor_preview,
                    textBot = dadosPassagem.corVeiculo
                )
            } else {
                RowPassagemPreview(
                    modifier = modifier,
                    icon = R.drawable.ic_bilhete_75,
                    contentDescription = R.string.description_icon_navio,
                    labelTop = R.string.label_nome_preview,
                    textTop = dadosPassagem.nomePassageiro1,
                    labelMid = R.string.label_documento_preview,
                    textMid = dadosPassagem.documentoPassageiro1,
                    labelBot = R.string.label_data_nascimento_preview,
                    textBot = dadosPassagem.dataNascimento1
                )
            }
        }
    }
}

@Composable
private fun RowPassagemPreview(
    modifier: Modifier,
    icon: Int,
    contentDescription: Int,
    labelTop: Int,
    textTop: String,
    labelMid: Int,
    textMid: String,
    labelBot: Int,
    textBot: String,
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Icon(
            modifier = modifier,
            painter = painterResource(id = icon),
            contentDescription = stringResource(id = contentDescription),
            tint = MaterialTheme.colorScheme.onPrimary
        )

        Box {
            VerticalDivider(
                modifier = modifier
                    .height(80.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row {
                TextBoldWhiteItalic(
                    text = "${stringResource(id = labelTop)}: "
                )
                TextRegularWhite(text = textTop)
            }
            Row {
                TextBoldWhiteItalic(
                    text = "${stringResource(id = labelMid)}: "
                )
                TextRegularWhite(text = textMid)
            }
            Row {
                TextBoldWhiteItalic(
                    text = "${stringResource(id = labelBot)}: "
                )
                TextRegularWhite(text = textBot)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PassagemPreviewCardPreview() {
    PassagemPreviewCard(
        modifier = Modifier,
        dadosPassagem = dadosPassagemSample,
        onClick = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun PassagemVeiculoPreviewCardPreview() {
    PassagemPreviewCard(
        modifier = Modifier,
        dadosPassagem = dadosPassagemVeiculoSample,
        onClick = {}
    )
}