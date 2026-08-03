package dev.matheus.fluviapp.ui.components.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.operacoes.Agencia
import dev.matheus.fluviapp.domain.screendata.DadosPassagem
import dev.matheus.fluviapp.sampledata.dadosPassagemSample
import dev.matheus.fluviapp.sampledata.dadosPassagemVeiculoSample
import dev.matheus.fluviapp.ui.components.contents.CommonRowDetalhamento
import dev.matheus.fluviapp.ui.components.texts.FluviWordmark
import dev.matheus.fluviapp.ui.components.texts.TextRegularBrown
import dev.matheus.fluviapp.ui.components.texts.TextSubTitleBrownBold
import dev.matheus.fluviapp.ui.components.texts.TextTitleBrownRegular
import dev.matheus.fluviapp.services.printerservice.qrcode.QRCodeGenerator
import dev.matheus.fluviapp.ui.theme.HeaderNavy
import dev.matheus.fluviapp.ui.theme.LightColors
import dev.matheus.fluviapp.ui.theme.SteelTeal
import dev.matheus.fluviapp.ui.theme.marcaDaAgencia

/**
 * Emissão da **passagem digital**: renderiza o bilhete, captura como imagem e devolve o bitmap para
 * quem vai persistir/compartilhar. Não é "impressão" — impressão é o caminho do papel
 * (`ImpressaoPassagem`, térmica). Aqui a passagem *nasce* em imagem, e o nome passou a dizer isso.
 */
@Composable
fun EmissaoPassagemDigitalDialog(
    modifier: Modifier = Modifier,
    dadosPassagem: DadosPassagem,
    onDismiss: () -> Unit,
    onProcessaImageBitmap: (ImageBitmap) -> Unit = {}
) {
    val graphicsLayer = rememberGraphicsLayer()

    // usePlatformDefaultWidth = false: sem a largura estreita padrão do Dialog, o bilhete ocupa a
    // largura real e sai com as MESMAS proporções do preview (ConteudoPassagemDigital).
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Box de CAPTURA: grava o conteúdo desenhado no graphicsLayer p/ virar bitmap
        // (fundo branco). O conteúdo visual em si mora em ConteudoPassagemDigital (previewável).
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
            ConteudoPassagemDigital(
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
 * Conteúdo visual da passagem digital, SEM o Dialog/captura — por isso é renderizável no @Preview
 * (o Dialog não aparece no preview do Android Studio). É aqui que se edita a aparência do bilhete.
 * Força [LightColors] + fundo branco: o bilhete é lido claro, independente do tema do app.
 *
 * A identidade é a da **agência emissora** (ADR-0015 §5), resolvida do snapshot da passagem: logo 1 no
 * topo assinando o documento, logo 2 como marca d'água. Sem marca própria (agência coringa ou nome
 * livre), assina o **FluviApp** — que é o que o app já fazia e segue sendo o default.
 */
@Composable
private fun ConteudoPassagemDigital(
    modifier: Modifier = Modifier,
    dadosPassagem: DadosPassagem
) {
    val marca = marcaDaAgencia(dadosPassagem.agencia)

    MaterialTheme(colorScheme = LightColors) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(Color.White)
                .heightIn(min = 400.dp)
        ) {
            if (marca != null) {
                Image(
                    painter = painterResource(marca.marcaDagua),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .matchParentSize()
                        .padding(48.dp)
                        .alpha(0.12f),
                )
            } else {
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
            }

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
                    if (marca != null) {
                        Image(
                            painter = painterResource(marca.logoTopo),
                            contentDescription = dadosPassagem.agencia,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .padding(16.dp)
                                .size(width = 140.dp, height = 56.dp),
                        )
                    } else {
                        FluviWordmark(
                            modifier = Modifier.padding(16.dp),
                            fontSize = 28.sp,
                            // Legível no papel branco: gradiente escuro no lugar do default claro.
                            fluviColor = SteelTeal,
                            appGradient = listOf(SteelTeal, HeaderNavy, SteelTeal),
                            strokeWidth = 3f,
                        )
                    }
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

                // O QR fecha o bilhete, centralizado no rodapé: é o que o validador lê na doca
                // (ADR-0012). Fica FORA da coluna de detalhes de propósito — quem embarca aponta a
                // câmera para o pé do bilhete, não procura o código no meio dos dados.
                QrCodeEmbarque(idPassagem = dadosPassagem.idPassagem)
            }
        }
    }
}

/**
 * QR de embarque no bilhete digital — paridade com o físico (ImpressaoHelper.printQrCode), que já
 * codifica o id da passagem. É um ponteiro (ADR-0012): o validador lê o doc ao vivo pelo id. Fundo
 * branco fixo (o bilhete é claro). Sem id não há o que ler, então não há QR.
 */
@Composable
private fun QrCodeEmbarque(
    idPassagem: String,
    modifier: Modifier = Modifier
) {
    if (idPassagem.isBlank()) return
    val qrBitmap = remember(idPassagem) {
        runCatching { QRCodeGenerator().generate(idPassagem, 320) }.getOrNull()
    } ?: return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(
            bitmap = qrBitmap.asImageBitmap(),
            contentDescription = stringResource(R.string.label_qr_embarque),
            modifier = Modifier
                .size(180.dp)
                .background(Color.White)
        )
        TextRegularBrown(text = stringResource(R.string.label_qr_embarque))
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
// espaço vertical p/ o bilhete inteiro aparecer, QR incluído. É por aqui que se ajusta o visual.
@Preview(name = "Agência com marca", showBackground = true, heightDp = 940)
@Composable
private fun PassagemDigitalComMarcaPreview() {
    ConteudoPassagemDigital(dadosPassagem = dadosPassagemSample.copy(agencia = Agencia.MATRIZ.name))
}

@Preview(name = "Sem marca — FluviApp", showBackground = true, heightDp = 940)
@Composable
private fun PassagemDigitalSemMarcaPreview() {
    ConteudoPassagemDigital(dadosPassagem = dadosPassagemSample.copy(agencia = Agencia.AUTONOMO.name))
}

@Preview(name = "Veículo", showBackground = true, heightDp = 940)
@Composable
private fun PassagemDigitalVeiculoPreview() {
    ConteudoPassagemDigital(dadosPassagem = dadosPassagemVeiculoSample)
}