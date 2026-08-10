package dev.matheus.fluviapp.ui.states

import dev.matheus.fluviapp.domain.screendata.SecaoMenu

data class MainScreenUiState(
    val userName: String = "",
    /** Seções liberadas ao cargo do usuário (política PermissoesUsuario). */
    val secoesVisiveis: List<SecaoMenu> = emptyList(),
    // REVITALIZAÇÃO (F8.0): `listaViagens` saiu. O `MainScreenViewModel` já não a preenchia — quem a
    // consumia era o `HomeContent`, comentado fora da tela desde que o painel virou `PainelVazio`. O que
    // o Início mostra é assunto da **F10**, e ele nasce do contexto escolhido, não de uma lista de cards.
    val isRefreshing: Boolean = false,

    /** Sync falhou (offline): banner não-bloqueante sobre os dados do cache (D4). */
    val sincronizacaoComErro: Boolean = false,

    val mainScreenState: MainScreenState = MainScreenState.HOME,
)
