package br.com.gruponaveg.ui.states

import br.com.gruponaveg.R
import br.com.gruponaveg.model.screendata.DadosBotoesMenus
import br.com.gruponaveg.model.screendata.DadosViagemCard

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
