package dev.matheus.fluviapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.unit.sp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.components.forms.buttons.CommonIconButton
import dev.matheus.fluviapp.ui.components.forms.fields.FormTextFieldBrownLeadingTrailingIcon
import dev.matheus.fluviapp.ui.components.texts.FluviWordmark
import dev.matheus.fluviapp.ui.components.texts.TextRegularBrownItalic
import dev.matheus.fluviapp.ui.states.PrimeiroAcessoUiState

/**
 * Criar a própria senha no primeiro acesso (ADR-0015 §2.1). Não é cadastro: a conta e o registro na
 * equipe já existem — o que esta tela faz é trocar a senha padrão e dar nascimento ao perfil.
 */
@Composable
fun PrimeiroAcessoScreen(
    state: PrimeiroAcessoUiState,
    modifier: Modifier = Modifier,
    onSenhaChange: (String) -> Unit = {},
    onConfirmacaoChange: (String) -> Unit = {},
    onClickVisibilidade: () -> Unit = {},
    onClickConfirmar: () -> Unit = {},
) {
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            modifier = modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            FluviWordmark(modifier = modifier.size(200.dp), fontSize = 36.sp)
        }

        Column(
            modifier
                .padding(horizontal = 16.dp)
                .weight(2f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.title_primeiro_acesso),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )

            // Confirma à pessoa que ela é quem a gestão cadastrou, antes de pedir uma senha nova.
            TextRegularBrownItalic(
                text = if (state.nome.isNotBlank()) {
                    stringResource(R.string.msg_primeiro_acesso_ola, state.nome)
                } else {
                    stringResource(R.string.msg_primeiro_acesso)
                },
            )

            if (state.mensagemErro != 0) {
                Text(
                    text = stringResource(state.mensagemErro),
                    modifier = modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = Color.Red,
                    fontSize = 14.sp,
                )
            }

            CampoSenha(
                modifier = modifier,
                value = state.senha,
                label = R.string.label_nova_senha,
                isError = state.isSenhaError,
                isVisible = state.isSenhaVisible,
                onValueChange = onSenhaChange,
                onClickVisibilidade = onClickVisibilidade,
            )

            CampoSenha(
                modifier = modifier,
                value = state.confirmacao,
                label = R.string.label_confirmar_senha,
                isError = state.isConfirmacaoError,
                isVisible = state.isSenhaVisible,
                onValueChange = onConfirmacaoChange,
                onClickVisibilidade = onClickVisibilidade,
                imeAction = ImeAction.Done,
            )

            Spacer(modifier.height(16.dp))

            CommonIconButton(
                modifier = modifier,
                onClick = onClickConfirmar,
                text = stringResource(R.string.btn_criar_senha),
                color = MaterialTheme.colorScheme.primary,
                isProcessing = state.processando,
            )
        }
    }
}

@Composable
private fun CampoSenha(
    modifier: Modifier,
    value: String,
    label: Int,
    isError: Boolean,
    isVisible: Boolean,
    onValueChange: (String) -> Unit,
    onClickVisibilidade: () -> Unit,
    imeAction: ImeAction = ImeAction.Next,
) {
    FormTextFieldBrownLeadingTrailingIcon(
        modifier = modifier.fillMaxWidth(),
        value = value,
        label = label,
        onValueChange = onValueChange,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = stringResource(R.string.description_campo_senha),
            )
        },
        trailingIcon = {
            IconButton(onClick = onClickVisibilidade) {
                Icon(
                    imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = stringResource(R.string.description_visibilidade),
                )
            }
        },
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        textoErro = R.string.error_camp_obrig,
    )
}

@Preview(showBackground = true)
@Composable
private fun PrimeiroAcessoScreenPreview() {
    PrimeiroAcessoScreen(state = PrimeiroAcessoUiState(nome = "Ana Ribeiro"))
}
