package dev.matheus.fluviapp.ui.states

import dev.matheus.fluviapp.model.screendata.DadosViagemCard
import dev.matheus.fluviapp.model.screendata.SecaoMenu

data class MainScreenUiState(
    val userName: String = "",
    /** Seções liberadas ao cargo do usuário (política PermissoesUsuario). */
    val secoesVisiveis: List<SecaoMenu> = emptyList(),
    val listaViagens: List<DadosViagemCard> = emptyList(),
    val isRefreshing: Boolean = false,

    val mainScreenState: MainScreenState = MainScreenState.HOME,
)
