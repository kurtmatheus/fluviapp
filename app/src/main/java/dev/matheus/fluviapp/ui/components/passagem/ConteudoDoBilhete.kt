package dev.matheus.fluviapp.ui.components.passagem

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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.matheus.fluviapp.services.printerservice.qrcode.QRCodeGenerator
import dev.matheus.fluviapp.ui.components.texts.FluviWordmark
import dev.matheus.fluviapp.ui.components.texts.TextRegularBrown
import dev.matheus.fluviapp.ui.components.texts.TextRegularBrownItalic
import dev.matheus.fluviapp.ui.components.texts.TextSubTitleBrownBold
import dev.matheus.fluviapp.ui.components.texts.TextTitleBrownRegular
import dev.matheus.fluviapp.ui.states.passagem.BilheteDigital
import dev.matheus.fluviapp.ui.states.passagem.PassageiroDoBilhete
import dev.matheus.fluviapp.ui.theme.HeaderNavy
import dev.matheus.fluviapp.ui.theme.LightColors
import dev.matheus.fluviapp.ui.theme.SteelTeal
import dev.matheus.fluviapp.ui.theme.marcaDaAgencia

/**
 * **O bilhete, desenhado** — e é este mesmo desenho que vira imagem.
 *
 * O caminho antigo renderizava **duas vezes**: uma num `Dialog` para o operador ver, outra idêntica e
 * escondida para capturar. Aqui a pré-visualização **é** o que se grava, o que elimina de vez a chance de as
 * duas divergirem — e é o que a decisão de *"pré-visualizar no mesmo ato de emitir"* torna possível.
 *
 * ### Três coisas que este composable força, e todas por serem sobre papel
 *
 * - **[LightColors] e fundo branco explícitos**: o bilhete é lido claro, e é guardado como imagem — ele não
 *   segue o tema do app, senão o arquivo sairia preto para quem usa modo escuro;
 * - **a marca da agência assina** (ADR-0015 §5): logo no topo, logo 2 como marca d'água. Sem marca própria,
 *   assina o FluviApp — o documento nunca sai sem assinatura;
 * - **o QR fecha o rodapé**, fora da coluna de dados: quem valida aponta a câmera para o pé do bilhete, não
 *   procura o código no meio do texto (ADR-0012).
 */
@Composable
fun ConteudoDoBilhete(
    bilhete: BilheteDigital,
    modifier: Modifier = Modifier,
) {
    val marca = marcaDaAgencia(bilhete.agencia)

    MaterialTheme(colorScheme = LightColors) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(Color.White)
                .heightIn(min = 400.dp),
        ) {
            if (marca != null) {
                Image(
                    painter = painterResource(marca.marcaDagua),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.matchParentSize().padding(48.dp).alpha(0.12f),
                )
            } else {
                FluviWordmark(
                    modifier = Modifier.matchParentSize(),
                    fontSize = 64.sp,
                    alpha = 0.3f,
                    fluviColor = SteelTeal,
                    appGradient = listOf(SteelTeal, HeaderNavy, SteelTeal),
                    strokeWidth = 3f,
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    if (marca != null) {
                        Image(
                            painter = painterResource(marca.logoTopo),
                            contentDescription = bilhete.agencia,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(width = 140.dp, height = 56.dp),
                        )
                    } else {
                        FluviWordmark(
                            modifier = Modifier,
                            fontSize = 28.sp,
                            fluviColor = SteelTeal,
                            appGradient = listOf(SteelTeal, HeaderNavy, SteelTeal),
                            strokeWidth = 3f,
                        )
                    }
                    TextTitleBrownRegular(text = bilhete.numero)
                }

                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onBackground)

                TextSubTitleBrownBold(text = bilhete.travessia)
                TextRegularBrown(text = bilhete.partida)
                if (bilhete.embarcacao.isNotBlank()) TextRegularBrown(text = bilhete.embarcacao)
                TextSubTitleBrownBold(text = "[${bilhete.bilhete}]")
                // A gratuidade aparece por extenso: é ela que a fiscalização confere contra a credencial.
                bilhete.gratuidade?.let { TextRegularBrownItalic(text = it) }

                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

                bilhete.passageiros.forEach { PassageiroNoBilhete(it) }

                bilhete.veiculo?.let { veiculo ->
                    TextSubTitleBrownBold(text = veiculo.placa)
                    TextRegularBrown(
                        text = listOfNotNull(veiculo.classe, veiculo.modelo, veiculo.cor)
                            .filter { it.isNotBlank() }
                            .joinToString(" · "),
                    )
                }

                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

                TextSubTitleBrownBold(text = bilhete.total)
                bilhete.observacao?.let { TextRegularBrownItalic(text = it) }

                QrDeEmbarque(idPassagem = bilhete.idPassagem)
            }
        }
    }
}

