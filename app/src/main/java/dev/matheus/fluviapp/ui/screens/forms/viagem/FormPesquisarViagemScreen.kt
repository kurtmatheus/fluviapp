package dev.matheus.fluviapp.ui.screens.forms.viagem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import dev.matheus.fluviapp.ui.components.forms.dropdowns.DropDownFormField
import dev.matheus.fluviapp.ui.components.forms.search.CommonSearchAreaForm
import dev.matheus.fluviapp.ui.components.texts.SupportingTextRed
import dev.matheus.fluviapp.ui.screens.forms.CommonScreenNoBottom
import dev.matheus.fluviapp.ui.states.PesquisarViagemUiState

@Composable
fun FormPesquisarViagemScreen(
    state: PesquisarViagemUiState,
    focusManager: FocusManager = LocalFocusManager.current,
    onCheckEmpresa: () -> Unit = {},
    onEmpresaChange: (String) -> Unit = {},
    onCheckNavio: () -> Unit = {},
    onNavioChange: (String) -> Unit = {},
    onCheckTrecho: () -> Unit = {},
    onOrigemChange: (String) -> Unit = {},
    onDestinoChange: (String) -> Unit = {},
    onClickVoltar: () -> Unit = {},
    onClickPesquisar: () -> Unit = {},
) {
    CommonScreenNoBottom(
        titleTopAppBar = R.string.title_top_viagem,
        titleTopContent = R.string.subtitle_pesquisar_viagens,
        isShowRightIcon = false,
        hasRefresh = false,
        isRefreshing = false,
        onClickVoltar = onClickVoltar,
    ) { modifier, _ ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CommonTopRow(modifier = modifier, titulo = R.string.subtitle_pesquisar_viagens)

            CommonSearchAreaForm(
                modifier = modifier,
                labelFiltro = R.string.label_filtro_empresa,
                checked = state.isCheckedEmpresa,
                onCheck = { onCheckEmpresa() }
            ) {
                DropDownFormField(
                    listaItens = state.listaEmpresas.map { it.nome },
                    label = R.string.label_empresa,
                    modifier = modifier.fillMaxWidth(),
                    value = state.empresa,
                    onValueChange = onEmpresaChange,
                    isError = state.isEmpresaError,
                    focusManager = focusManager
                )
            }

            CommonSearchAreaForm(
                modifier = modifier,
                labelFiltro = R.string.label_filtro_navio,
                checked = state.isCheckedNavio,
                onCheck = { onCheckNavio() }
            ) {
                DropDownFormField(
                    listaItens = state.listaNavios.map { it.descricaoNome },
                    label = R.string.label_navio,
                    modifier = modifier.fillMaxWidth(),
                    value = state.navio,
                    onValueChange = onNavioChange,
                    isError = state.isNavioError,
                    focusManager = focusManager
                )
            }

            CommonSearchAreaForm(
                modifier = modifier,
                labelFiltro = R.string.label_filtro_trecho,
                checked = state.isCheckedTrecho,
                onCheck = { onCheckTrecho() }
            ) {
                DropDownFormField(
                    listaItens = state.listaMunicipios.map { it.descricaoNome },
                    label = R.string.label_trecho_origem,
                    modifier = modifier.fillMaxWidth(),
                    value = state.origem,
                    onValueChange = onOrigemChange,
                    isError = state.isTrechoError,
                    focusManager = focusManager
                )

                DropDownFormField(
                    listaItens = state.listaMunicipios.map { it.descricaoNome },
                    label = R.string.label_trecho_destino,
                    modifier = modifier.fillMaxWidth(),
                    value = state.destino,
                    onValueChange = onDestinoChange,
                    isError = state.isTrechoError,
                    focusManager = focusManager
                )

                if (state.isTrechoError) {
                    SupportingTextRed(
                        modifier = modifier.padding(top = 10.dp),
                        text = stringResource(id = state.textTrechoError)
                    )
                }
            }

            CommonIconButton(
                modifier = modifier,
                onClick = onClickPesquisar,
                text = stringResource(id = R.string.btn_common_pesquisar),
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = stringResource(id = R.string.description_lupa)
                    )
                },
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Preview
@Composable
private fun FormPesquisarViagemScreenPreview() {
    FormPesquisarViagemScreen(
        state = PesquisarViagemUiState(isCheckedTrecho = true, isTrechoError = true),
    )
}
