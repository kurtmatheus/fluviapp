package dev.matheus.fluviapp.ui.screens.passagem

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.passagem.ResultadoEmbarque
import dev.matheus.fluviapp.ui.states.passagem.ConferenciaDeEmbarque
import dev.matheus.fluviapp.ui.components.RequestPermission
import dev.matheus.fluviapp.ui.components.contents.CommonTopRow
import dev.matheus.fluviapp.ui.components.forms.buttons.CommonIconButton
import dev.matheus.fluviapp.ui.components.texts.TextRegularBrown
import dev.matheus.fluviapp.ui.components.texts.TextRegularBrownItalic
import dev.matheus.fluviapp.ui.components.texts.TextSubTitleBrownBold
import dev.matheus.fluviapp.ui.components.texts.TextTitleBrownRegular
import dev.matheus.fluviapp.ui.screens.forms.CommonScreenNoBottom
import dev.matheus.fluviapp.ui.states.passagem.EmbarqueUiState

@Composable
fun EmbarqueScreen(
    state: EmbarqueUiState,
    onClickVoltar: () -> Unit = {},
    onQrLido: (String) -> Unit = {},
    onConfirmar: () -> Unit = {},
    onReiniciar: () -> Unit = {},
) {
    CommonScreenNoBottom(
        titleTopAppBar = R.string.title_top_embarque,
        titleTopContent = R.string.subtitle_embarque,
        isShowRightIcon = false,
        hasRefresh = false,
        isRefreshing = false,
        onClickVoltar = onClickVoltar
    ) { modifier, title ->
        Column(
            modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CommonTopRow(modifier = modifier, titulo = title)

            when {
                state.processando -> CircularProgressIndicator(
                    modifier = Modifier.padding(40.dp)
                )

                state.resultado != null -> ResultadoView(
                    resultado = state.resultado,
                    onReiniciar = onReiniciar
                )

                state.conferencia != null -> ConferenciaView(
                    conferencia = state.conferencia,
                    onConfirmar = onConfirmar,
                    onCancelar = onReiniciar
                )

                else -> LeitorView(onQrLido = onQrLido)
            }
        }
    }
}

/**
 * Fase 1 — a leitura do QR, delegada ao **scanner do zxing-android-embedded** (`ScanContract`).
 *
 * A tela não hospeda mais a câmera: ela *pede um resultado*. O leitor é uma Activity da própria biblioteca,
 * lançada por contrato de resultado — o que troca o preview embutido (câmera vinculada ao ciclo de vida do
 * Composable, análise quadro a quadro, `ImageProxy` a fechar na mão) por uma fronteira de uma linha: entra
 * [ScanOptions], volta o conteúdo lido.
 *
 * A permissão continua sendo pedida **aqui**, antes de lançar: a biblioteca também a pede, mas quem nega
 * dentro dela só recebe um resultado vazio, e a tela ficaria muda. Pedindo antes, a negativa tem texto.
 *
 * O leitor abre sozinho ao entrar nesta fase — é o que a doca espera de um totem de embarque — e o botão
 * fica para quem cancelou a leitura e quer tentar de novo.
 */
@Composable
private fun LeitorView(onQrLido: (String) -> Unit) {
    val context = LocalContext.current
    var permissaoConcedida by remember { mutableStateOf(false) }
    var permissaoNegada by remember { mutableStateOf(false) }

    if (!permissaoConcedida) {
        RequestPermission(
            context = context,
            permission = Manifest.permission.CAMERA,
            onGrantedPermission = { permissaoConcedida = true; permissaoNegada = false },
            onDeniedPermission = { permissaoNegada = true }
        )
    }

    // `contents` é nulo quando a leitura foi cancelada (voltar, ou permissão negada dentro do scanner):
    // cancelar não é ler, e nada sobe para o ViewModel.
    val leitor = rememberLauncherForActivityResult(ScanContract()) { resultado ->
        resultado.contents?.let(onQrLido)
    }

    when {
        permissaoConcedida -> {
            LaunchedEffect(Unit) { leitor.launch(opcoesDeLeitura(context)) }

            TextRegularBrown(text = stringResource(R.string.msg_aponte_qr))
            CommonIconButton(
                modifier = Modifier,
                text = stringResource(R.string.btn_escanear),
                onClick = { leitor.launch(opcoesDeLeitura(context)) },
                isProcessing = false
            )
        }

        permissaoNegada -> TextRegularBrownItalic(
            modifier = Modifier.padding(24.dp),
            text = stringResource(R.string.msg_permissao_camera_negada)
        )
    }
}

