package dev.matheus.fluviapp.ui.screens.passagem.emissao

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.documento.TipoDocumento
import dev.matheus.fluviapp.ui.components.forms.dropdowns.DropDownFormField
import dev.matheus.fluviapp.ui.components.forms.fields.FormFieldCalendario
import dev.matheus.fluviapp.ui.components.forms.fields.FormTextFieldBrownNoIcon
import dev.matheus.fluviapp.ui.states.passagem.ClienteEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.ErroDeEmissao
import dev.matheus.fluviapp.util.visualtransformation.keyboardType
import dev.matheus.fluviapp.util.visualtransformation.visualTransformation

/**
 * **O formulário de pessoa** — um dos dois lugares do fluxo em que ainda se digita ([ADR-0029] D1).
 *
 * Quatro campos, e três deles são obrigatórios porque a **chave natural do pool** depende disso: *"não existe
 * criança sem documento nesse negócio"* ([ADR-0018] D4). O telefone é o único que não identifica ninguém — ele
 * existe para **alcançar** a pessoa —, e por isso é o único opcional.
 *
 * ### O tipo do documento governa o campo seguinte
 *
 * Máscara e teclado saem do [TipoDocumento] escolhido (`visualTransformation()`, `keyboardType()`), e não de
 * um `when` nesta tela. É o que o [ADR-0020] D2 fixou ao tirar o documento do catálogo genérico: um tipo novo
 * cadastrado por administrador caía no `else` e ficava **sem máscara e sem validação**.
 */
@Composable
fun FormularioDeCliente(
    cliente: ClienteEmEdicao,
    aoMudar: (ClienteEmEdicao) -> Unit,
    erros: Set<ErroDeEmissao>,
    modifier: Modifier = Modifier,
) {
    // Um erro só neste passo, e ele é sobre a pessoa inteira: qual campo falta o operador vê pelo que está
    // em branco. Marcar os quatro de vermelho ao mesmo tempo diria menos, não mais.
    val incompleto = erros.isNotEmpty()

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FormTextFieldBrownNoIcon(
            modifier = Modifier.fillMaxWidth(),
            value = cliente.nome,
            label = R.string.label_nome_passageiro,
            onValueChange = { aoMudar(cliente.copy(nome = it)) },
            isError = incompleto && cliente.nome.isBlank(),
        )

        DropDownFormField(
            modifier = Modifier.fillMaxWidth(),
            listaItens = TipoDocumento.entries.map { it.rotulo },
            label = R.string.label_tipo_documento,
            value = cliente.tipoDocumento?.rotulo.orEmpty(),
            isError = incompleto && cliente.tipoDocumento == null,
            // O dropdown devolve o **rótulo**; o tipo se resolve por ele, e não pelo `name` — é a mesma
            // separação que o `porRotulo` da Acomodação fez: um lê o que o Firestore gravou, o outro lê o
            // que a pessoa escolheu.
            onValueChange = { rotulo ->
                val tipo = TipoDocumento.entries.firstOrNull { it.rotulo.equals(rotulo, ignoreCase = true) }
                aoMudar(cliente.copy(tipoDocumento = tipo, numeroDocumento = ""))
            },
        )

        FormTextFieldBrownNoIcon(
            modifier = Modifier.fillMaxWidth(),
            value = cliente.numeroDocumento,
            label = R.string.label_documento,
            onValueChange = { aoMudar(cliente.copy(numeroDocumento = it)) },
            enabled = cliente.tipoDocumento != null,
            visualTransformation = cliente.tipoDocumento.visualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = cliente.tipoDocumento.keyboardType()),
            isError = incompleto && cliente.tipoDocumento != null && cliente.paraCliente() == null,
            textoErro = R.string.error_documento_invalido,
        )

        FormFieldCalendario(
            modifier = Modifier.fillMaxWidth(),
            focusManager = LocalFocusManager.current,
            value = cliente.dataNascimento,
            onValueChange = { aoMudar(cliente.copy(dataNascimento = it)) },
            label = R.string.label_data_nascimento,
            isError = incompleto && cliente.dataNascimento.isBlank(),
            textoErro = R.string.error_camp_obrig,
        )

        FormTextFieldBrownNoIcon(
            modifier = Modifier.fillMaxWidth(),
            value = cliente.telefone,
            label = R.string.label_telefone_cliente,
            onValueChange = { aoMudar(cliente.copy(telefone = it)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        )
    }
}