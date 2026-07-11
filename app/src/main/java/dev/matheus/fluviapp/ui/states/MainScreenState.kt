package dev.matheus.fluviapp.ui.states

import dev.matheus.fluviapp.model.screendata.DadosBotoesMenus
import dev.matheus.fluviapp.model.screendata.SecaoMenu

/**
 * Conteúdo central da Main Screen. HOME = viagens disponíveis; SECAO = os cards de ação da seção
 * escolhida no drawer (Passagem/Viagem/Agente/…). Unifica os antigos PASSAGENS/OPERACOES.
 */
sealed class MainScreenState {
    data object LOADING : MainScreenState()
    data object HOME : MainScreenState()
    data class SECAO(
        val secao: SecaoMenu,
        val acoes: List<DadosBotoesMenus>,
    ) : MainScreenState()
}
