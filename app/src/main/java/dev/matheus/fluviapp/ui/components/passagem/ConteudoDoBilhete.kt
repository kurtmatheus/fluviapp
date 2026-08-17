package dev.matheus.fluviapp.ui.components.passagem

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.matheus.fluviapp.services.printerservice.qrcode.QRCodeGenerator
import dev.matheus.fluviapp.ui.components.texts.FluviWordmark
import dev.matheus.fluviapp.ui.screens.passagem.BilheteScreen
import dev.matheus.fluviapp.ui.states.passagem.BilheteDigital
import dev.matheus.fluviapp.ui.states.passagem.PassageiroDoBilhete
import dev.matheus.fluviapp.ui.theme.AbyssNavy
import dev.matheus.fluviapp.ui.theme.HeaderNavy
import dev.matheus.fluviapp.ui.theme.LightColors
import dev.matheus.fluviapp.ui.theme.SteelTeal
import dev.matheus.fluviapp.ui.theme.SurfaceLight
import dev.matheus.fluviapp.ui.theme.marcaDaAgencia
import dev.matheus.fluviapp.ui.viewmodel.passagem.BilheteUiState

/**
 * **O bilhete, desenhado** — e é este mesmo desenho que vira imagem.
 *
 * O caminho antigo renderizava **duas vezes**: uma num `Dialog` para o operador ver, outra idêntica e
 * escondida para capturar. Aqui a pré-visualização **é** o que se grava, o que elimina de vez a chance de as
 * duas divergirem — e é o que a decisão de *"pré-visualizar no mesmo ato de emitir"* torna possível.
 *
 * ### Documento, e não cupom (decisão do analista, 2026-08-17)
 *
 * O desenho anterior era o do **papel de 80 mm**: uma coluna centralizada, tudo do mesmo tamanho, campos
 * separados por linha tracejada e nenhum rótulo — a forma que a bobina impõe. A via digital não tem essa
 * restrição, e herdá-la custava as três coisas que fazem um documento parecer documento:
 *
 * - **hierarquia**: a embarcação abre o bilhete, em corpo grande, porque é a primeira pergunta de quem está na
 *   doca — *é este barco?*. O trajeto vem em seguida, e a partida e o tipo de bilhete abaixo dele;
 * - **rótulo em cada valor**: sem ele, "F/B Modelo" e "Rede" são duas linhas que o leitor tem de adivinhar. Os
 *   rótulos usam **o nome do campo** do [BilheteDigital] — foi por isso que `travessia` virou [BilheteDigital.trajeto];
 * - **caixa em vez de tracejado**: agrupar por área desenhada, alinhada à esquerda, é o que separa "quem
 *   viaja" de "o que se pagou" sem gastar uma linha por separação.
 *
 * A **impressão física continua com o desenho dela** (`services/printerservice`): são dois suportes, e cada um
 * tem a sua forma. O que os dois compartilham é o conteúdo, não o layout.
 *
 * ### O que este composable força, e todas por serem sobre imagem
 *
 * - **[LightColors] e fundo branco explícitos**: o bilhete é lido claro, e é guardado como imagem — ele não
 *   segue o tema do app, senão o arquivo sairia preto para quem usa modo escuro;
 * - **escala de tipos própria** ([RotuloDoCampo], [ValorDoCampo]): os componentes de texto do app pintam por
 *   papel do tema (`onBackground`) e servem a telas; o documento precisa de rótulo miúdo e valor forte, e
 *   fixados — o que se grava não pode depender de quem está lendo;
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CabecalhoAssinado(bilhete = bilhete, marcaPresente = marca != null) {
                    if (marca != null) {
                        Image(
                            painter = painterResource(marca.logoTopo),
                            contentDescription = bilhete.agencia,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(width = 140.dp, height = 56.dp),
                        )
                    }
                }

                // **A embarcação abre o documento**: quem está na doca pergunta primeiro qual é o barco, e a
                // resposta é a única linha do bilhete em corpo de título.
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    RotuloDoCampo(texto = "Embarcação")
                    Text(
                        text = bilhete.embarcacao.ifBlank { "—" },
                        color = HeaderNavy,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 30.sp,
                    )
                }

                CampoEmCaixa(
                    rotulo = "Trajeto",
                    valor = bilhete.trajeto.ifBlank { "—" },
                    modifier = Modifier.fillMaxWidth(),
                    tamanhoDoValor = 16.sp,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CampoEmCaixa(
                        rotulo = "Partida",
                        valor = bilhete.partida,
                        modifier = Modifier.weight(1f),
                    )
                    CampoEmCaixa(
                        rotulo = "Bilhete",
                        valor = bilhete.bilhete,
                        modifier = Modifier.weight(1f),
                    )
                }

                // A gratuidade aparece por extenso, e em caixa própria: é ela que a fiscalização confere
                // contra a credencial, então ela não pode dividir área com outro campo.
                bilhete.gratuidade?.let {
                    CampoEmCaixa(
                        rotulo = "Gratuidade",
                        valor = it,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (bilhete.passageiros.isNotEmpty()) {
                    SecaoDoBilhete(titulo = "Quem viaja") {
                        bilhete.passageiros.forEach { PassageiroNoBilhete(it) }
                    }
                }

                bilhete.veiculo?.let { veiculo ->
                    SecaoDoBilhete(titulo = "Veículo") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            CampoEmCaixa(
                                rotulo = "Placa",
                                valor = veiculo.placa,
                                modifier = Modifier.weight(1f),
                                tamanhoDoValor = 18.sp,
                            )
                            CampoEmCaixa(
                                rotulo = "Classe",
                                valor = veiculo.classe,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        // **Uma caixa por campo que existe**, e nenhuma pelo que não se pergunta: carreta e
                        // caminhão não têm modelo (o tipo já é o modelo), e só a moto tem cilindrada. A linha
                        // se encurta em vez de abrir lacuna, e nenhum campo se esconde dentro de outro.
                        val complementos = listOf(
                            "Modelo" to veiculo.modelo,
                            "Cor" to veiculo.cor,
                            "Cilindrada" to veiculo.cilindrada,
                        ).filter { (_, valor) -> !valor.isNullOrBlank() }

                        if (complementos.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                complementos.forEach { (rotulo, valor) ->
                                    CampoEmCaixa(
                                        rotulo = rotulo,
                                        valor = valor.orEmpty(),
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }

                TotalDoBilhete(total = bilhete.total)

                bilhete.observacao?.let {
                    Text(
                        text = it,
                        color = SteelTeal,
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                    )
                }

                QrDeEmbarque(idPassagem = bilhete.idPassagem)
            }
        }
    }
}

/**
 * A faixa de identidade: **quem emitiu** de um lado, **qual bilhete** do outro, e uma régua navy fechando.
 *
 * O número entra numa pílula escura porque ele é a referência que se dita no telefone e se procura na lista —
 * em corpo de texto solto, ele se perdia ao lado da logo.
 *
 * A régua tem 2 dp e cor cheia, enquanto os divisores antigos tinham 1 dp: é o que marca o fim do cabeçalho
 * sem precisar de mais espaço. Sem marca própria, o wordmark assina no lugar da logo.
 */
