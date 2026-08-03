package dev.matheus.fluviapp.ui.states

data class SplashScreenUiState(
    val splashScreenState: SplashScreenState = SplashScreenState.Carregando
)

/**
 * O que a splash resolve antes de deixar o app entrar (ADR-0020 D9).
 *
 * [Carregando] deixou de ser um estado que nunca se observa: a splash passa a **carregar o contexto**
 * (usuário → funcionário → atuação), e isso vai à rede. [Erro] existe por consequência direta — sem ele,
 * uma falha de leitura prenderia a tela em [Carregando] para sempre, que é exatamente a "informação
 * omitida" que a decisão existe para impedir.
 */
sealed class SplashScreenState {
    data object Carregando : SplashScreenState()
    data object Logado : SplashScreenState()
    data object Deslogado : SplashScreenState()

    /** Falha ao resolver o contexto. A tela oferece tentar de novo; não se entra pela metade. */
    data object Erro : SplashScreenState()
}