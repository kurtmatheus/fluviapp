package dev.matheus.fluviapp.ui.screens.forms.passagem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.screendata.DadosImpressora
import dev.matheus.fluviapp.sampledata.dadosPassagemSample
import dev.matheus.fluviapp.sampledata.listaDadosImpressoraSample
import dev.matheus.fluviapp.ui.components.cards.CommonDetalhamentoCard
import dev.matheus.fluviapp.ui.components.contents.CommonTopRow
import dev.matheus.fluviapp.ui.components.contents.DetalhamentoPassagemContent
import dev.matheus.fluviapp.ui.components.dialogs.CommonInformativeDialog
import dev.matheus.fluviapp.ui.components.dialogs.EmissaoPassagemDigitalDialog
import dev.matheus.fluviapp.ui.components.dialogs.ImpressorasDialog
import dev.matheus.fluviapp.ui.components.forms.buttons.CommonIconButton
import dev.matheus.fluviapp.ui.components.sheets.BarraInferiorEmissao
import dev.matheus.fluviapp.ui.components.texts.TextTitleBrownItalic
import dev.matheus.fluviapp.ui.screens.forms.CommonScreenNoBottom
import dev.matheus.fluviapp.ui.states.ImpressaoState
import dev.matheus.fluviapp.ui.states.passagem.DetalhesPassagemState

@Composable
fun DetalhesPassagemScreen(
    state: DetalhesPassagemState,
    stateImpressao: ImpressaoState,
    onClickVoltar: () -> Unit = {},
    onClickContinuarCadastrando: (String) -> Unit = {},
    onClickConfirmReturnDialog: () -> Unit = {},
    onClickDismissReturnDialog: () -> Unit = {},
    onClickEditarPassagem: (String, String) -> Unit = { _, _ -> },
    onClickDeletarPassagem: (String) -> Unit = {},
    onClickDismissDeleteDialog: () -> Unit = {},
    onClickConfirmDeleteDialog: (String) -> Unit = {},
    onClickSelecionarImpressora: (DadosImpressora) -> Unit = {},
    onDismissDialogImpressoras: () -> Unit = {},
    onClickEmitir: () -> Unit = {},
    onDismissSheetEmissao: () -> Unit = {},
    onClickImpressaoFisica: () -> Unit = {},
    onClickEmitirPassagemDigital: () -> Unit = {},
    onClickDismissDialogViaEmbarcacao: () -> Unit = {},
    onClickImprimirViaEmbarcacao: () -> Unit = {},
    onDismissDialogEmissaoPassagemDigital: () -> Unit = {},
    onProcessaImageBitmap: (ImageBitmap) -> Unit = {},
    onClickMenuImpressoras: () -> Unit = {},
    onParearNovaImpressora: () -> Unit = {}
) {

    val scrollState = rememberScrollState()

    CommonScreenNoBottom(
        titleTopAppBar = R.string.title_top_passagem,
        titleTopContent = R.string.subtitle_detalhes_passagem,
        isShowRightIcon = true,
        rightIcon = Icons.Filled.Print,
        onClickRightIcon = onClickMenuImpressoras,
        hasRefresh = false,
        isRefreshing = false,
        onClickVoltar = onClickVoltar,
    ) { modifier, title ->

        DetalhesPassagemDialogHandler(
            modifier = modifier,
            state = state,
            stateImpressao = stateImpressao,
            onClickDismissReturnDialog = onClickDismissReturnDialog,
            onClickConfirmReturnDialog = onClickConfirmReturnDialog,
            onClickDismissDeleteDialog = onClickDismissDeleteDialog,
            onClickConfirmDeleteDialog = onClickConfirmDeleteDialog,
            onDismissDialogImpressoras = onDismissDialogImpressoras,
            onClickSelecionarImpressora = onClickSelecionarImpressora,
            onClickDismissDialogViaEmbarcacao = onClickDismissDialogViaEmbarcacao,
            onClickImprimirViaEmbarcacao = onClickImprimirViaEmbarcacao,
            onDismissDialogEmissaoPassagemDigital = onDismissDialogEmissaoPassagemDigital,
            onProcessaImageBitmap = onProcessaImageBitmap,
            onParearNovaImpressora = onParearNovaImpressora
        )

        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(
                    scrollState
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CommonTopRow(modifier = modifier, titulo = title)

            CommonDetalhamentoCard(
                modifier = modifier
            ) {
                DetalhamentoPassagemContent(modifier = it, state = state)
            }

            if (state.isAdminOuFuncResposavel) {
                CommonIconButton(
                    modifier = modifier,
                    onClick = onClickEmitir,
                    text = stringResource(id = R.string.btn_emitir),
                    isProcessing = stateImpressao.isPrinting
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_imprimir_24),
                        contentDescription = stringResource(id = R.string.description_impressora)
                    )
                }
                CommonIconButton(
                    modifier = modifier,
                    onClick = { onClickContinuarCadastrando(state.dadosPassagem.idViagem) },
                    text = stringResource(id = R.string.btn_cadastrar_mais_passagens),
                    color = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(id = R.string.description_adicionar)
                    )
                }

                TextButton(onClick = {
                    onClickEditarPassagem(
                        state.dadosPassagem.idViagem,
                        state.dadosPassagem.idPassagem
                    )
                }
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(id = R.string.description_editar),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = modifier.padding(5.dp))
                    TextTitleBrownItalic(text = stringResource(id = R.string.btn_editar))
                }

                TextButton(onClick = {
                    onClickDeletarPassagem(state.dadosPassagem.idPassagem)
                }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(id = R.string.description_deletar),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = modifier.padding(5.dp))
                    TextTitleBrownItalic(text = stringResource(id = R.string.btn_deletar))
                }
            }
        }
        if (state.isShowSheetEmissao) {
            BarraInferiorEmissao(
                modifier,
                onDismissSheetEmissao,
                onClickImpressaoFisica,
                onClickEmitirPassagemDigital
            )
        }

    }
}

