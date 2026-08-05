package dev.matheus.fluviapp.ui.screens.forms.localidade

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
import dev.matheus.fluviapp.ui.components.forms.areas.localidade.ContentLocalidadeAreaForm
import dev.matheus.fluviapp.ui.components.forms.buttons.CommonIconButton
import dev.matheus.fluviapp.ui.screens.forms.CommonScreenNoBottom
import dev.matheus.fluviapp.ui.states.FormLocalidadeUiState

@Composable
fun FormLocalidadeScreen(
    uiState: FormLocalidadeUiState,
    onCodigoIbgeChange: (String) -> Unit = {},
    onMunicipioChange: (String) -> Unit = {},
    onUfChange: (String) -> Unit = {},
    onConsultarIbge: () -> Unit = {},
    onClickSalvar: () -> Unit = {},
    onClickVoltar: () -> Unit = {},
) {
    CommonScreenNoBottom(
        titleTopAppBar = R.string.title_top_localidade,
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
                ContentLocalidadeAreaForm(
                    modifier = it,
                    state = uiState,
                    onCodigoIbgeChange = onCodigoIbgeChange,
                    onMunicipioChange = onMunicipioChange,
                    onUfChange = onUfChange,
                    onConsultarIbge = onConsultarIbge,
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
private fun FormLocalidadeScreenPreview() {
    FormLocalidadeScreen(uiState = FormLocalidadeUiState())
}