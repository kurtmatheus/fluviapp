package dev.matheus.fluviapp.ui.components.forms.fields

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R

/**
 * **O campo de observação** — o único texto livre da emissão, e por isso o único que precisa parecer livre.
 *
 * Duas coisas o distinguem de um campo comum, e as duas vêm do balcão:
 *
 * 1. **ele é alto**. Uma linha convida a escrever uma palavra; três dizem que cabe uma frase. A observação é
 *    onde entra o que o sistema não tem campo para guardar — "vai desembarcar em X", "leva carga de mão" —,
 *    e um campo baixo faz o operador desistir de escrever;
 * 2. **ele tem microfone**, e isso não é adorno: quem atende com fila na frente digita mal e devagar. O
 *    reconhecimento é o do próprio Android ([RecognizerIntent]), em `pt-BR` — o mesmo que o formulário
 *    anterior à revitalização usava, e a única coisa daquela tela que valia a pena trazer de volta inteira.
 *
 * O texto ditado **substitui** o conteúdo em vez de concatenar, que é como o campo antigo se comportava: o
 * ditado é uma frase pensada de uma vez, e emendá-la ao que já estava produz observação sem pontuação.
 */
@Composable
fun CampoDeObservacao(
    valor: String,
    aoMudar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ouvinte = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { resultado ->
        if (resultado.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val falado = resultado.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!falado.isNullOrBlank()) aoMudar(falado)
    }

    OutlinedTextField(
        modifier = modifier.fillMaxWidth().heightIn(min = 120.dp),
        value = valor,
        onValueChange = aoMudar,
        label = { Text(stringResource(R.string.label_observacao)) },
        // Sem `singleLine`: o campo cresce com o texto, e o Enter quebra linha em vez de fechar o teclado.
        minLines = 3,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Default,
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onBackground,
            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
            focusedBorderColor = MaterialTheme.colorScheme.onBackground,
            unfocusedBorderColor = MaterialTheme.colorScheme.onBackground,
            focusedLabelColor = MaterialTheme.colorScheme.onBackground,
            unfocusedLabelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
        ),
        trailingIcon = {
            IconButton(onClick = { ouvinte.launch(intencaoDeFala()) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_microfone_24),
                    contentDescription = stringResource(R.string.description_mic),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        },
    )
}

/** O reconhecimento de fala do sistema, em português — texto livre, sem gramática nem vocabulário fixo. */
private fun intencaoDeFala() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
}