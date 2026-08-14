package dev.matheus.fluviapp.ui.screens.passagem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.passagem.CategoriaPassagem
import dev.matheus.fluviapp.domain.passagem.StatusPassagem
import dev.matheus.fluviapp.ui.components.forms.fields.FormFieldCalendario
import dev.matheus.fluviapp.ui.components.texts.TextRegularBrown
import dev.matheus.fluviapp.ui.components.texts.TextRegularBrownItalic
import dev.matheus.fluviapp.ui.components.texts.TextSubTitleBrownBold
import dev.matheus.fluviapp.ui.screens.forms.CommonScreenNoBottom
import dev.matheus.fluviapp.ui.states.passagem.PassagemNaLista
import dev.matheus.fluviapp.ui.states.passagem.PesquisaPassagemUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * **A busca de bilhetes** — a ação que dá sentido à seção Passagens no menu (F9.6).
 *
 * A tela abre **já com a resposta**: os bilhetes de hoje, sem ninguém apertar nada. Quem procura um bilhete
 * no balcão procura o de hoje quase sempre, e obrigar a preencher um formulário para chegar à pergunta mais
 * comum era o que o fluxo antigo fazia — uma tela inteira de filtros antes de qualquer resultado.
 *
 * Os filtros **estreitam** o que já está na tela, em vez de habilitar a busca: trocar o dia ou marcar um
 * status refaz a consulta na hora.
 */
@Composable
fun PesquisaPassagemScreen(
    state: PesquisaPassagemUiState,
    onEscolherData: (LocalDate) -> Unit = {},
    onAlternarStatus: (StatusPassagem) -> Unit = {},
    onAlternarCategoria: (CategoriaPassagem) -> Unit = {},
    onAbrirBilhete: (String) -> Unit = {},
    onClickVoltar: () -> Unit = {},
) {
    CommonScreenNoBottom(
        titleTopAppBar = R.string.title_top_passagem,
        titleTopContent = R.string.subtitle_pesquisar_passagem,
        isShowRightIcon = false,
        hasRefresh = false,
        isRefreshing = false,
        onClickVoltar = onClickVoltar,
    ) { modifier, _ ->
        Column(
            modifier = modifier.fillMaxSize().navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FormFieldCalendario(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                focusManager = LocalFocusManager.current,
                value = state.data.format(FORMATO_BR),
                onValueChange = { texto ->
                    runCatching { LocalDate.parse(texto, FORMATO_BR) }.getOrNull()?.let(onEscolherData)
                },
                label = R.string.label_data_viagem,
                isError = false,
                textoErro = R.string.error_camp_obrig,
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Só os dois status que a busca de balcão distingue: o que vale e o que já embarcou. A
                // cancelada aparece na lista, apagada — filtrar por ela seria uma pergunta que ninguém faz.
                listOf(StatusPassagem.EMITIDA, StatusPassagem.EMBARCADA).forEach { status ->
                    FilterChip(
                        selected = state.status == status,
                        onClick = { onAlternarStatus(status) },
                        label = { TextRegularBrownItalic(text = status.rotulo()) },
                    )
                }
                CategoriaPassagem.entries.forEach { categoria ->
                    FilterChip(
                        selected = state.categoria == categoria,
                        onClick = { onAlternarCategoria(categoria) },
                        label = { TextRegularBrownItalic(text = categoria.rotulo) },
                    )
                }
            }

            when {
                state.buscando -> Row(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }

                // Fail-closed com recado: quem não tem vínculo não vê bilhete nenhum, e precisa saber por quê.
                state.semEscopo -> TextRegularBrownItalic(
                    modifier = Modifier.padding(24.dp),
                    text = "Sem vínculo ativo não há passagens a mostrar.",
                )

                state.resultados.isEmpty() && state.buscou -> TextRegularBrownItalic(
                    modifier = Modifier.padding(24.dp),
                    text = "Nenhuma passagem nesta data.",
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.resultados, key = { it.idPassagem }) { passagem ->
                        PassagemDaLista(passagem = passagem, onClick = { onAbrirBilhete(passagem.idPassagem) })
                    }
                }
            }
        }
    }
}

/**
 * Uma linha da lista. Tocar **abre o bilhete** — que é o mesmo destino de quem acabou de emitir (ADR-0030 D5),
 * e o motivo de ele ter rota própria.
 */
@Composable
private fun PassagemDaLista(passagem: PassagemNaLista, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            // Encerrada fica **apagada**, e não escondida: ela continua sendo um fato, e é justamente o que
            // alguém procura ao perguntar "o que aconteceu com aquela passagem?".
            contentColor = if (passagem.encerrada) {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                TextSubTitleBrownBold(text = passagem.numero)
                TextRegularBrown(text = passagem.bilhete)
                TextRegularBrownItalic(text = passagem.partida)
            }
            TextRegularBrownItalic(text = passagem.status)
        }
    }
}

private val FORMATO_BR: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")