/**
 * O que este leitor aceita: **só QR**. O bilhete é QR (ADR-0012), e um leitor que também decodificasse
 * código de barras aceitaria ler o que a doca não emite — a restrição é do domínio, não de desempenho.
 *
 * `barcodeImageEnabled = false` porque o embarque quer o id, não a foto do bilhete; o bipe é a confirmação
 * audível de que leu, para quem está olhando a fila e não a tela.
 */
private fun opcoesDeLeitura(context: Context) = ScanOptions()
    .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
    .setPrompt(context.getString(R.string.msg_aponte_qr))
    .setBeepEnabled(true)
    .setBarcodeImageEnabled(false)

/**
 * Fase 2 — dados resolvidos ao vivo; operador confere o bilhete antes de confirmar.
 *
 * A tela recebe a **projeção pronta** ([ConferenciaDeEmbarque]) e não o agregado: travessia e partida são ids
 * no bilhete (ADR-0023 D8), e resolvê-los é da junção — que roda no ViewModel, com o carregamento à vista.
 * Aqui não há um `Map` a consultar nem um id a traduzir; há texto a mostrar.
 *
 * **Não há nome de passageiro nesta tela, e é decisão**: *o embarque confere bilhete e não pessoa*. O que se
 * exibe é o que decide na doca — que bilhete é, para onde vai, quando sai e se ainda vale.
 */
@Composable
private fun ConferenciaView(
    conferencia: ConferenciaDeEmbarque,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextTitleBrownRegular(text = conferencia.numero)
        TextSubTitleBrownBold(text = conferencia.bilhete)
        if (conferencia.travessia.isNotBlank()) TextRegularBrown(text = conferencia.travessia)
        TextRegularBrown(text = conferencia.partida)
        TextRegularBrownItalic(text = conferencia.status)
    }
    CommonIconButton(
        modifier = Modifier,
        text = stringResource(R.string.btn_confirmar_embarque),
        onClick = onConfirmar,
        isProcessing = false
    )
    CommonIconButton(
        modifier = Modifier,
        text = stringResource(R.string.btn_escanear_outro),
        onClick = onCancelar,
        isProcessing = false
    )
}

/** Fase 3 — desfecho da confirmação. */
@Composable
private fun ResultadoView(
    resultado: ResultadoEmbarque,
    onReiniciar: () -> Unit,
) {
    val mensagem = when (resultado) {
        is ResultadoEmbarque.Confirmada -> stringResource(R.string.msg_embarque_confirmado)
        is ResultadoEmbarque.JaEmbarcada -> stringResource(R.string.msg_embarque_ja_utilizado)
        ResultadoEmbarque.NaoEmitida -> stringResource(R.string.msg_embarque_nao_emitida)
        ResultadoEmbarque.NaoEncontrada -> stringResource(R.string.msg_embarque_nao_encontrada)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextTitleBrownRegular(text = mensagem)
        when (resultado) {
            is ResultadoEmbarque.Confirmada -> {
                TextSubTitleBrownBold(text = "#${resultado.passagem.numero}")
                TextRegularBrown(text = resultado.passagem.metadados.embarque?.em.orEmpty())
            }
            // Só o instante: quem carimbou é um **uid**, e uid não se mostra a ninguém. O nome de quem
            // validou se resolve por referência, na junção da F9.4 (ADR-0023 D8).
            is ResultadoEmbarque.JaEmbarcada -> TextRegularBrown(text = resultado.carimbo.em)
            else -> {}
        }
    }
    CommonIconButton(
        modifier = Modifier,
        text = stringResource(R.string.btn_escanear_outro),
        onClick = onReiniciar,
        isProcessing = false
    )
}
