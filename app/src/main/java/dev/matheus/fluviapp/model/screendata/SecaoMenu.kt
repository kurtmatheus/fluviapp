package dev.matheus.fluviapp.model.screendata

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dev.matheus.fluviapp.R

/**
 * Seções do menu lateral (drawer) da Main Screen. Cada seção abre, no conteúdo central da Main
 * Screen, seus cards de ação (cadastrar/pesquisar). Substitui o antigo par PASSAGENS/OPERAÇÕES.
 *
 * EMPRESA entra na fase 2 (quando o cadastro de empresa existir).
 */
enum class SecaoMenu(
    @StringRes val titulo: Int,
    @DrawableRes val icone: Int,
) {
    PASSAGEM(R.string.btn_menu_passagens, R.drawable.ic_bilhete),
    VIAGEM(R.string.label_menu_viagens, R.drawable.ic_navio_75),
    AGENTE(R.string.label_menu_agentes, R.drawable.ic_user_75),
}