/**
 * Nome e documento **por inteiro** — o bilhete não mascara (decisão do analista): ele vai para a mão de quem
 * já sabe o próprio número, e é conferido contra a identidade na doca.
 */
@Composable
private fun PassageiroNoBilhete(passageiro: PassageiroDoBilhete) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TextRegularBrownItalic(text = passageiro.papel)
        TextSubTitleBrownBold(text = passageiro.nome)
        TextRegularBrown(text = passageiro.documento)
    }
}

/**
 * O QR carrega **o id da passagem**, e nada mais (ADR-0012): ele é ponteiro, e quem valida lê o documento ao
 * vivo no servidor. É por isso que um bilhete fotografado nunca fica "velho" — o que ele leva é o endereço,
 * não o estado. Sem id não há o que ler, então não há QR.
 */
@Composable
private fun QrDeEmbarque(idPassagem: String) {
    if (idPassagem.isBlank()) return

    val qr = remember(idPassagem) { QRCodeGenerator().generate(idPassagem, size = 300) }

    Image(
        bitmap = qr.asImageBitmap(),
        contentDescription = null,
        modifier = Modifier.padding(top = 12.dp).size(150.dp).background(Color.White),
    )
}

/**
 * **A captura**: desenha o bilhete e grava exatamente o que foi desenhado.
 *
 * A espera antes de gravar não é superstição — é defeito já pago uma vez. Capturar no primeiro frame pega o
 * layout **antes do posicionamento**, e a imagem sai com os elementos sobrepostos; o comentário estava no
 * código antigo e a linha volta com ele.
 *
 * O fundo branco é desenhado **dentro** da gravação porque o `graphicsLayer` grava o que se pinta: sem ele, o
 * PNG sai com fundo transparente e vira um bilhete ilegível em qualquer visualizador que use tema escuro.
 */
@Composable
fun BilheteCapturavel(
    bilhete: BilheteDigital,
    aoCapturar: (ImageBitmap) -> Unit,
    modifier: Modifier = Modifier,
) {
    val camada = rememberGraphicsLayer()

    Box(
        modifier = modifier.drawWithContent {
            camada.record {
                this@drawWithContent.drawRect(color = Color.White)
                this@drawWithContent.drawContent()
            }
            drawLayer(camada)
        },
    ) {
        ConteudoDoBilhete(bilhete = bilhete)

        LaunchedEffect(bilhete.idPassagem) {
            while (camada.size.width == 0 || camada.size.height == 0) withFrameNanos { }
            aoCapturar(camada.toImageBitmap())
        }
    }
}

@Preview
@Composable
private fun ConteudoDoBilhetePreview() {
    ConteudoDoBilhete(
        bilhete = BilheteDigital(
            idPassagem = "passagem-modelo-1",
            numero = "#41",
            agencia = "NAVEG",
            travessia = "Porto de Val-de-Cães · Belém/PA → Porto de Parintins · Parintins/AM",
            partida = "Terça-feira, 18/08 · 18:00",
            embarcacao = "F/B Modelo",
            bilhete = "Suíte · 2 pessoas",
            passageiros = listOf(
                PassageiroDoBilhete("Titular", "Ana Ribeiro", "CPF 529.982.247-25"),
                PassageiroDoBilhete("Acompanhante", "Bruno Costa", "CPF 111.444.777-35"),
            ),
            total = "R$ 300,00",
        ),
    )
}