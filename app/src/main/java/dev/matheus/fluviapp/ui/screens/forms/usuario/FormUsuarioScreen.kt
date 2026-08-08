package dev.matheus.fluviapp.ui.screens.forms.usuario

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.operacoes.Usuario
import dev.matheus.fluviapp.ui.components.forms.areas.CommonAreaForm
import dev.matheus.fluviapp.ui.components.forms.buttons.CommonIconButton
import dev.matheus.fluviapp.ui.components.forms.dropdowns.DropDownFormField
import dev.matheus.fluviapp.ui.components.forms.fields.FormTextFieldBrownNoIcon
import dev.matheus.fluviapp.ui.components.texts.TextRegularBrown
import dev.matheus.fluviapp.ui.screens.forms.CommonScreenNoBottom
import dev.matheus.fluviapp.ui.states.EmpresaOpcao
import dev.matheus.fluviapp.ui.states.FormUsuarioUiState

@Composable
fun FormUsuarioScreen(
    uiState: FormUsuarioUiState,
    onNomeChange: (String) -> Unit = {},
    onEmailChange: (String) -> Unit = {},
    onPapelChange: (String) -> Unit = {},
    onEmpresaChange: (String) -> Unit = {},
    onCargoChange: (String) -> Unit = {},
    onClickSalvar: () -> Unit = {},
    onClickVoltar: () -> Unit = {},
) {
    CommonScreenNoBottom(
        titleTopAppBar = R.string.title_top_usuarios,
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
            CommonAreaForm(modifier = modifier, titleArea = uiState.titulo) {
                FormTextFieldBrownNoIcon(
                    modifier = it,
                    value = uiState.nome,
                    label = R.string.label_nome_usuario,
                    onValueChange = onNomeChange,
                    isError = uiState.isNomeError,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )

                // O e-mail é a chave: é o id do convite e é por ele que o primeiro acesso se encontra.
                FormTextFieldBrownNoIcon(
                    modifier = it,
                    value = uiState.email,
                    label = R.string.label_email,
                    onValueChange = onEmailChange,
                    isError = uiState.isEmailError,
                    textoErro = R.string.error_email_invalido,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )

                DropDownFormField(
                    modifier = it.fillMaxWidth(),
                    listaItens = uiState.papeis,
                    label = R.string.label_papel,
                    value = uiState.papel?.name.orEmpty(),
                    isError = uiState.isPapelError,
                    onValueChange = onPapelChange,
                )

                // A segunda metade só existe para o operador: `ADM`/`GESTOR` não atuam em empresa
                // nenhuma (§8.1), e um campo que nunca terá resposta não deve nem aparecer.
                if (uiState.perguntaVinculo) {
                    DropDownFormField(
                        modifier = it.fillMaxWidth(),
                        listaItens = uiState.empresas.map { empresa -> empresa.nome },
                        label = R.string.label_empresa,
                        value = uiState.empresa,
                        isError = uiState.isEmpresaError,
                        onValueChange = onEmpresaChange,
                    )
                    DropDownFormField(
                        modifier = it.fillMaxWidth(),
                        listaItens = uiState.cargos,
                        label = R.string.label_cargo,
                        value = uiState.cargo,
                        isError = uiState.isCargoError,
                        onValueChange = onCargoChange,
                    )
                }

                TextRegularBrown(text = stringResource(R.string.msg_convite_explicacao))
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
private fun FormUsuarioScreenPreview() {
    FormUsuarioScreen(
        uiState = FormUsuarioUiState(
            nome = "Ana Ribeiro",
            email = "ana.ribeiro@fluviapp.com.br",
            papel = Usuario.Papel.OPERADOR,
            empresa = "Navegação Norte",
            empresas = listOf(EmpresaOpcao("e1", "Navegação Norte")),
        ),
    )
}