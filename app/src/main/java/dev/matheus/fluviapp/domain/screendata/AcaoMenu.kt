package dev.matheus.fluviapp.domain.screendata

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dev.matheus.fluviapp.R

/**
 * As ações que cada [SecaoMenu] oferece — **a estrutura do menu, como domínio** (ADR-0020 F3).
 *
 * Ela morava dentro do grafo de navegação, na função `acoesDe(secao)` do `MainScreenNavComposable`, que
 * misturava duas perguntas de naturezas diferentes: **quais ações uma seção tem** (domínio: é o que o
 * negócio oferece naquele contexto) e **para onde cada uma navega** (navegação: é rota). Misturadas, a
 * primeira só podia ser respondida com um `NavGraphBuilder` por perto — ou seja, não podia ser testada.
 *
 * Aqui fica a primeira. A segunda continua no grafo, que é o lugar dela, e passa a receber um
 * [AcaoMenu] em vez de treze callbacks.
 *
 * Cada valor declara a sua seção, e a seção deriva as suas ações — assim acrescentar uma ação é uma
 * linha, e o compilador cobra o destino no `when` do grafo.
 */
enum class AcaoMenu(
    val secao: SecaoMenu,
    @StringRes val titulo: Int,
    @DrawableRes val icone: Int,
) {
    // A busca é a **única** ação da seção, e a emissão não está aqui de propósito: ela começa pela **saída**,
    // no Início, porque um bilhete sem travessia não existe (ADR-0028 D5). Menu que oferecesse "nova
    // passagem" teria de perguntar a viagem — que é a pergunta que o card de saída já respondeu.
    //
    // A **contagem saiu na F9.6**: ela é ocupação, e ocupação não tem domínio planejado (ADR-0027 D2). Ela
    // estava aqui apontando para uma tela que a F9.2 apagou, e uma ação de menu que não leva a lugar nenhum
    // é pior do que ação nenhuma. Volta com o domínio dela.
    PASSAGEM_PESQUISAR(SecaoMenu.PASSAGEM, R.string.btn_pesquisar_passagens, R.drawable.ic_lupa_75),

    // A Viagem volta com as ações da partida física (F8.2). São as mesmas duas de qualquer cadastro do
    // molde — e **não há uma terceira**: não existe "editar viagem" para ninguém (ADR-0016 §7.1).
    VIAGEM_NOVA(SecaoMenu.VIAGEM, R.string.btn_nova_viagem, R.drawable.ic_add_75),
    VIAGEM_PESQUISAR(SecaoMenu.VIAGEM, R.string.btn_pesquisar_viagens, R.drawable.ic_lupa_75),

    EQUIPE_NOVO(SecaoMenu.EQUIPE, R.string.btn_novo_agente, R.drawable.ic_add_75),
    EQUIPE_PESQUISAR(SecaoMenu.EQUIPE, R.string.btn_pesquisar_agente, R.drawable.ic_lupa_75),

    EMPRESA_NOVA(SecaoMenu.EMPRESA, R.string.btn_nova_empresa, R.drawable.ic_add_75),
    EMPRESA_PESQUISAR(SecaoMenu.EMPRESA, R.string.btn_pesquisar_empresa, R.drawable.ic_lupa_75),

    EMBARCACAO_NOVA(SecaoMenu.EMBARCACAO, R.string.btn_nova_embarcacao, R.drawable.ic_add_75),
    EMBARCACAO_PESQUISAR(SecaoMenu.EMBARCACAO, R.string.btn_pesquisar_embarcacao, R.drawable.ic_lupa_75),

    LOCALIDADE_NOVA(SecaoMenu.LOCALIDADE, R.string.btn_nova_localidade, R.drawable.ic_add_75),
    LOCALIDADE_PESQUISAR(SecaoMenu.LOCALIDADE, R.string.btn_pesquisar_localidade, R.drawable.ic_lupa_75),

    PORTO_NOVO(SecaoMenu.PORTO, R.string.btn_novo_porto, R.drawable.ic_add_75),
    PORTO_PESQUISAR(SecaoMenu.PORTO, R.string.btn_pesquisar_porto, R.drawable.ic_lupa_75),

    USUARIO_NOVO(SecaoMenu.USUARIOS, R.string.btn_novo_usuario, R.drawable.ic_add_75),
    USUARIO_PESQUISAR(SecaoMenu.USUARIOS, R.string.btn_pesquisar_usuario, R.drawable.ic_lupa_75),

    ROTA_NOVA(SecaoMenu.ROTA, R.string.btn_nova_rota, R.drawable.ic_add_75),
    ROTA_PESQUISAR(SecaoMenu.ROTA, R.string.btn_pesquisar_rota, R.drawable.ic_lupa_75);

    companion object {
        /** As ações desta seção, na ordem declarada. */
        fun de(secao: SecaoMenu): List<AcaoMenu> = entries.filter { it.secao == secao }
    }
}

/** O menu montado: cada seção visível com as ações que oferece. */
fun acoesPorSecao(secoes: List<SecaoMenu>): Map<SecaoMenu, List<AcaoMenu>> =
    secoes.associateWith { AcaoMenu.de(it) }