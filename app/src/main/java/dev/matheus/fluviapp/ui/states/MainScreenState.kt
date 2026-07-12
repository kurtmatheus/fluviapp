package dev.matheus.fluviapp.ui.states

/**
 * Conteúdo central da Main Screen. As seções não trocam mais o conteúdo — elas expandem como
 * sub-menus dentro do drawer e navegam direto. Sobram o carregamento e o Início (viagens).
 */
sealed class MainScreenState {
    data object LOADING : MainScreenState()
    data object HOME : MainScreenState()
}
