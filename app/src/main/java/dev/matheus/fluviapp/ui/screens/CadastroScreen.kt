package dev.matheus.fluviapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.components.forms.buttons.CommonIconButton
import dev.matheus.fluviapp.ui.components.forms.fields.FormTextFieldBrownLeadingIcon
import dev.matheus.fluviapp.ui.components.forms.fields.FormTextFieldBrownLeadingTrailingIcon
import dev.matheus.fluviapp.ui.screens.forms.CommonScreenNoBottom
import dev.matheus.fluviapp.ui.states.CadastroUiState

@Composable
fun CadastroScreen(
    state: CadastroUiState,
    modifier: Modifier = Modifier,
    onClickVoltar: () -> Unit = {},
    onClickVisibilitySenha: () -> Unit = {},
    onClickCadastrar: () -> Unit = {},
) {
    CommonScreenNoBottom(
        titleTopAppBar = R.string.title_cadastro,
        titleTopContent = 0,
        isShowRightIcon = false,
        hasRefresh = false,
        isRefreshing = false,
        onClickVoltar = onClickVoltar,
    ) { scaffoldModifier, _ ->
        Column(
            modifier = scaffoldModifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.exibirErro) {
                Text(
                    text = stringResource(state.mensagemErro),
                    modifier = modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = Color.Red,
                )
            }

            FormTextFieldBrownLeadingIcon(
                modifier = modifier.fillMaxWidth(),
                value = state.nome,
                label = R.string.label_nome_completo,
                onValueChange = state.onNomeChange,
                isError = state.isNomeError,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = stringResource(R.string.description_campo_user),
                    )
                },
                textoErro = R.string.error_camp_obrig,
            )

            FormTextFieldBrownLeadingIcon(
                modifier = modifier.fillMaxWidth(),
                value = state.email,
                label = R.string.label_email,
                onValueChange = state.onEmailChange,
                isError = state.isEmailError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = stringResource(R.string.description_campo_user),
                    )
                },
                textoErro = R.string.error_camp_obrig,
            )

            FormTextFieldBrownLeadingTrailingIcon(
                modifier = modifier.fillMaxWidth(),
                value = state.senha,
                label = R.string.label_senha,
                onValueChange = state.onSenhaChange,
                isError = state.isSenhaError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = stringResource(R.string.description_campo_senha),
                    )
                },
                trailingIcon = {
                    IconButton(onClick = onClickVisibilitySenha) {
                        Icon(
                            imageVector = if (state.isSenhaVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = stringResource(R.string.description_visibilidade),
                        )
                    }
                },
                visualTransformation = if (state.isSenhaVisible) VisualTransformation.None else PasswordVisualTransformation(),
                textoErro = R.string.error_senha_curta,
            )

            FormTextFieldBrownLeadingTrailingIcon(
                modifier = modifier.fillMaxWidth(),
                value = state.confirmarSenha,
                label = R.string.label_confirmar_senha,
                onValueChange = state.onConfirmarSenhaChange,
                isError = state.isConfirmarSenhaError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = stringResource(R.string.description_campo_senha),
                    )
                },
                trailingIcon = {
                    IconButton(onClick = onClickVisibilitySenha) {
                        Icon(
                            imageVector = if (state.isSenhaVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = stringResource(R.string.description_visibilidade),
                        )
                    }
                },
                visualTransformation = if (state.isSenhaVisible) VisualTransformation.None else PasswordVisualTransformation(),
                textoErro = R.string.error_senhas_diferentes,
            )

            Spacer(modifier.height(16.dp))

            CommonIconButton(
                modifier = modifier,
                onClick = onClickCadastrar,
                text = stringResource(R.string.btn_criar_conta),
                color = MaterialTheme.colorScheme.primary,
                isProcessing = state.cadastrando,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CadastroScreenPreview() {
    CadastroScreen(
        state = CadastroUiState(
            nome = "Fulano",
            email = "a@b.com",
            isConfirmarSenhaError = true,
            exibirErro = true,
            mensagemErro = R.string.error_senhas_diferentes,
        ),
    )
}