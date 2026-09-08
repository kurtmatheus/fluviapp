package dev.matheus.fluviapp.ui.screens.forms.usuario

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.components.contents.CommonTopRow
import dev.matheus.fluviapp.ui.components.dialogs.CommonInformativeDialog
import dev.matheus.fluviapp.ui.components.forms.divider.FormDashedDivider
import dev.matheus.fluviapp.ui.components.forms.fields.FormFieldCalendario
import dev.matheus.fluviapp.ui.components.forms.fields.FormTextFieldBrownNoIcon
import dev.matheus.fluviapp.ui.components.texts.TextRegularBrown
import dev.matheus.fluviapp.ui.components.texts.TextSubTitleBrownItalic
import dev.matheus.fluviapp.ui.components.texts.TextTitleBrownRegular
import dev.matheus.fluviapp.ui.screens.forms.CommonScreenNoBottom
import dev.matheus.fluviapp.ui.states.PesquisaUsuarioUiState
import dev.matheus.fluviapp.ui.states.UsuarioResultado
import dev.matheus.fluviapp.ui.theme.FluviAppTheme

/**
 * A lista de usuários — e **a gestão de acesso** ([ADR-0032] D6), que supera o ADR-0021 D2.
 *
 * Ela era somente-leitura por decisão: o papel de quem já entrou vive em `users/{uid}`, que a regra
 * tornava imutável pelo cliente, e um botão que não cumpre é pior do que a ausência dele. O que mudou não
 * foi a opinião sobre botões — foi a regra: o `ADM` passou a escrever `ativo` e `expiraEm`.
 *
 * **O papel continua sem botão**, e agora por escrito: ele está fora da lista fechada de chaves que a
 * regra abre, inclusive para o `ADM`. Trocar papel segue sendo ato de console.
 *
 * ### Dois gestos por linha, e nenhum para quem não entrou
 *
 * Desativar/reativar tem **confirmação** — mesmo molde dos deletes lógicos das outras seções, e pela mesma
 * razão: é gesto que atinge outra pessoa. O prazo abre o seletor de data direto, porque escolher uma data
 * já é a confirmação, e um diálogo antes do seletor seria cerimônia sobre cerimônia.
 *
 * Quem só foi convidado não tem nenhum dos dois: não há acesso a governar antes do primeiro acesso.
 */
@Composable
fun ResultSearchUsuarioScreen(
    uiState: PesquisaUsuarioUiState,
    onEmailChange: (String) -> Unit = {},
    onClickVoltar: () -> Unit = {},
    onAlternarAcesso: (String, Boolean) -> Unit = { _, _ -> },
    onDefinirPrazo: (String, String) -> Unit = { _, _ -> },
) {
    CommonScreenNoBottom(
        titleTopAppBar = R.string.title_top_usuarios,
        titleTopContent = R.string.subtitle_pesquisar_usuarios,
        isShowRightIcon = false,
        hasRefresh = false,
        isRefreshing = false,
        onClickVoltar = onClickVoltar,
    ) { modifier, titulo ->
        // Quem está marcado para ter o acesso alternado (estado local de UI); != null abre a confirmação.
        var paraAlternar by remember { mutableStateOf<UsuarioResultado?>(null) }

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
                    CardResultUsuario(
                        modifier = modifier,
                        usuario = usuario,
                        podeGerir = uiState.podeGerir,
                        onAlternarAcesso = { paraAlternar = it },
                        onDefinirPrazo = onDefinirPrazo,
                    )
                }
            }
        }

        paraAlternar?.let { usuario ->
            CommonInformativeDialog(
                modifier = Modifier,
                textMensagem = if (usuario.ativo) {
                    R.string.msg_confirmar_desativar_acesso
                } else {
                    R.string.msg_confirmar_reativar_acesso
                },
                textConfirm = if (usuario.ativo) R.string.btn_desativar else R.string.btn_reativar,
                textDismiss = R.string.btn_cancelar,
                onConfirm = {
                    onAlternarAcesso(usuario.id, !usuario.ativo)
                    paraAlternar = null
                },
                onDismiss = { paraAlternar = null },
            )
        }
    }
}

@Composable
fun CardResultUsuario(
    modifier: Modifier,
    usuario: UsuarioResultado,
    podeGerir: Boolean = false,
    onAlternarAcesso: (UsuarioResultado) -> Unit = {},
    onDefinirPrazo: (String, String) -> Unit = { _, _ -> },
) {
    // O seletor de data abre num campo, e o campo só existe enquanto está aberto: uma linha de lista com
    // campo de data permanente seria um formulário disfarçado de lista.
    var escolhendoPrazo by remember { mutableStateOf(false) }

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
                // O prazo aparece só quando existe: "sem prazo" é o caso normal, e escrevê-lo em toda
                // linha faria o normal parecer exceção.
                if (usuario.prazo.isNotBlank()) {
                    TextRegularBrown(text = stringResource(R.string.label_acesso_ate, usuario.prazo))
                }
            }

            // Ativo · Desativado · Expirado · Convidado — a situação vem de onde ela mora (D6/Q1).
            TextSubTitleBrownItalic(text = usuario.situacao)

            if (podeGerir && usuario.temAcesso) {
                IconButton(onClick = { escolhendoPrazo = true }) {
                    Icon(
                        imageVector = Icons.Default.EventBusy,
                        contentDescription = stringResource(R.string.description_prazo_de_acesso),
                    )
                }
                IconButton(onClick = { onAlternarAcesso(usuario) }) {
                    Icon(
                        imageVector = if (usuario.ativo) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = stringResource(
                            if (usuario.ativo) R.string.btn_desativar else R.string.btn_reativar
                        ),
                    )
                }
            }
        }

        if (escolhendoPrazo) {
            FormFieldCalendario(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                focusManager = LocalFocusManager.current,
                value = usuario.prazo,
                label = R.string.label_acesso_expira_em,
                onValueChange = { data ->
                    onDefinirPrazo(usuario.id, data)
                    escolhendoPrazo = false
                },
                isError = false,
                textoErro = R.string.error_camp_obrig,
            )
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
                podeGerir = true,
                resultados = listOf(
                    UsuarioResultado(
                        id = "uid-adm",
                        email = "adm@fluviapp.com.br",
                        nome = "Kurt",
                        papel = "ADM",
                        vinculo = "",
                        situacao = "Ativo",
                        ativo = true,
                        temAcesso = true,
                    ),
                    UsuarioResultado(
                        id = "uid-ana",
                        email = "ana.ribeiro@fluviapp.com.br",
                        nome = "Ana Ribeiro",
                        papel = "OPERADOR",
                        vinculo = "Navegação Norte · SUPERVISOR",
                        situacao = "Expirado",
                        prazo = "31/08/2026",
                        ativo = true,
                        temAcesso = true,
                    ),
                    UsuarioResultado(
                        id = "uid-bruno",
                        email = "bruno.costa@fluviapp.com.br",
                        nome = "Bruno Costa",
                        papel = "OPERADOR",
                        vinculo = "Navegação Norte · AGENTE",
                        situacao = "Desativado",
                        ativo = false,
                        temAcesso = true,
                    ),
                    UsuarioResultado(
                        id = "",
                        email = "carla.dias@fluviapp.com.br",
                        nome = "Carla Dias",
                        papel = "GESTOR",
                        vinculo = "",
                        situacao = "Convidado",
                    ),
                ),
            ),
        )
    }
}
