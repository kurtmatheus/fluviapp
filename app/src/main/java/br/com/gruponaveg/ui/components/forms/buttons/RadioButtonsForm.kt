package br.com.gruponaveg.ui.components.forms.buttons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.gruponaveg.R
import br.com.gruponaveg.sampledata.listaFormaPagamentoSample
import br.com.gruponaveg.ui.components.texts.SupportingText
import br.com.gruponaveg.ui.components.texts.SupportingTextRed
import br.com.gruponaveg.ui.components.texts.TextRegularBrown
import br.com.gruponaveg.ui.theme.NavyBlue

@Composable
fun FormRadioButtons(
    modifier: Modifier,
    opcoes: List<String>,
    isOpcaoSelecionada: (String) -> Boolean = { false },
    onOpcaoSelecionadaChange: (String) -> Unit = {},
    isError: Boolean = false
) {
    val radioButtonColors = if(isError) RadioButtonDefaults.colors(
        selectedColor = Red,
        unselectedColor = Red,
        disabledSelectedColor = NavyBlue,
        disabledUnselectedColor = Color.LightGray
    ) else RadioButtonDefaults.colors(
        selectedColor = MaterialTheme.colorScheme.primary,
        unselectedColor = MaterialTheme.colorScheme.onBackground,
        disabledSelectedColor = NavyBlue,
        disabledUnselectedColor = Color.LightGray
    )
    Column {
        Row(
            modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            opcoes.forEach { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = modifier
                        .selectable(
                            selected = isOpcaoSelecionada(item),
                            onClick = { onOpcaoSelecionadaChange(item) },
                            role = Role.RadioButton
                        )
                        .padding(8.dp)
                ) {
                    RadioButton(
                        selected = isOpcaoSelecionada(item),
                        onClick = null,
                        colors = radioButtonColors
                    )
                    if (isError) SupportingTextRed(text = item)
                    else TextRegularBrown(text = item)
                }
            }
        }

        if (isError) {
            SupportingText(
                modifier = modifier.padding(10.dp, 0.dp),
                text = stringResource(id = R.string.error_camp_obrig)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FormRadioButtonPreview() {
        FormRadioButtons(
            modifier = Modifier.padding(10.dp),
            opcoes = listaFormaPagamentoSample.map { it.descricaoNome }
        )
}

@Preview(showBackground = true)
@Composable
private fun FormRadioButtonErrorPreview() {
    FormRadioButtons(
        modifier = Modifier.padding(10.dp),
        opcoes = listaFormaPagamentoSample.map { it.descricaoNome },
        isError = true
    )
}