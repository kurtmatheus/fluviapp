package dev.matheus.fluviapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.screendata.AcaoMenu
import dev.matheus.fluviapp.domain.screendata.SECOES_REVITALIZADAS
import dev.matheus.fluviapp.domain.screendata.SecaoMenu
import dev.matheus.fluviapp.domain.screendata.acoesPorSecao
import dev.matheus.fluviapp.ui.components.contents.InicioContent
import dev.matheus.fluviapp.ui.components.drawer.FluviMenuDrawer
import dev.matheus.fluviapp.ui.states.MainScreenState
import dev.matheus.fluviapp.ui.states.MainScreenUiState

/**
 * **Revitalização (ADR-0020):** o painel exibe apenas o que já foi refeito ponta a ponta — hoje, a
 * Empresa. Some daqui tudo que pertence a domínio ainda não revitalizado: a lista de próximas viagens, o
 * atalho de nova passagem, o pull-to-refresh que atualizava essa lista e a barra inferior com o embarque
 * (que é leitura de QR de passagem). O menu já chega recortado pelo `secoesDoMenu`.
 *
 * A escolha é agir como app **recém-implementado**, e não como app completo com pedaços quebrados: quem
 * abre o painel vê um lugar vazio e um menu com uma seção, não botões que levam a telas sem dado.
 */
@Composable
fun MainScreen(
    state: MainScreenUiState,
    acoesPorSecao: Map<SecaoMenu, List<AcaoMenu>> = emptyMap(),
    onAcaoMenu: (AcaoMenu) -> Unit = {},
    onClickInicio: () -> Unit = {},
    onClickDeslogar: () -> Unit = {},
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    /** Tocar numa saída do Início abre a **emissão** naquela ocorrência (F9.5). */
    onClickViagemDisponivel: (String) -> Unit = {},
    // REVITALIZAÇÃO: voltam com as seções Passagem / Viagem.
    // onClickEmbarque: () -> Unit = {},
    // onRefresh: () -> Unit = {},
) {
    val estado = state.mainScreenState

    CommonScreen(
        modifier = Modifier,
        titleTopContent = 0,
        isMainTopAppBar = true,
        titleTopAppBar = 0,
        userNameTopAppBar = state.userName,
        isShowBottomAppBar = false,
        isShowRightIcon = false,
        hasRefresh = false,
        isRefreshing = state.isRefreshing,
        inicioAtivo = true,
        onClickInicio = onClickInicio,
        drawerContent = { fechar ->
            FluviMenuDrawer(
                userName = state.userName,
                secoes = state.secoesVisiveis,
                acoesPorSecao = acoesPorSecao,
                isDarkTheme = isDarkTheme,
                onInicio = { onClickInicio(); fechar() },
                onNavegar = { acao -> onAcaoMenu(acao); fechar() },
                onToggleTheme = onToggleTheme,
                onDeslogar = onClickDeslogar,
            )
        },
        content = { modifier, _ ->
            Column(modifier = Modifier.fillMaxSize()) {
                // Banner offline-first (D4): não-bloqueante, sobre os dados do cache. Some quando um
                // snapshot do servidor chega (EstadoSincronizacao.reportarSucesso via RegistroSincronizacao).
                if (state.sincronizacaoComErro) BannerSincronizacaoOffline()

                when (estado) {
                    is MainScreenState.LOADING -> Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    // O Início voltou na F8.4, e **quem decide o que ele mostra é o domínio**: a tela
                    // recebe um `InicioDaTela` já resolvido e desenha a face dele. A divisão entre
                    // plataforma e empresa não mora mais aqui.
                    // A saída do Início é a **porta da emissão** (F9.5): tocar num card leva a vender
                    // naquela ocorrência, e é daí que a data e a hora do bilhete vêm — nunca de um campo.
                    is MainScreenState.HOME -> InicioContent(
                        modifier = modifier,
                        inicio = state.inicio,
                        onClickViagem = onClickViagemDisponivel,
                    )
                }
            }
        },
    )
}

// O `PainelVazio` saiu na F8.4: cada face do Início agora tem o próprio recado dentro do
// `InicioContent`, e um "painel vazio" genérico apagaria justamente a distinção que o domínio passou a
// fazer — a plataforma sem lista, a empresa sem saída e a empresa sem concessão são três coisas.

@Composable
private fun BannerSincronizacaoOffline() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.msg_sincronizacao_offline),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Preview(name = "Home", showBackground = true)
@Composable
private fun MainScreenHomePreview() {
    MainScreen(
        MainScreenUiState(
            userName = "Odair",
            secoesVisiveis = SECOES_REVITALIZADAS.toList(),
            mainScreenState = MainScreenState.HOME,
        ),
        acoesPorSecao = acoesPorSecao(SECOES_REVITALIZADAS.toList()),
    )
}

@Preview(name = "Home offline (banner)", showBackground = true)
@Composable
private fun MainScreenOfflinePreview() {
    MainScreen(
        MainScreenUiState(
            userName = "Odair",
            secoesVisiveis = SECOES_REVITALIZADAS.toList(),
            sincronizacaoComErro = true,
            mainScreenState = MainScreenState.HOME,
        ),
        acoesPorSecao = acoesPorSecao(SECOES_REVITALIZADAS.toList()),
    )
}
