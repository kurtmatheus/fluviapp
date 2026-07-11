package dev.matheus.fluviapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.components.forms.buttons.CommonIconButton
import dev.matheus.fluviapp.ui.components.forms.fields.FormTextFieldBrownLeadingIcon
import dev.matheus.fluviapp.ui.components.texts.TextRegularBrown
import dev.matheus.fluviapp.ui.screens.forms.CommonScreenNoBottom
import dev.matheus.fluviapp.ui.states.RecuperarSenhaUiState

@Composable
fun RecuperarSenhaScreen(
    state: RecuperarSenhaUiState,
    modifier: Modifier = Modifier,
    onClickVoltar: () -> Unit = {},
    onClickEnviar: () -> Unit = {},
) {
    CommonScreenNoBottom(
        titleTopAppBar = R.string.title_recuperar_senha,
        titleTopContent = 0,
        isShowRightIcon = false,
        hasRefresh = false,
        isRefreshing = false,
        onClickVoltar = onClickVoltar,
    ) { scaffoldModifier, _ ->
        Column(
            modifier = scaffoldModifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier.height(8.dp))

            TextRegularBrown(
                modifier = modifier.fillMaxWidth(),
                text = stringResource(R.string.msg_recuperar_senha_instrucao),
            )

            FormTextFieldBrownLeadingIcon(
                modifier = modifier.fillMaxWidth(),
                value = state.email,
                label = R.string.label_email,
                onValueChange = state.onEmailChange,
                isError = state.isEmailError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = stringResource(R.string.label_email),
                    )
                },
                textoErro = R.string.error_camp_obrig,
            )

            if (state.exibirMensagem) {
                Text(
                    text = stringResource(state.mensagem),
                    modifier = modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = if (state.isSucesso) MaterialTheme.colorScheme.primary else Color.Red,
                )
            }

            Spacer(modifier.height(8.dp))

            CommonIconButton(
                modifier = modifier,
                onClick = onClickEnviar,
                text = stringResource(R.string.btn_enviar_recuperacao),
                color = MaterialTheme.colorScheme.primary,
                isProcessing = state.enviando,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecuperarSenhaScreenPreview() {
    RecuperarSenhaScreen(
        state = RecuperarSenhaUiState(
            email = "operador@fluvi.app",
            exibirMensagem = true,
            isSucesso = true,
            mensagem = R.string.msg_recuperacao_enviada,
        ),
    )
}