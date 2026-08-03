package dev.matheus.fluviapp.ui.screens.forms.passagem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.components.contents.CommonTopRow
import dev.matheus.fluviapp.ui.components.forms.buttons.CommonIconButton
import dev.matheus.fluviapp.ui.components.forms.buttons.FilterButton
import dev.matheus.fluviapp.ui.components.forms.dropdowns.DropDownFormField
import dev.matheus.fluviapp.ui.components.forms.fields.FormFieldCalendario
import dev.matheus.fluviapp.ui.components.texts.SupportingTextRed
import dev.matheus.fluviapp.ui.components.texts.TextTitleBrownItalic
import dev.matheus.fluviapp.ui.screens.forms.CommonScreenNoBottom
import dev.matheus.fluviapp.ui.states.passagem.PesquisarPassagemUiState

@Composable
fun FormPesquisarPassagemScreen(
    state: PesquisarPassagemUiState,
    focusManager: FocusManager = LocalFocusManager.current,
    onClickVoltar: () -> Unit = {},
    onClickPesquisar: () -> Unit = {},
) {
    CommonScreenNoBottom(
        titleTopAppBar = R.string.title_top_passagem,
        titleTopContent = R.string.title_top_passagem,
        isShowRightIcon = false,
        hasRefresh = false,
        isRefreshing = false,
        onClickVoltar = onClickVoltar,
    ) { modifier, _ ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CommonTopRow(modifier = modifier, titulo = R.string.btn_pesquisar_passagens)
            FormFieldCalendario(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                focusManager = focusManager,
                value = state.data,
                label = R.string.label_data_viagem,
                onValueChange = state.onDataChange,
                isError = state.isDataError,
                textoErro = R.string.error_camp_obrig
            )

            DropDownFormField(
                modifier = modifier.fillMaxWidth(),
                listaItens = state.listaSituacaoPassagem,
                label = R.string.label_situacao,
                value = state.situacao,
                onValueChange = state.onSituacaoChange,
                isError = state.isSituacaoError
            )

            if (state.temPermissaoEspecial) {
                DropDownFormField(
                    modifier = modifier.fillMaxWidth(),
                    listaItens = state.listaOperadores,
                    label = R.string.label_funcionario,
                    value = state.operador,
                    onValueChange = state.onOperadorChange,
                )
            }

            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.Start
            ) {
                TextTitleBrownItalic(text = "${stringResource(id = R.string.label_filtrar_por)}:")
                Row(
                    modifier = modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilterButton(
                        modifier = modifier,
                        label = R.string.label_veiculo,
                        isChecked = state.isVeiculoChecked,
                        onCheck = state.onCheckVeiculo
                    )
                    FilterButton(
                        modifier = modifier,
                        label = R.string.label_passageiro,
                        isChecked = state.isPassageiroChecked,
                        onCheck = state.onCheckPassageiro
                    )
                }

                if (state.isFiltroError) {
                    SupportingTextRed(
                        modifier = modifier.padding(top = 10.dp),
                        text = stringResource(id = R.string.error_selecione_opcao)
                    )
                }

            }

            CommonIconButton(
                modifier = modifier,
                onClick = onClickPesquisar,
                text = stringResource(id = R.string.btn_common_pesquisar),
                color = MaterialTheme.colorScheme.primary,
                isProcessing = state.isProcessing
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(id = R.string.description_lupa)
                )
            }
        }
    }
}

@Preview
@Composable
private fun PesquisarPassagemFormScreenPreview() {
    FormPesquisarPassagemScreen(
        state = PesquisarPassagemUiState(
            isFiltroError = true,
            temPermissaoEspecial = true
        )
    )
}