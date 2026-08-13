package dev.matheus.fluviapp.ui.states.passagem

import dev.matheus.fluviapp.domain.passagem.Passagem
import dev.matheus.fluviapp.domain.passagem.ResultadoEmbarque

/**
 * Estado da tela de validação de embarque (ADR-0012). Três fases derivadas dos campos:
 *  - **escaneando**: `passagem == null && resultado == null` → câmera ativa.
 *  - **resolvida**: `passagem != null` → mostra os dados e o botão de confirmar.
 *  - **resultado**: `resultado != null` → desfecho (confirmado / já usado / não emitido / inexistente).
 */
data class EmbarqueUiState(
    val processando: Boolean = false,
    val passagem: Passagem? = null,
    val resultado: ResultadoEmbarque? = null,
) {
    val escaneando: Boolean get() = passagem == null && resultado == null && !processando
}