package dev.matheus.fluviapp.ui.states

import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.model.screendata.DadosBotoesMenus
import dev.matheus.fluviapp.model.screendata.DadosViagemCard

data class MainScreenUiState(
    val userName: String = "",
    val isDiretorOuAdm: Boolean = false,
    val listaViagens: List<DadosViagemCard> = emptyList(),
    val listaBotoesMenu: List<DadosBotoesMenus> = emptyList(),
    val title: Int = R.string.subtitle_viagens_disponiveis,
    val homeActive: Boolean = true,
    val passagensActive: Boolean = false,
    val operacoesActive: Boolean = false,
    val exibirUserDialog: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSync: Boolean = false,

    val mainScreenState: MainScreenState = MainScreenState.HOME
)
