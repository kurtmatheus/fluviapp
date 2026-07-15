package dev.matheus.fluviapp.model.screendata

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dev.matheus.fluviapp.R

/**
 * Seções do menu lateral (drawer) da Main Screen. Cada seção abre, no conteúdo central da Main
 * Screen, seus cards de ação (cadastrar/pesquisar). Substitui o antigo par PASSAGENS/OPERAÇÕES.
 *
 */
enum class SecaoMenu(
    @StringRes val titulo: Int,
    @DrawableRes val icone: Int,
) {
    PASSAGEM(R.string.btn_menu_passagens, R.drawable.ic_bilhete),
    VIAGEM(R.string.label_menu_viagens, R.drawable.ic_navio_75),
    AGENTE(R.string.label_menu_agentes, R.drawable.ic_user_75),
    EMPRESA(R.string.label_menu_empresa, R.drawable.ic_empresa_24),
    NAVIO(R.string.label_menu_navios, R.drawable.ic_navio_75),
}
