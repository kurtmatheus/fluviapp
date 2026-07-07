package dev.matheus.fluviapp.model.screendata

data class DadosBotoesMenus(
    val title: Int,
    val icon: Int,
    val onClick: () -> Unit = {}
)
