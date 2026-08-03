package dev.matheus.fluviapp.ui.states

import dev.matheus.fluviapp.domain.screendata.DadosBotoesMenus

data class MenuState(
    val userName: String = "",
    val listaBotoesMenus: List<DadosBotoesMenus> = emptyList(),
    val active: Boolean = false,
    val exibirUserDialog: Boolean = false
)
