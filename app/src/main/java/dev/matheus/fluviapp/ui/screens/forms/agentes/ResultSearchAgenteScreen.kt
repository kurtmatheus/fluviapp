package dev.matheus.fluviapp.ui.screens.forms.agentes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.model.cadastro.passagem.Agente
import dev.matheus.fluviapp.sampledata.listaAgenteSample
import dev.matheus.fluviapp.ui.components.contents.CommonTopRow
import dev.matheus.fluviapp.ui.components.forms.divider.FormDashedDivider
import dev.matheus.fluviapp.ui.components.forms.dropdowns.FilterDropDownForm
import dev.matheus.fluviapp.ui.components.texts.TextRegularBrown
import dev.matheus.fluviapp.ui.components.texts.TextSubTitleBrownItalic
import dev.matheus.fluviapp.ui.components.texts.TextTitleBrownRegular
import dev.matheus.fluviapp.ui.screens.forms.CommonScreenNoBottom
import dev.matheus.fluviapp.ui.states.AgenteUiState
import dev.matheus.fluviapp.ui.theme.FluviAppTheme

@Composable
fun ResultSearchAgenteScreen(
    uiState: AgenteUiState,
    onClickVoltar: () -> Unit = {},
    onNavegaParaEditor: (String) -> Unit = {}
) {
    CommonScreenNoBottom(
        titleTopAppBar = R.string.title_top_agente,
        titleTopContent = R.string.subtitle_pesquisar_agentes,
        isShowRightIcon = true,
        hasRefresh = false,
        isRefreshing = false,
        onClickVoltar = onClickVoltar
    ) { modifier, titulo ->
        Column {
            CommonTopRow(modifier = modifier, titulo = titulo)

            Column(
                modifier = modifier.padding(10.dp, 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterDropDownForm(
                    modifier = modifier.fillMaxWidth(),
                    listaItens = uiState.listaAgencia,
                    label = R.string.label_agencia,
                    value = uiState.agencia,
                    isError = uiState.isAgenciaError,
                    onValueChange = uiState.onAgenciaChange,
                    keyboardType = KeyboardType.Text
                )

            }
            FormDashedDivider(modifier = modifier.fillMaxWidth())

            LazyColumn {
                items(uiState.resultadosListaAgente.filtrarResultados(agencia = uiState.agencia)) {
                    CardResultAgente(modifier, it, onNavegaParaEditor)
                }
            }
        }
    }

}

private fun List<Agente>.filtrarResultados(agencia: String): List<Agente> {
    return filter { it.agencia.startsWith(agencia, ignoreCase = true) }
}

@Composable
fun CardResultAgente(
    modifier: Modifier,
    agente: Agente,
    onNavegaParaEditor: (String) -> Unit
) {
    Column {
        Row(
            modifier = modifier
                .height(100.dp)
                .padding(10.dp, 10.dp)
                .clickable { onNavegaParaEditor(agente.id) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Icon(
                modifier = modifier,
                painter = painterResource(id = R.drawable.ic_user_75),
                contentDescription = stringResource(R.string.description_icon_user)
            )

            Column(
                modifier = modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                TextSubTitleBrownItalic(text = agente.agencia)
                TextTitleBrownRegular(text = agente.descricaoNome)
                TextRegularBrown(text = agente.lotacao)

            }
        }
        HorizontalDivider(modifier = Modifier)
    }
}

@Preview
@Composable
private fun ResultSearchAgenteScreenPreview() {
    FluviAppTheme {
        ResultSearchAgenteScreen(
            uiState = AgenteUiState(
                agencia = "AGENCIA LITORAL",
                listaAgencia = listaAgenteSample.map { it.agencia },
                resultadosListaAgente = listaAgenteSample.filter { it.agencia == "AGENCIA LITORAL" }
            )
        )
    }
}