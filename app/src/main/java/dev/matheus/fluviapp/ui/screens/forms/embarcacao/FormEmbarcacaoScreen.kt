package dev.matheus.fluviapp.ui.screens.forms.embarcacao

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.components.forms.areas.CommonAreaForm
import dev.matheus.fluviapp.ui.components.forms.areas.embarcacao.ContentEmbarcacaoAreaForm
import dev.matheus.fluviapp.ui.components.forms.buttons.CommonIconButton
import dev.matheus.fluviapp.ui.screens.forms.CommonScreenNoBottom
import dev.matheus.fluviapp.ui.states.FormEmbarcacaoUiState

@Composable
fun FormEmbarcacaoScreen(
    uiState: FormEmbarcacaoUiState,
    onNomeChange: (String) -> Unit = {},
    onTipoChange: (String) -> Unit = {},
    onEmpresaChange: (String) -> Unit = {},
    onCapacidadeVeiculoChange: (String) -> Unit = {},
    onCapacidadeSuite2Change: (String) -> Unit = {},
    onCapacidadeSuite3Change: (String) -> Unit = {},
    onCapacidadeCamaroteChange: (String) -> Unit = {},
    onClickSalvar: () -> Unit = {},
    onClickVoltar: () -> Unit = {},
) {
    CommonScreenNoBottom(
        titleTopAppBar = R.string.title_top_embarcacao,
        titleTopContent = 0,
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
        ) {
            CommonAreaForm(
                modifier = modifier,
                titleArea = uiState.titulo,
            ) {
                ContentEmbarcacaoAreaForm(
                    modifier = it,
                    state = uiState,
                    onNomeChange = onNomeChange,
                    onTipoChange = onTipoChange,
                    onEmpresaChange = onEmpresaChange,
                    onCapacidadeVeiculoChange = onCapacidadeVeiculoChange,
                    onCapacidadeSuite2Change = onCapacidadeSuite2Change,
                    onCapacidadeSuite3Change = onCapacidadeSuite3Change,
                    onCapacidadeCamaroteChange = onCapacidadeCamaroteChange,
                )
            }
            Column(
                modifier = modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CommonIconButton(
                    modifier = modifier,
                    onClick = onClickSalvar,
                    text = stringResource(id = R.string.btn_salvar),
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = stringResource(id = R.string.description_confirmacao),
                        )
                    },
                    color = MaterialTheme.colorScheme.primary,
                    isProcessing = uiState.isProcessing,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FormEmbarcacaoScreenPreview() {
    FormEmbarcacaoScreen(uiState = FormEmbarcacaoUiState())
}
