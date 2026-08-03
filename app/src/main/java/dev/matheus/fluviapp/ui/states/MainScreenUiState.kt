package dev.matheus.fluviapp.ui.states

import dev.matheus.fluviapp.domain.screendata.DadosViagemCard
import dev.matheus.fluviapp.domain.screendata.SecaoMenu

data class MainScreenUiState(
    val userName: String = "",
    /** Seções liberadas ao cargo do usuário (política PermissoesUsuario). */
    val secoesVisiveis: List<SecaoMenu> = emptyList(),
    val listaViagens: List<DadosViagemCard> = emptyList(),
    val isRefreshing: Boolean = false,

    /** Sync falhou (offline): banner não-bloqueante sobre os dados do cache (D4). */
    val sincronizacaoComErro: Boolean = false,

    val mainScreenState: MainScreenState = MainScreenState.HOME,
)