@Composable
private fun CabecalhoAssinado(
    bilhete: BilheteDigital,
    marcaPresente: Boolean,
    logo: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (marcaPresente) {
                logo()
            } else {
                FluviWordmark(
                    modifier = Modifier,
                    fontSize = 28.sp,
                    fluviColor = SteelTeal,
                    appGradient = listOf(SteelTeal, HeaderNavy, SteelTeal),
                    strokeWidth = 3f,
                )
            }

            Text(
                modifier = Modifier
                    .background(HeaderNavy, RoundedCornerShape(percent = 50))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                text = bilhete.numero,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        HorizontalDivider(thickness = 2.dp, color = HeaderNavy)
    }
}

/**
 * O rótulo de um campo: miúdo, espaçado e em caixa alta.
 *
 * Caixa alta com `letterSpacing` é o que deixa o rótulo **legível sem competir** com o valor. Sem o
 * espaçamento, 10 sp em maiúsculas vira um borrão; com ele, lê-se como etiqueta de formulário — que é
 * exatamente o papel dele no documento.
 */
@Composable
private fun RotuloDoCampo(texto: String) {
    Text(
        text = texto.uppercase(),
        color = SteelTeal,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
    )
}

/** O valor de um campo: navy cheio, semibold, no corpo que a caixa pedir. */
@Composable
private fun ValorDoCampo(texto: String, tamanho: TextUnit = 15.sp) {
    Text(
        text = texto,
        color = AbyssNavy,
        fontSize = tamanho,
        fontWeight = FontWeight.SemiBold,
        lineHeight = tamanho * 1.35f,
    )
}

