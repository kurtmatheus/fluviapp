package br.com.gruponaveg.ui.screens.forms.viagem

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.gruponaveg.R
import br.com.gruponaveg.sampledata.listaDadosDadosViagemHomeSampleCards
import br.com.gruponaveg.ui.components.cards.CommonDetalhamentoCard
import br.com.gruponaveg.ui.components.contents.CommonTopRow
import br.com.gruponaveg.ui.components.contents.DetalhamentoViagemContent
import br.com.gruponaveg.ui.components.dialogs.CommonInformativeDialog
import br.com.gruponaveg.ui.components.forms.buttons.CommonIconButton
import br.com.gruponaveg.ui.components.texts.TextTitleBrownItalic
import br.com.gruponaveg.ui.screens.forms.CommonScreenNoBottom
import br.com.gruponaveg.ui.states.PesquisarViagemUiState

@Composable
fun DetalhesViagemScreen(
    state: PesquisarViagemUiState,
    onClickVoltar: () -> Unit = {},
    isShowConfirmDeleteDialog: Boolean,
    onShowConfirmDeleteDialog: (Boolean) -> Unit,
    onClickConfirmDialog: (String) -> Unit = {},
    onClickDismissDialog: () -> Unit = {},
    onClickEditarViagem: (String) -> Unit = {},
    onClickAdicionarPassagem: (String) -> Unit = {},
) {

    val scrollState = rememberScrollState()

    CommonScreenNoBottom(
        titleTopAppBar = R.string.title_top_viagem,
        titleTopContent = R.string.subtitle_detalhes_viagem,
        isShowRightIcon = false,
        hasRefresh = false,
        isRefreshing = false,
        onClickVoltar = onClickVoltar
    ) { modifierForm, title ->

        if (isShowConfirmDeleteDialog) {
            CommonInformativeDialog(
                modifier = modifierForm,
                textMensagem = R.string.msg_confirmar_exclusao,
                textConfirm = R.string.btn_excluir,
                textDismiss = R.string.btn_cancelar,
                onDismiss = onClickDismissDialog,
                onConfirm = { onClickConfirmDialog(state.dadosViagemCard.idViagem) }
            )
        }

        Column(
            modifier = modifierForm
                .fillMaxSize()
                .verticalScroll(
                    scrollState
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CommonTopRow(modifier = modifierForm, titulo = title)

            CommonDetalhamentoCard(
                modifier = modifierForm
            ) {
                DetalhamentoViagemContent(modifier = it, dados = state.dadosViagemCard)
            }

            CommonIconButton(
                modifier = modifierForm,
                onClick = { onClickAdicionarPassagem(state.dadosViagemCard.idViagem) },
                text = stringResource(id = R.string.btn_adiocionar_passagem),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(id = R.string.description_adicionar)
                    )
                },
                color = MaterialTheme.colorScheme.primary
            )

            TextButton(onClick = { onClickEditarViagem(state.dadosViagemCard.idViagem) }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(id = R.string.description_editar),
                    tint = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = modifierForm.padding(5.dp))
                TextTitleBrownItalic(text = stringResource(id = R.string.btn_editar))
            }

            TextButton(onClick = { onShowConfirmDeleteDialog(true) }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(id = R.string.description_deletar),
                    tint = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = modifierForm.padding(5.dp))
                TextTitleBrownItalic(text = stringResource(id = R.string.btn_deletar))
            }
        }
    }
}


@Preview
@Composable
private fun ResultadosViagemSearchScreenPreview() {
    DetalhesViagemScreen(
        state = PesquisarViagemUiState(
            dadosViagemCard = listaDadosDadosViagemHomeSampleCards[0]
        ),
        isShowConfirmDeleteDialog = false,
        onShowConfirmDeleteDialog = {},
    )
}
