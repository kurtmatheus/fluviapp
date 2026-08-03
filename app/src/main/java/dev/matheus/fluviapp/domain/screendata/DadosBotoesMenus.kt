package dev.matheus.fluviapp.domain.screendata

data class DadosBotoesMenus(
    val title: Int,
    val icon: Int,
    val onClick: () -> Unit = {}
)
