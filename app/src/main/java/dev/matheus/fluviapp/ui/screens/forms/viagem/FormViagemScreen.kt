package dev.matheus.fluviapp.ui.screens.forms.viagem

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.components.forms.areas.CommonAreaForm
import dev.matheus.fluviapp.ui.components.forms.areas.viagem.ContentViagemAreaForm
import dev.matheus.fluviapp.ui.components.forms.buttons.CommonIconButton
import dev.matheus.fluviapp.ui.screens.forms.CommonScreenNoBottom
import dev.matheus.fluviapp.ui.states.FormViagemUiState

@Composable
fun FormViagemScreen(
    uiState: FormViagemUiState,
    onClickSalvar: (Context) -> Unit,
    onClickVoltar: () -> Unit
) {
    val context = LocalContext.current

    CommonScreenNoBottom(
        titleTopAppBar = R.string.title_top_viagem,
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
                titleArea = uiState.titleJanela
            ) {
                ContentViagemAreaForm(
                    modifier = it,
                    state = uiState
                )
            }
            Column(
                modifier = modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CommonIconButton(
                    modifier = modifier,
                    onClick = { onClickSalvar(context) },
                    text = stringResource(id = R.string.btn_salvar),
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = stringResource(id = R.string.description_confirmacao)
                        )
                    },
                    color = MaterialTheme.colorScheme.primary,
                    isProcessing = uiState.isProcessando
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun NovaPassagemFormScreenPreview() {
        FormViagemScreen(
            uiState = FormViagemUiState(),
            onClickSalvar = {},
            onClickVoltar = {}
        )
}