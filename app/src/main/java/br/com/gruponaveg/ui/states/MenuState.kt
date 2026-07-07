package br.com.gruponaveg.ui.states

import br.com.gruponaveg.model.screendata.DadosBotoesMenus

data class MenuState(
    val userName: String = "",
    val listaBotoesMenus: List<DadosBotoesMenus> = emptyList(),
    val active: Boolean = false,
    val exibirUserDialog: Boolean = false
)
