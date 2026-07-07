package dev.matheus.fluviapp.ui.states

import dev.matheus.fluviapp.model.screendata.DadosBotoesMenus
import dev.matheus.fluviapp.model.screendata.MenuBotoesCategoria

sealed class MainScreenState {
    data object LOADING : MainScreenState()
    data object HOME : MainScreenState()
    data class PASSAGENS(val listaBotoesMenus: List<DadosBotoesMenus>) : MainScreenState()
    data class OPERACOES(val listaBotoesMenus: List<MenuBotoesCategoria>) : MainScreenState()
}
