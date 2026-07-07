package br.com.gruponaveg.ui.components.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import br.com.gruponaveg.R
import br.com.gruponaveg.model.screendata.DadosPassagem
import br.com.gruponaveg.sampledata.dadosPassagemSample
import br.com.gruponaveg.sampledata.dadosPassagemVeiculoSample
import br.com.gruponaveg.ui.components.contents.CommonRowDetalhamento
import br.com.gruponaveg.ui.components.texts.TextRegularBrown
import br.com.gruponaveg.ui.components.texts.TextSubTitleBrownBold
import br.com.gruponaveg.ui.components.texts.TextTitleBrownRegular
import br.com.gruponaveg.ui.screens.forms.CommonScreenNoBottom
import kotlinx.coroutines.launch

@Composable
fun ImpressaoDigitalDialog(
    modifier: Modifier = Modifier,
    dadosPassagem: DadosPassagem,
    onDismiss: () -> Unit,
    onProcessaImageBitmap: (ImageBitmap) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()

    Dialog(
        onDismissRequest = onDismiss
    ) {
        val logo1 = ImageBitmap.imageResource(id = R.drawable.logo1)
        val logo2 = ImageBitmap.imageResource(id = R.drawable.logo2)

        Box(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 400.dp)
                .drawWithContent {
                    graphicsLayer.record {
                        this@drawWithContent.drawRect(
                            color = Color.White
                        )
                        this@drawWithContent.drawContent()
                    }
                    drawLayer(graphicsLayer)
                }
        ) {
            Image(
                modifier = modifier.matchParentSize(),
                bitmap = logo2,
                alignment = Alignment.Center,
                alpha = 0.2f,
                contentDescription = stringResource(R.string.description_logo_do_app)
            )

            Column(
                modifier = modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Image(
                        modifier = modifier.offset(y = (-40).dp),
                        bitmap = logo1,
                        alignment = Alignment.Center,
                        contentDescription = stringResource(R.string.description_logo_do_app)
                    )
                    TextTitleBrownRegular(
                        modifier = modifier.padding(40.dp),
                        text = "#${dadosPassagem.numero}"
                    )
                }

                TextTitleBrownRegular(
                    modifier = modifier.offset(y = calculaOffset()),
                    text = stringResource(R.string.card_title_detalhes_passagem)
                )

                TextRegularBrown(
                    modifier = modifier.offset(y = calculaOffset(10)),
                    text = "${dadosPassagem.empresaNome} - ${dadosPassagem.navio}"
                )

                val acomodacao = if (dadosPassagem.ehVeiculo) dadosPassagem.tipoVeiculo
                else dadosPassagem.acomodacao

                TextSubTitleBrownBold(
                    modifier = modifier.offset(y = calculaOffset(20)),
                    text = "[$acomodacao]"
                )

                Column(
                    modifier = modifier
                        .fillMaxWidth()
                        .offset(y = calculaOffset(40)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(15.dp)
                ) {
                    CommonRowDetalhamento(
                        modifier = modifier
                            .padding(horizontal = 10.dp),
                        label = R.string.label_tipo_passagem,
                        valor = dadosPassagem.tipoPassagem
                    )

                    if (dadosPassagem.temGratuidade) {
                        CommonRowDetalhamento(
                            modifier = modifier
                                .padding(horizontal = 10.dp),
                            label = R.string.label_tipo_gratuidade,
                            valor = dadosPassagem.tipoGratuidade
                        )
                    }

                    if (!dadosPassagem.ehVeiculo) {
                        SecaoPassageiros(modifier, dadosPassagem)
                    } else {
                        SecaoVeiculo(modifier, dadosPassagem)
                    }

                    CommonRowDetalhamento(
                        modifier = modifier
                            .padding(horizontal = 10.dp),
                        label = R.string.label_valor_pago,
                        valor = dadosPassagem.valorAPagar
                    )

                    Column(
                        modifier = modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TextSubTitleBrownBold(
                            modifier = modifier,
                            text = "${dadosPassagem.origem}/${dadosPassagem.destino}"
                        )

                        TextRegularBrown(
                            modifier = modifier,
                            text = "${dadosPassagem.dataViagem} - ${dadosPassagem.horaViagem}"
                        )
                    }
                }
            }
            LaunchedEffect(key1 = Unit) {
                coroutineScope.launch {
                    onProcessaImageBitmap(graphicsLayer.toImageBitmap())
                }
            }
        }
    }
}

