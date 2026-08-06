package dev.matheus.fluviapp.domain.screendata

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
    // EMPRESA vem primeiro (ADR-0020 D10): é a **parte**, e dela dependem todas as outras — embarcacao tem
    // dono, funcionário tem vínculo, rota tem agência. A ordem do enum é a ordem do menu, então ela
    // também é a ordem de dependência do cadastro.
    EMPRESA(R.string.label_menu_empresa, R.drawable.ic_empresa_24),
    EMBARCACAO(R.string.label_menu_flotilha, R.drawable.ic_embarcacao_75),
    // Capacidade da plataforma (ADR-0016 §5): não pertence a empresa nenhuma. Vem depois dos ativos e
    // antes da operação porque é o que o porto — e, por ele, a rota — vai referenciar.
    LOCALIDADE(R.string.label_menu_localidades, R.drawable.ic_localidade_24),
    // O PORTO vem logo depois da LOCALIDADE porque depende dela: não se cadastra um cais sem a cidade
    // em que ele fica. A ordem do enum continua sendo a ordem em que se consegue cadastrar.
    PORTO(R.string.label_menu_portos, R.drawable.ic_porto_24),
    PASSAGEM(R.string.btn_menu_passagens, R.drawable.ic_bilhete),
    VIAGEM(R.string.label_menu_viagens, R.drawable.ic_embarcacao_75),
    // "Equipe" (ADR-0015 §8.1): o coletivo dos Funcionario — as pessoas na operação, com cargo, agência
    // e lotação. O nome AGENTE saiu daqui para não colidir com o cargo Funcionario.Cargo.AGENTE.
    EQUIPE(R.string.label_menu_equipe, R.drawable.ic_user_75),
}