/**
 * **Rótulo e valor dentro de uma área desenhada** — o átomo do novo desenho.
 *
 * O fundo é [SurfaceLight] e não uma borda: numa imagem, o preenchimento sobrevive melhor à compressão e à
 * redução de escala do que uma linha de 1 dp, e é o que dá ao documento a aparência de campo preenchido.
 *
 * A caixa é opaca **por consequência útil**: onde há campo, a marca d'água não atravessa o texto.
 */
@Composable
private fun CampoEmCaixa(
    rotulo: String,
    valor: String,
    modifier: Modifier = Modifier,
    tamanhoDoValor: TextUnit = 15.sp,
) {
    Column(
        modifier = modifier
            .background(SurfaceLight, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        RotuloDoCampo(texto = rotulo)
        ValorDoCampo(texto = valor, tamanho = tamanhoDoValor)
    }
}

/** Um título de seção e o que ele agrupa — "Quem viaja", "Veículo". */
@Composable
private fun SecaoDoBilhete(titulo: String, conteudo: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RotuloDoCampo(texto = titulo)
        conteudo()
    }
}

/**
 * Nome e documento **por inteiro** — o bilhete não mascara (decisão do analista): ele vai para a mão de quem
 * já sabe o próprio número, e é conferido contra a identidade na doca.
 *
 * O papel ("Titular", "Acompanhante", "Responsável pela retirada") é o rótulo da caixa, e não uma linha de
 * texto: ele diz **o que a pessoa é neste bilhete**, que é a definição de rótulo.
 */
@Composable
private fun PassageiroNoBilhete(passageiro: PassageiroDoBilhete) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceLight, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        RotuloDoCampo(texto = passageiro.papel)
        ValorDoCampo(texto = passageiro.nome.ifBlank { "Nome não informado" }, tamanho = 17.sp)
        Text(
            text = passageiro.documento,
            color = AbyssNavy,
            fontSize = 13.sp,
        )
    }
}

/**
 * **O total, invertido** — fundo navy e valor branco.
 *
 * É o único bloco que troca de cor, e por hierarquia: numa via digital de altura livre, o dinheiro é o que se
 * procura ao reabrir o arquivo meses depois. Zerado ele continua ali, com o mesmo peso — um bilhete gratuito
 * também precisa provar quanto custou.
 */
@Composable
private fun TotalDoBilhete(total: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderNavy, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "TOTAL",
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.4.sp,
        )
        Text(
            text = total,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * O QR carrega **o id da passagem**, e nada mais (ADR-0012): ele é ponteiro, e quem valida lê o documento ao
 * vivo no servidor. É por isso que um bilhete fotografado nunca fica "velho" — o que ele leva é o endereço,
 * não o estado. Sem id não há o que ler, então não há QR.
 *
 * A legenda existe para o passageiro, não para quem valida: sem ela, um quadrado preto no pé de um documento é
 * uma pergunta sem resposta.
 */
@Composable
private fun QrDeEmbarque(idPassagem: String) {
    if (idPassagem.isBlank()) return

    val qr = remember(idPassagem) { QRCodeGenerator().generate(idPassagem, size = 300) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Image(
            bitmap = qr.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(150.dp).background(Color.White),
        )
        Text(
            text = "Apresente este código no embarque",
            color = SteelTeal,
            fontSize = 11.sp,
        )
    }
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

/**
 * A tela **depois de gravada**: bilhete desenhado, aviso da galeria e os dois gestos. É este o estado que
 * vale a pena ver desenhado — o de carregamento é um indicador centralizado, e o de não encontrado é uma
 * linha de texto.
 */
@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun BilheteScreenPreview() {
    BilheteScreen(
        state = BilheteUiState(
            carregando = false,
            naoEncontrado = false,
            bilhete = bilheteDeRedeInteira,
            arquivo = Uri.EMPTY,
            chaveDaOcorrencia = "viagem-modelo-1@2026-08-18",
        ),
    )
}