package dev.matheus.fluviapp.ui.screens.passagem

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
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

/** Fase 1 — câmera ativa lendo o QR (pede a permissão de câmera antes). */
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

    when {
        permissaoConcedida -> {
            TextRegularBrown(text = stringResource(R.string.msg_aponte_qr))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .aspectRatio(1f)
            ) {
                CameraPreviewQr(onQrLido = onQrLido, modifier = Modifier.fillMaxSize())
            }
        }

        permissaoNegada -> TextRegularBrownItalic(
            modifier = Modifier.padding(24.dp),
            text = stringResource(R.string.msg_permissao_camera_negada)
        )
    }
}

/**
 * Fase 2 — dados resolvidos ao vivo; operador confere o bilhete antes de confirmar.
 *
 * A tela recebe a **projeção pronta** ([ConferenciaDeEmbarque]) e não o agregado: nome, travessia e partida
 * são ids no bilhete (ADR-0023 D8), e resolvê-los é da junção — que roda no ViewModel, com o carregamento à
 * vista. Aqui não há um `Map` a consultar nem um id a traduzir; há texto a mostrar.
 *
 * A identificação pode vir vazia sem que nada tenha falhado: o pool é PII e a leitura dele é recortada pela
 * assinatura, então quem embarca um bilhete vendido por **outra agência** legitimamente não vê o nome.
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
        TextSubTitleBrownBold(text = conferencia.identificacao)
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

/**
 * Preview da câmera (CameraX) com análise por ML Kit (ADR-0012). Lê só QR; entrega o `rawValue`
 * (o id da passagem) via [onQrLido]. Vinculada ao ciclo de vida do Composable.
 */
@Composable
private fun CameraPreviewQr(
    onQrLido: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val scanner = BarcodeScanning.getClient(
                    BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build()
                )

                val analise = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                        analisarFrame(scanner, imageProxy, onQrLido)
                    } }

                runCatching {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analise
                    )
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}

@OptIn(ExperimentalGetImage::class)
private fun analisarFrame(
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: ImageProxy,
    onQrLido: (String) -> Unit,
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }
    val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    scanner.process(input)
        .addOnSuccessListener { barcodes ->
            barcodes.firstOrNull()?.rawValue?.let { onQrLido(it) }
        }
        .addOnCompleteListener { imageProxy.close() }
}