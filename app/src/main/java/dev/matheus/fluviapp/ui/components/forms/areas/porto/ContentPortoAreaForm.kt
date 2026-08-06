package dev.matheus.fluviapp.ui.components.forms.areas.porto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.components.forms.dropdowns.DropDownFormField
import dev.matheus.fluviapp.ui.components.forms.fields.FormTextFieldBrownNoIcon
import dev.matheus.fluviapp.ui.states.FormPortoUiState

@Composable
fun ContentPortoAreaForm(
    modifier: Modifier,
    state: FormPortoUiState,
    onNomeChange: (String) -> Unit,
    onLocalidadeChange: (String) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // A mensagem de erro vem do próprio erro (`erroNome.mensagem`), e não de um `if` na tela: são
        // duas queixas diferentes — falta preencher × já existe aqui — e quem sabe qual é o estado.
        FormTextFieldBrownNoIcon(
            modifier = modifier.fillMaxWidth(),
            value = state.nome,
            label = R.string.label_nome_porto,
            onValueChange = onNomeChange,
            isError = state.erroNome.existe,
            textoErro = state.erroNome.mensagem,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
        )

        // Lista fechada por cadastro: o porto não escreve onde fica, escolhe entre as localidades que
        // existem. A tela mostra rótulos e devolve o rótulo escolhido; traduzir para id é do VM.
        DropDownFormField(
            listaItens = state.localidades.map { it.rotulo },
            label = R.string.label_localidade,
            modifier = modifier.fillMaxWidth(),
            value = state.rotuloLocalidade,
            onValueChange = onLocalidadeChange,
            isError = state.isLocalidadeError,
        )
    }
}