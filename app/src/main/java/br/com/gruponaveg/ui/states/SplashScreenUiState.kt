package br.com.gruponaveg.ui.states

data class SplashScreenUiState(
    val splashScreenState: SplashScreenState = SplashScreenState.Carregando
)

sealed class SplashScreenState {
    data object Carregando : SplashScreenState()
    data object Logado : SplashScreenState()
    data object Deslogado : SplashScreenState()
}