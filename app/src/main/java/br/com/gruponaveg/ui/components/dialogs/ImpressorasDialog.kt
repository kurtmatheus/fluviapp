package br.com.gruponaveg.ui.components.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.gruponaveg.R
import br.com.gruponaveg.model.screendata.DadosImpressora
import br.com.gruponaveg.sampledata.listaDadosImpressoraSample
import br.com.gruponaveg.ui.components.texts.TextRegularBrownItalic
import br.com.gruponaveg.ui.states.ImpressaoState

@Composable
fun ImpressorasDialog(
    modifier: Modifier,
    state: ImpressaoState,
    onDismiss: () -> Unit,
    onSelecionaImpressora: (DadosImpressora) -> Unit,
    onParearNovaImpressora: () -> Unit
) {
    CommonExpansiveDialog(
        modifier = modifier,
        onDismiss = onDismiss,
        containerColor = MaterialTheme.colorScheme.onPrimary,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(10.dp, 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TextRegularBrownItalic(text = stringResource(id = R.string.label_impressoras_pareadas))
            HorizontalDivider(
                modifier = modifier
                    .fillMaxWidth(),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onBackground
            )

            LazyColumn {
                items(state.listaImpressorasPareadas) {
                    Row(
                        modifier = modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clickable {
                                onSelecionaImpressora(it)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            modifier = modifier.size(30.dp),
                            imageVector = Icons.Outlined.Print,
                            contentDescription = stringResource(id = R.string.description_impressora)
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            TextRegularBrownItalic(text = it.nome)
                            TextRegularBrownItalic(text = it.endereco)
                        }
                    }
                }
            }
        }

        HorizontalDivider(thickness = 1.dp)
        IconButton(
            modifier = modifier.fillMaxWidth(),
            onClick = onParearNovaImpressora
        ) {
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add_75),
                    contentDescription = stringResource(R.string.description_adicionar),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ImpressorasDialogPreview() {
    ImpressorasDialog(
        modifier = Modifier,
        state = ImpressaoState(
            listaImpressorasPareadas = listaDadosImpressoraSample
        ),
        onDismiss = {},
        onSelecionaImpressora = {},
        onParearNovaImpressora = {}
    )
}