package dev.matheus.fluviapp.ui.screens.passagem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.components.forms.buttons.CommonIconButton
import dev.matheus.fluviapp.ui.components.passagem.BilheteCapturavel
import dev.matheus.fluviapp.ui.components.texts.TextRegularBrownItalic
import dev.matheus.fluviapp.ui.screens.forms.CommonScreenNoBottom
import dev.matheus.fluviapp.ui.viewmodel.passagem.BilheteUiState

/**
 * **A tela do bilhete** — a mesma para quem acabou de emitir e para quem foi buscar um bilhete antigo.
 *
 * Ela desenha e **grava ao desenhar**: não há botão de salvar, porque ver o bilhete e tê-lo salvo são o mesmo
 * ato (decisão do analista). O único gesto é **compartilhar**, que é o que entrega o documento ao passageiro.
 *
 * O aviso de que o arquivo está na galeria aparece **depois** de gravado, e não como promessa: é a diferença
 * entre dizer "salvamos" e mostrar que salvou.
 */
@Composable
fun BilheteScreen(
    state: BilheteUiState,
    onClickVoltar: () -> Unit = {},
    onCapturar: (ImageBitmap) -> Unit = {},
    onCompartilhar: () -> Unit = {},
    onNovaPassagem: (String) -> Unit = {},
) {
    CommonScreenNoBottom(
        titleTopAppBar = R.string.title_top_passagem,
        titleTopContent = R.string.subtitle_bilhete_digital,
        isShowRightIcon = false,
        hasRefresh = false,
        isRefreshing = false,
        onClickVoltar = onClickVoltar,
    ) { modifier, _ ->
        Column(
            modifier = modifier.fillMaxSize().navigationBarsPadding().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                state.carregando && state.bilhete == null -> Row(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }

                state.naoEncontrado -> TextRegularBrownItalic(
                    modifier = Modifier.padding(24.dp),
                    text = "Não foi possível carregar este bilhete.",
                )

                state.bilhete != null -> {
                    // A pré-visualização **é** o que se grava: uma renderização só, e não uma para ver e
                    // outra escondida para capturar, como fazia o caminho antigo.
                    BilheteCapturavel(
                        bilhete = state.bilhete,
                        aoCapturar = onCapturar,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )

                    if (state.arquivo != null) {
                        TextRegularBrownItalic(text = "Salvo na galeria")
                    }

                    CommonIconButton(
                        modifier = Modifier,
                        onClick = onCompartilhar,
                        text = "Compartilhar",
                        icon = { Icon(Icons.Filled.Share, contentDescription = null) },
                    )

                    // **Nova passagem na mesma saída** — o próximo da fila costuma ser para a mesma viagem.
                    // Só aparece quando se sabe de que travessia este bilhete é; abrir um bilhete antigo por
                    // outro caminho não oferece o gesto, porque ali não há atendimento em curso.
                    state.chaveDaOcorrencia?.let { chave ->
                        CommonIconButton(
                            modifier = Modifier,
                            onClick = { onNovaPassagem(chave) },
                            text = "Nova passagem",
                            color = MaterialTheme.colorScheme.secondary,
                            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        )
                    }
                }
            }
        }
    }
}