@Composable
private fun SecaoPassageiros(
    modifier: Modifier,
    dadosPassagem: DadosPassagem
) {
    CommonRowDetalhamento(
        modifier = modifier
            .padding(horizontal = 10.dp),
        label = R.string.label_nome_passageiro,
        valor = dadosPassagem.nomePassageiro1
    )

    CommonRowDetalhamento(
        modifier = modifier
            .padding(horizontal = 10.dp),
        label = R.string.label_documento,
        valor = dadosPassagem.documentoPassageiro1
    )

    if (dadosPassagem.tem2Pessoas) {
        CommonRowDetalhamento(
            modifier = modifier
                .padding(horizontal = 10.dp),
            label = R.string.label_nome_passageiro,
            valor = dadosPassagem.nomePassageiro2
        )

        CommonRowDetalhamento(
            modifier = modifier
                .padding(horizontal = 10.dp),
            label = R.string.label_documento,
            valor = dadosPassagem.documentoPassageiro2
        )

        if (dadosPassagem.tem3Pessoas) {
            CommonRowDetalhamento(
                modifier = modifier
                    .padding(horizontal = 10.dp),
                label = R.string.label_nome_passageiro,
                valor = dadosPassagem.nomePassageiro3
            )

            CommonRowDetalhamento(
                modifier = modifier
                    .padding(horizontal = 10.dp),
                label = R.string.label_documento,
                valor = dadosPassagem.documentoPassageiro3
            )
        }
    }
}

@Composable
fun SecaoVeiculo(
    modifier: Modifier,
    dadosPassagem: DadosPassagem
) {
    if (dadosPassagem.temResponsavel) {
        CommonRowDetalhamento(
            modifier = modifier
                .padding(horizontal = 10.dp),
            label = R.string.label_nome_responsavel,
            valor = dadosPassagem.nomeResponsavelRetirada
        )

        CommonRowDetalhamento(
            modifier = modifier
                .padding(horizontal = 10.dp),
            label = R.string.label_documento,
            valor = dadosPassagem.numeroDocumentoResponsavelRetirada
        )
    }

    CommonRowDetalhamento(
        modifier = modifier
            .padding(horizontal = 10.dp),
        label = R.string.label_modelo_veiculo,
        valor = dadosPassagem.modeloVeiculo
    )

    CommonRowDetalhamento(
        modifier = modifier
            .padding(horizontal = 10.dp),
        label = R.string.label_placa_veículo,
        valor = dadosPassagem.placaVeiculo
    )

    CommonRowDetalhamento(
        modifier = modifier
            .padding(horizontal = 10.dp),
        label = R.string.label_cor_veículo,
        valor = dadosPassagem.corVeiculo
    )

}

private fun calculaOffset(soma: Int = 0) = (-90 + soma).dp

@Preview(showBackground = true)
@Composable
private fun ImpressaoDigitalDialogPreview() {
    CommonScreenNoBottom(
        titleTopAppBar = R.string.subtitle_nova_passagem,
        titleTopContent = R.string.subtitle_menu_operacoes,
        isShowRightIcon = false,
        hasRefresh = false,
        isRefreshing = false
    ) { modifier, _ ->
        ImpressaoDigitalDialog(
            modifier = modifier,
            dadosPassagem = dadosPassagemSample,
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ImpressaoDigitalDialogVeiculoPreview() {
    CommonScreenNoBottom(
        titleTopAppBar = R.string.subtitle_nova_passagem,
        titleTopContent = R.string.subtitle_menu_operacoes,
        isShowRightIcon = false,
        hasRefresh = false,
        isRefreshing = false
    ) { modifier, _ ->
        ImpressaoDigitalDialog(
            modifier = modifier,
            dadosPassagem = dadosPassagemVeiculoSample,
            onDismiss = {}
        )
    }
}