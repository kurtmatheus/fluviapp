package dev.matheus.fluviapp.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.matheus.fluviapp.BuildConfig
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.components.forms.buttons.CommonIconButton
import dev.matheus.fluviapp.ui.components.forms.fields.FormTextFieldBrownLeadingIcon
import dev.matheus.fluviapp.ui.components.forms.fields.FormTextFieldBrownLeadingTrailingIcon
import dev.matheus.fluviapp.ui.components.texts.TextRegularBrownItalic
import dev.matheus.fluviapp.ui.states.LoginUiState

@Composable
fun LoginScreen(
    state: LoginUiState,
    modifier: Modifier = Modifier,
    onClickVisibilitySenha: () -> Unit = {},
    onClickLogar: () -> Unit = {},
    onClickReenviar: () -> Unit = {},
    onClickCadastrar: () -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            modifier = modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo2),
                modifier = modifier
                    .size(250.dp),
                contentScale = ContentScale.Crop,
                contentDescription = stringResource(R.string.description_logo_do_app),
            )
        }

        if (!state.carregandoUsuarios) {
            Column(
                modifier
                    .padding(horizontal = 16.dp)
                    .weight(2f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state.exibirErro) {
                    Text(
                        text = stringResource(state.mensagemErro),
                        modifier = modifier
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = Color.Red,
                        fontSize = 14.sp
                    )
                }

                FormTextFieldBrownLeadingIcon(
                    modifier = modifier.fillMaxWidth(),
                    value = state.email,
                    label = R.string.label_email,
                    onValueChange = state.onUsuarioChange,
                    isError = state.isUsuarioError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = stringResource(id = R.string.description_campo_user)
                        )
                    },
                    textoErro = R.string.error_camp_obrig
                )

                FormTextFieldBrownLeadingTrailingIcon(
                    modifier = modifier.fillMaxWidth(),
                    value = state.senha,
                    label = R.string.label_senha,
                    onValueChange = state.onSenhaChange,
                    isError = state.isSenhaError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = stringResource(R.string.description_campo_senha)
                        )
                    },
                    trailingIcon = {
                        if (state.isSenhaVisible) {
                            IconButton(onClick = onClickVisibilitySenha) {
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = stringResource(R.string.description_visibilidade)
                                )
                            }
                        } else {
                            IconButton(onClick = onClickVisibilitySenha) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = stringResource(R.string.description_visibilidade)
                                )
                            }
                        }
                    },
                    visualTransformation = if (state.isSenhaVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    textoErro = R.string.error_camp_obrig
                )

                Spacer(modifier.height(16.dp))

                CommonIconButton(
                    modifier = modifier,
                    onClick = onClickLogar,
                    text = stringResource(R.string.btn_entrar),
                    color = MaterialTheme.colorScheme.primary,
                    isProcessing = state.logando
                )

                if (state.exibirReenviarVerificacao) {
                    TextButton(onClick = onClickReenviar) {
                        Text(text = stringResource(R.string.btn_reenviar_verificacao))
                    }
                }

                TextButton(onClick = onClickCadastrar) {
                    Text(text = stringResource(R.string.btn_criar_conta))
                }
            }
        } else {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = modifier.padding(10.dp))
                TextRegularBrownItalic(text = stringResource(id = R.string.msg_carreg_usuarios))
            }
        }

        Text(
            modifier = modifier
                .padding(20.dp)
                .fillMaxWidth(),
            text = "${stringResource(id = R.string.app_versao)} ${BuildConfig.VERSION_NAME}",
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ScreenLoginPreview() {
        LoginScreen(
            state = LoginUiState(
                mensagemErro = R.string.error_usuario_incorreto,
                exibirErro = true
            )
        )
}

@Preview(showBackground = true)
@Composable
fun ScreenLoginCarregandoUsuariosPreview() {
    LoginScreen(
        state = LoginUiState(
            carregandoUsuarios = true
        )
    )
}