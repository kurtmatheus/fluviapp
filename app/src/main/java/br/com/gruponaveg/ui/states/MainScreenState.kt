package br.com.gruponaveg.ui.states

import br.com.gruponaveg.model.screendata.DadosBotoesMenus
import br.com.gruponaveg.model.screendata.MenuBotoesCategoria

sealed class MainScreenState {
    data object LOADING : MainScreenState()
    data object HOME : MainScreenState()
    data class PASSAGENS(val listaBotoesMenus: List<DadosBotoesMenus>) : MainScreenState()
    data class OPERACOES(val listaBotoesMenus: List<MenuBotoesCategoria>) : MainScreenState()
}
