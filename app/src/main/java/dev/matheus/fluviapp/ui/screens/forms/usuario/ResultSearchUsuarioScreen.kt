package dev.matheus.fluviapp.ui.screens.forms.usuario

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.components.contents.CommonTopRow
import dev.matheus.fluviapp.ui.components.forms.divider.FormDashedDivider
import dev.matheus.fluviapp.ui.components.forms.fields.FormTextFieldBrownNoIcon
import dev.matheus.fluviapp.ui.components.texts.TextRegularBrown
import dev.matheus.fluviapp.ui.components.texts.TextSubTitleBrownItalic
import dev.matheus.fluviapp.ui.components.texts.TextTitleBrownRegular
import dev.matheus.fluviapp.ui.screens.forms.CommonScreenNoBottom
import dev.matheus.fluviapp.ui.states.PesquisaUsuarioUiState
import dev.matheus.fluviapp.ui.states.UsuarioResultado
import dev.matheus.fluviapp.ui.theme.FluviAppTheme

/**
 * A lista de usuários — **somente leitura** (ADR-0021 D2). Sem editar e sem revogar: o papel de quem já
 * entrou vive em `users/{uid}`, que a regra torna imutável pelo cliente, e um botão que não cumpre é
 * pior do que a ausência dele.
 */
@Composable
fun ResultSearchUsuarioScreen(
    uiState: PesquisaUsuarioUiState,
    onEmailChange: (String) -> Unit = {},
    onClickVoltar: () -> Unit = {},
) {
    CommonScreenNoBottom(
        titleTopAppBar = R.string.title_top_usuarios,
        titleTopContent = R.string.subtitle_pesquisar_usuarios,
        isShowRightIcon = false,
        hasRefresh = false,
        isRefreshing = false,
        onClickVoltar = onClickVoltar,
    ) { modifier, titulo ->
        Column {
            CommonTopRow(modifier = modifier, titulo = titulo)

            Column(
                modifier = modifier.padding(10.dp, 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FormTextFieldBrownNoIcon(
                    modifier = modifier.fillMaxWidth(),
                    value = uiState.email,
                    label = R.string.label_email,
                    onValueChange = onEmailChange,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Search,
                    ),
                )
            }
            FormDashedDivider(modifier = modifier.fillMaxWidth())

            LazyColumn {
                items(uiState.resultados) { usuario ->
                    CardResultUsuario(modifier = modifier, usuario = usuario)
                }
            }
        }
    }
}

@Composable
fun CardResultUsuario(
    modifier: Modifier,
    usuario: UsuarioResultado,
) {
    Column {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(10.dp, 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                TextTitleBrownRegular(text = usuario.nome)
                TextRegularBrown(text = usuario.email)
                // O papel é o que ele pode no app; o vínculo, o que ele faz na operação. Só operador
                // tem o segundo — e é essa ausência que diz que a plataforma não atua em empresa nenhuma.
                TextRegularBrown(text = usuario.papel)
                if (usuario.vinculo.isNotBlank()) {
                    TextRegularBrown(text = usuario.vinculo)
                }
            }

            // Convidado × Ativo: o convite não some quando é usado — vira registro.
            TextSubTitleBrownItalic(text = usuario.situacao)
        }
        HorizontalDivider(modifier = Modifier)
    }
}

@Preview
@Composable
private fun ResultSearchUsuarioScreenPreview() {
    FluviAppTheme {
        ResultSearchUsuarioScreen(
            uiState = PesquisaUsuarioUiState(
                resultados = listOf(
                    UsuarioResultado(
                        email = "adm@fluviapp.com.br",
                        nome = "Kurt",
                        papel = "ADM",
                        vinculo = "",
                        situacao = "Ativo",
                    ),
                    UsuarioResultado(
                        email = "ana.ribeiro@fluviapp.com.br",
                        nome = "Ana Ribeiro",
                        papel = "OPERADOR",
                        vinculo = "Navegação Norte · SUPERVISOR",
                        situacao = "Convidado",
                    ),
                ),
            ),
        )
    }
}