@Composable
private fun DetalhesPassagemDialogHandler(
    modifier: Modifier,
    state: DetalhesPassagemState,
    stateImpressao: ImpressaoState,
    onClickDismissReturnDialog: () -> Unit,
    onClickConfirmReturnDialog: () -> Unit,
    onClickDismissDeleteDialog: () -> Unit,
    onClickConfirmDeleteDialog: (String) -> Unit,
    onDismissDialogImpressoras: () -> Unit,
    onClickSelecionarImpressora: (DadosImpressora) -> Unit,
    onClickDismissDialogViaEmbarcacao: () -> Unit,
    onClickImprimirViaEmbarcacao: () -> Unit,
    onDismissDialogEmissaoPassagemDigital: () -> Unit,
    onProcessaImageBitmap: (ImageBitmap) -> Unit,
    onParearNovaImpressora: () -> Unit
) {
    if (state.isShowConfirmReturnDialog) {
        CommonInformativeDialog(
            modifier = Modifier,
            textMensagem = R.string.msg_retornar_emissao,
            textConfirm = R.string.btn_retornar,
            textDismiss = R.string.btn_cancelar,
            onDismiss = onClickDismissReturnDialog,
            onConfirm = onClickConfirmReturnDialog
        )
    }

    if (state.isShowConfirmDeleteDialog) {
        CommonInformativeDialog(
            modifier = Modifier,
            textMensagem = R.string.msg_confirmar_exclusao,
            textConfirm = R.string.btn_excluir,
            textDismiss = R.string.btn_cancelar,
            onDismiss = onClickDismissDeleteDialog,
            onConfirm = { onClickConfirmDeleteDialog(state.dadosPassagem.idPassagem) }
        )
    }

    if (!ImpressaoState.isPrinterSelected &&
        stateImpressao.exibirDialogImpressoras
    ) {
        ImpressorasDialog(
            modifier = modifier,
            state = stateImpressao,
            onDismiss = onDismissDialogImpressoras,
            onSelecionaImpressora = { onClickSelecionarImpressora(it) },
            onParearNovaImpressora = onParearNovaImpressora
        )
    }

    if (stateImpressao.isShowDialogImpressaoViaEmbarcacao) {
        CommonInformativeDialog(
            modifier = Modifier,
            textMensagem = R.string.msg_emissao_via_embarcacao,
            textConfirm = R.string.btn_sim,
            textDismiss = R.string.btn_cancelar,
            onDismiss = onClickDismissDialogViaEmbarcacao,
            onConfirm = onClickImprimirViaEmbarcacao
        )
    }

    if (state.isShowDialogEmissaoPassagemDigital) {
        EmissaoPassagemDigitalDialog(
            modifier = modifier,
            onDismiss = onDismissDialogEmissaoPassagemDigital,
            dadosPassagem = state.dadosPassagem,
            onProcessaImageBitmap = onProcessaImageBitmap
        )
    }

}

@Preview
@Composable
private fun ResultadosViagemSearchScreenPreview() {
    DetalhesPassagemScreen(
        state = DetalhesPassagemState(
            dadosPassagem = dadosPassagemSample,
            isAdminOuFuncResposavel = true,
        ),
        stateImpressao = ImpressaoState()
    )
}

@Preview
@Composable
private fun ResultadosPassagemScreenDialogImpressorasPreview() {
    DetalhesPassagemScreen(
        state = DetalhesPassagemState(
            dadosPassagem = dadosPassagemSample,
            isAdminOuFuncResposavel = true
        ),
        stateImpressao = ImpressaoState(
            exibirDialogImpressoras = true,
            listaImpressorasPareadas = listaDadosImpressoraSample
        )
    )
}

@Preview
@Composable
private fun ResultadosPassagemScreenImprimindoPreview() {
    DetalhesPassagemScreen(
        state = DetalhesPassagemState(
            dadosPassagem = dadosPassagemSample,
            isAdminOuFuncResposavel = true
        ),
        stateImpressao = ImpressaoState(
            isPrinting = true
        )
    )
}

@Preview
@Composable
private fun ResultadosPassagemScreenPreview() {
    DetalhesPassagemScreen(
        state = DetalhesPassagemState(
            dadosPassagem = dadosPassagemSample,
            isAdminOuFuncResposavel = true
        ),
        stateImpressao = ImpressaoState(
            isShowDialogImpressaoViaEmbarcacao = true
        )
    )
}
