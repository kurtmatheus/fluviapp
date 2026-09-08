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
 * **Revitalização (ADR-0020):** o painel exibe apenas o que já foi refeito ponta a ponta. Sai daqui tudo
 * que pertence a domínio ainda não revitalizado — o atalho de nova passagem e o pull-to-refresh —, e o
 * menu já chega recortado pelo `secoesDoMenu`.
 *
 * A escolha é agir como app **recém-implementado**, e não como app completo com pedaços quebrados: quem
 * abre o painel vê um menu com o que existe, não botões que levam a telas sem dado.
 *
 * **A barra inferior com o embarque voltou**, e voltou por medida e não por prazo: a F9 refez a tela, o
 * ViewModel, a escrita e a regra de servidor da aresta `EMITIDA→EMBARCADA` — o que faltava era só a porta.
 * Ela aparece onde o painel vende (`state.podeEmbarcar`), pelo mesmo critério que dá a seção Passagem a
 * quem agencia: conferir bilhete é do painel que emite bilhete.
 */
@Composable
fun MainScreen(
    state: MainScreenUiState,
    acoesPorSecao: Map<SecaoMenu, List<AcaoMenu>> = emptyMap(),
    onAcaoMenu: (AcaoMenu) -> Unit = {},
    /**
     * O "Início" do **menu lateral** — o único que restou depois de a barra inferior perder o dela.
     *
     * O da barra era navegação para a tela onde a barra está; este vem de dentro do drawer, onde há
     * outras seções abertas, e leva de volta ao painel. Mesma palavra, gestos diferentes.
     */
    onClickInicio: () -> Unit = {},
    onClickDeslogar: () -> Unit = {},
    /** A troca de perfil ([ADR-0032] D5) — opção do menu de quem tem os dois. */
    onClickTrocarPerfil: () -> Unit = {},
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    /** Tocar numa saída do Início abre a **emissão** naquela ocorrência (F9.5). */
    onClickViagemDisponivel: (String) -> Unit = {},
    /** O FAB central da barra inferior: ler o QR de um bilhete e conferir o embarque (ADR-0012). */
    onClickEmbarque: () -> Unit = {},
    // REVITALIZAÇÃO: o pull-to-refresh volta com a F10, se voltar — com o Início assinando a fonte
    // (2026-08-17), o gesto perdeu a função técnica e o que sobra é a expectativa de quem opera.
    // onRefresh: () -> Unit = {},
) {
    val estado = state.mainScreenState

    CommonScreen(
        modifier = Modifier,
        titleTopContent = 0,
        isMainTopAppBar = true,
        titleTopAppBar = 0,
        userNameTopAppBar = state.userName,
        isShowBottomAppBar = state.podeEmbarcar,
        isShowRightIcon = false,
        hasRefresh = false,
        isRefreshing = state.isRefreshing,
        onClickEmbarque = onClickEmbarque,
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
                podeTrocarPerfil = state.podeTrocarPerfil,
                perfilAtivo = state.perfilAtivo,
                empresaDoVinculo = state.empresaDoVinculo,
                // Fecha o menu junto: o painel de trás vai trocar inteiro, e vê-lo trocar por baixo de um
                // menu aberto seria mostrar o menu antigo sobre o painel novo.
                onTrocarPerfil = { onClickTrocarPerfil(); fechar() },
            )
        },
        content = { modifier, _ ->
            Column(modifier = Modifier.fillMaxSize()) {
                // Banner offline-first (D4): não-bloqueante, sobre os dados do cache. Some quando um
                // snapshot do servidor chega (EstadoSincronizacao.reportarSucesso via RegistroSincronizacao).
                if (state.sincronizacaoComErro) BannerSincronizacaoOffline()

                when (estado) {
                    // **A troca de perfil se anuncia como carregamento**, e nada além disso (ADR-0032 Q3):
                    // indicador circular padrão e uma linha de texto. Sem tela nova, sem cerimônia —
                    // celebrar a troca sugeriria uma separação que o servidor não faz.
                    //
                    // Este era um estado **sem produtor**: a tela o desenhava e nada o ligava. A troca é o
                    // gesto que faltava.
                    is MainScreenState.LOADING -> Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.padding(top = 12.dp))
                            Text(
                                text = stringResource(R.string.msg_carregando_perfil),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                            )
                        }
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

/** O painel de quem vende: a barra inferior existe, com o FAB de embarque protruso sobre ela. */
@Preview(name = "Home da empresa (com embarque)", showBackground = true)
@Composable
private fun MainScreenHomePreview() {
    MainScreen(
        MainScreenUiState(
            userName = "Odair",
            secoesVisiveis = SECOES_REVITALIZADAS.toList(),
            podeEmbarcar = true,
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

/** A troca de perfil (ADR-0032 Q3): indicador circular padrão e uma linha de texto. */
@Preview(name = "Trocando de perfil", showBackground = true)
@Composable
private fun MainScreenTrocandoPerfilPreview() {
    MainScreen(
        MainScreenUiState(
            userName = "Odair",
            secoesVisiveis = SECOES_REVITALIZADAS.toList(),
            podeTrocarPerfil = true,
            empresaDoVinculo = "Navegação Norte",
            mainScreenState = MainScreenState.LOADING,
        ),
    )
}
