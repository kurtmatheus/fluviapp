package dev.matheus.fluviapp.ui.components.forms.areas.embarcacao

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.viagem.TipoEmbarcacao
import dev.matheus.fluviapp.ui.components.forms.dropdowns.DropDownFormField
import dev.matheus.fluviapp.ui.components.forms.fields.FormTextFieldBrownNoIcon
import dev.matheus.fluviapp.ui.states.FormEmbarcacaoUiState

@Composable
fun ContentEmbarcacaoAreaForm(
    modifier: Modifier,
    state: FormEmbarcacaoUiState,
    onNomeChange: (String) -> Unit,
    onTipoChange: (String) -> Unit,
    onEmpresaChange: (String) -> Unit,
    onCapacidadeVeiculoChange: (String) -> Unit,
    onCapacidadeSuite2Change: (String) -> Unit,
    onCapacidadeSuite3Change: (String) -> Unit,
    onCapacidadeCamaroteChange: (String) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FormTextFieldBrownNoIcon(
            modifier = modifier.fillMaxWidth(),
            value = state.nome,
            label = R.string.label_nome_embarcacao,
            onValueChange = onNomeChange,
            isError = state.isNomeError,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Next,
            ),
        )

        // O que ela é, antes de quanto ela leva: é o tipo que decide se a próxima pergunta existe.
        // A tela não conhece o enum — mostra rótulos e devolve o rótulo escolhido; traduzir é do VM.
        DropDownFormField(
            listaItens = TipoEmbarcacao.entries.map { it.rotulo },
            label = R.string.label_tipo_embarcacao,
            modifier = modifier.fillMaxWidth(),
            value = state.tipo?.rotulo.orEmpty(),
            onValueChange = onTipoChange,
            isError = state.isTipoError,
        )

        // Vínculo N-1: a embarcação pertence a uma empresa (por nome).
        DropDownFormField(
            listaItens = state.listaEmpresas.map { it.nome },
            label = R.string.label_empresa,
            modifier = modifier.fillMaxWidth(),
            value = state.empresa,
            onValueChange = onEmpresaChange,
            isError = state.isEmpresaError,
        )

        // A pergunta só existe para quem leva veículo (ADR-0016 §8). Some para a lancha em vez de aparecer
        // desabilitada: campo cinza diz "aqui vai um número que você não pode dar"; ausência diz a
        // verdade — esta embarcação não tem essa medida.
        if (state.perguntaCapacidadeVeiculo) {
            FormTextFieldBrownNoIcon(
                modifier = modifier.fillMaxWidth(),
                value = state.capacidadeVeiculo,
                label = R.string.label_capacidade_veiculo,
                onValueChange = onCapacidadeVeiculoChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            )
        }

        FormTextFieldBrownNoIcon(
            modifier = modifier.fillMaxWidth(),
            value = state.capacidadeSuite2,
            label = R.string.label_capacidade_suite2,
            onValueChange = onCapacidadeSuite2Change,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        )

        FormTextFieldBrownNoIcon(
            modifier = modifier.fillMaxWidth(),
            value = state.capacidadeSuite3,
            label = R.string.label_capacidade_suite3,
            onValueChange = onCapacidadeSuite3Change,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        )

        FormTextFieldBrownNoIcon(
            modifier = modifier.fillMaxWidth(),
            value = state.capacidadeCamarote,
            label = R.string.label_capacidade_camarote,
            onValueChange = onCapacidadeCamaroteChange,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        )
    }
}
