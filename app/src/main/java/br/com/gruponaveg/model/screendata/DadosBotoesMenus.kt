package br.com.gruponaveg.model.screendata

data class DadosBotoesMenus(
    val title: Int,
    val icon: Int,
    val onClick: () -> Unit = {}
)
