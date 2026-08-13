package dev.matheus.fluviapp.ui.states.passagem

import dev.matheus.fluviapp.domain.passagem.Passagem
import dev.matheus.fluviapp.domain.passagem.ResultadoEmbarque

/**
 * Estado da tela de validação de embarque (ADR-0012). Três fases derivadas dos campos:
 *  - **escaneando**: `conferencia == null && resultado == null` → câmera ativa.
 *  - **resolvida**: `conferencia != null` → mostra os dados e o botão de confirmar.
 *  - **resultado**: `resultado != null` → desfecho (confirmado / já usado / não emitido / inexistente).
 *
 * ### Dois campos para a mesma passagem, e não é redundância
 *
 * A [conferencia] é o que a **tela** lê: a projeção pronta, com identificação, travessia e partida já
 * resolvidas ([ADR-0025] D4). A [passagem] é o que a **ação** usa: o agregado, de onde sai o id que vai para
 * o servidor. Fundi-los faria a tela receber o agregado inteiro — e voltaria a exigir dela justamente o que a
 * junção acabou de tirar: saber resolver id em nome.
 */
data class EmbarqueUiState(
    val processando: Boolean = false,
    /** O agregado resolvido ao vivo — só a ação o usa. */
    val passagem: Passagem? = null,
    /** A projeção que a tela exibe. */
    val conferencia: ConferenciaDeEmbarque? = null,
    val resultado: ResultadoEmbarque? = null,
) {
    val escaneando: Boolean get() = conferencia == null && resultado == null && !processando
}