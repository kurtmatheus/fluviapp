package dev.matheus.fluviapp.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.model.screendata.DadosPassagem
import dev.matheus.fluviapp.sampledata.dadosPassagemSample
import dev.matheus.fluviapp.sampledata.dadosPassagemVeiculoSample
import dev.matheus.fluviapp.ui.components.contents.CommonRowDetalhamento
import dev.matheus.fluviapp.ui.components.texts.FluviWordmark
import dev.matheus.fluviapp.ui.components.texts.TextRegularBrown
import dev.matheus.fluviapp.ui.components.texts.TextSubTitleBrownBold
import dev.matheus.fluviapp.ui.components.texts.TextTitleBrownRegular
import dev.matheus.fluviapp.ui.theme.HeaderNavy
import dev.matheus.fluviapp.ui.theme.LightColors
import dev.matheus.fluviapp.ui.theme.SteelTeal

@Composable
fun ImpressaoDigitalDialog(
    modifier: Modifier = Modifier,
    dadosPassagem: DadosPassagem,
    onDismiss: () -> Unit,
    onProcessaImageBitmap: (ImageBitmap) -> Unit = {}
) {
    val graphicsLayer = rememberGraphicsLayer()

    // usePlatformDefaultWidth = false: sem a largura estreita padrão do Dialog, o ticket ocupa a
    // largura real e sai com as MESMAS proporções do preview (ConteudoTicketImpressao).
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Box de CAPTURA: grava o conteúdo desenhado no graphicsLayer p/ virar bitmap de impressão
        // (fundo branco). O conteúdo visual em si mora em ConteudoTicketImpressao (previewável).
        Box(
            modifier = modifier
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
            ConteudoTicketImpressao(
                modifier = modifier,
                dadosPassagem = dadosPassagem
            )

            LaunchedEffect(key1 = Unit) {
                // Espera o conteúdo ser medido/desenhado (layer com tamanho) ANTES de capturar.
                // Capturar no 1º frame pega o layout antes do posicionamento — daí a imagem saía
                // com elementos sobrepostos e sem a formatação vista no preview.
                while (graphicsLayer.size.width == 0 || graphicsLayer.size.height == 0) {
                    withFrameNanos { }
                }
                onProcessaImageBitmap(graphicsLayer.toImageBitmap())
            }
        }
    }
}

/**
 * Conteúdo visual do ticket digital, SEM o Dialog/captura — por isso é renderizável no @Preview
 * (o Dialog não aparece no preview do Android Studio). É aqui que se edita a aparência do bilhete.
 * Força [LightColors] + fundo branco: o ticket é impresso em papel claro, independente do tema do app.
 */
@Composable
private fun ConteudoTicketImpressao(
    modifier: Modifier = Modifier,
    dadosPassagem: DadosPassagem
) {
    MaterialTheme(colorScheme = LightColors) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(Color.White)
                .heightIn(min = 400.dp)
        ) {
            FluviWordmark(
                modifier = Modifier.matchParentSize(),
                fontSize = 64.sp,
                // Marca d'água no papel branco: gradiente ESCURO (o default claro some sobre fundo
                // claro). Mesma receita legível do CommonDetalhamentoCard (commits de identidade).
                alpha = 0.3f,
                fluviColor = SteelTeal,
                appGradient = listOf(SteelTeal, HeaderNavy, SteelTeal),
                strokeWidth = 3f,
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FluviWordmark(
                        modifier = Modifier.padding(16.dp),
                        fontSize = 28.sp,
                        // Legível no papel branco: gradiente escuro no lugar do default claro.
                        fluviColor = SteelTeal,
                        appGradient = listOf(SteelTeal, HeaderNavy, SteelTeal),
                        strokeWidth = 3f,
                    )
                    TextTitleBrownRegular(
                        modifier = Modifier.padding(40.dp),
                        text = "#${dadosPassagem.numero}"
                    )
                }

                TextTitleBrownRegular(
                    text = stringResource(R.string.card_title_detalhes_passagem)
                )

                TextRegularBrown(
                    text = "${dadosPassagem.empresaNome} - ${dadosPassagem.navio}"
                )

                val acomodacao = if (dadosPassagem.ehVeiculo) dadosPassagem.tipoVeiculo
                else dadosPassagem.acomodacao

                TextSubTitleBrownBold(
                    text = "[$acomodacao]"
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(15.dp)
                ) {
                    CommonRowDetalhamento(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        label = R.string.label_tipo_passagem,
                        valor = dadosPassagem.tipoPassagem
                    )

                    if (dadosPassagem.temGratuidade) {
                        CommonRowDetalhamento(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            label = R.string.label_tipo_gratuidade,
                            valor = dadosPassagem.tipoGratuidade
                        )
                    }

                    if (!dadosPassagem.ehVeiculo) {
                        SecaoPassageiros(dadosPassagem)
                    } else {
                        SecaoVeiculo(dadosPassagem)
                    }

                    CommonRowDetalhamento(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        label = R.string.label_valor_pago,
                        valor = dadosPassagem.valorAPagar
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TextSubTitleBrownBold(
                            text = "${dadosPassagem.origem}/${dadosPassagem.destino}"
                        )

                        TextRegularBrown(
                            text = "${dadosPassagem.dataViagem} - ${dadosPassagem.horaViagem}"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SecaoPassageiros(
    dadosPassagem: DadosPassagem,
    modifier: Modifier = Modifier
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
private fun SecaoVeiculo(
    dadosPassagem: DadosPassagem,
    modifier: Modifier = Modifier
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

// Previews chamam o CONTEÚDO direto (não o Dialog, que não renderiza no preview). heightDp dá
// espaço vertical p/ o ticket inteiro aparecer. É por aqui que se ajusta o visual do bilhete.
@Preview(name = "Passageiro", showBackground = true, heightDp = 720)
@Composable
private fun ImpressaoDigitalPassageiroPreview() {
    ConteudoTicketImpressao(dadosPassagem = dadosPassagemSample)
}

@Preview(name = "Veículo", showBackground = true, heightDp = 720)
@Composable
private fun ImpressaoDigitalVeiculoPreview() {
    ConteudoTicketImpressao(dadosPassagem = dadosPassagemVeiculoSample)
}