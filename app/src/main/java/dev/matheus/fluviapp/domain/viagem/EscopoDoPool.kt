package dev.matheus.fluviapp.domain.viagem

import dev.matheus.fluviapp.domain.operacoes.PermissoesUsuario
import dev.matheus.fluviapp.domain.rota.Rota

/**
 * **O que cada lado enxerga do pool compartilhado** (decisão do analista, 2026-08-10).
 *
 * O ADR-0016 §7.1 dizia o contrário — *"visualização = pool − negadas; venda = concessão"* —, com o medo
 * de que filtrar a visualização pela concessão fizesse a agência nova abrir uma tela vazia. A decisão do
 * analista inverte isso, por um argumento de propósito: **o painel da empresa serve para gerir o que é
 * daquela empresa**, e não faz sentido ela ver embarcação, rota ou viagem fora da própria atuação.
 *
 * ### O que isso simplifica, e o que custa
 *
 * **Ver e vender passam a ser a mesma pergunta.** Eram duas — uma *deny-list* de conforto na tela e uma
 * *allow-list* de segurança no servidor —, com o risco permanente de discordarem: a agência via na lista
 * uma viagem que a emissão depois recusava. Com um critério só, a lista já é o que se pode vender.
 *
 * Com isso **a lista de negadas deixa de ter trabalho** e não é construída: `rotasNegadas` e
 * `viagensNegadas` do §7.1 saem do plano. Quem escolhia o que não ver agora não vê o que não recebeu.
 *
 * O preço é real e é o que o §7.1 temia: **agência sem concessão vê nada**. Provisionar deixa de ser
 * conveniência e vira **pré-requisito** — a empresa só opera depois de a plataforma lhe dar portos e
 * embarcações. É o mesmo fail-closed do resto do ADR, aplicado à leitura.
 *
 * ### Por que não é `EscopoEmpresa`
 *
 * [PermissoesUsuario.EscopoEmpresa] recorta por **dono** (`empresaId` no documento), e o pool não tem
 * dono — é justamente essa ausência que faz duas agências usarem a mesma viagem. O recorte aqui é por
 * **concessão**: não "de quem é isto", mas "isto me foi dado".
 */
sealed interface EscopoDoPool {
    /** Papel de plataforma: vê o pool inteiro. É ela quem o cura — e quem não vê, não conserta. */
    data object Todo : EscopoDoPool

    /** Empresa: vê o que a atuação dela concede. */
    data class Concedido(val atuacao: AtuacaoDaEmpresa) : EscopoDoPool

    /** Sem papel de plataforma e sem atuação: não há o que mostrar (fail-closed). */
    data object Nenhum : EscopoDoPool
}

/**
 * O escopo de quem está olhando. Atuação ausente **não** vira pool inteiro: é o mesmo cuidado do
 * `EscopoEmpresa` — "não filtra nada" e "não tem nada" pareceriam iguais e abririam a listagem para quem
 * não deveria ver.
 */
fun escopoDoPool(papel: String?, atuacao: AtuacaoDaEmpresa?): EscopoDoPool = when {
    PermissoesUsuario.ehPapelPlataforma(papel) -> EscopoDoPool.Todo
    atuacao != null -> EscopoDoPool.Concedido(atuacao)
    else -> EscopoDoPool.Nenhum
}

/**
 * Este porto está ao alcance de quem olha? É o que recorta o **seletor** do cadastro de rota (F8.3) —
 * criar virou subconjunto de ver, e um seletor que oferecesse porto não concedido produziria uma rota
 * que some da lista de quem a criou.
 */
fun EscopoDoPool.operaNo(portoId: String): Boolean = when (this) {
    EscopoDoPool.Todo -> true
    EscopoDoPool.Nenhum -> false
    is EscopoDoPool.Concedido -> atuacao.operaNoPorto(portoId)
}

/** Idem para a embarcação — o outro eixo da concessão, que recorta o seletor do cadastro de viagem. */
fun EscopoDoPool.concedeu(embarcacaoId: String): Boolean = when (this) {
    EscopoDoPool.Todo -> true
    EscopoDoPool.Nenhum -> false
    is EscopoDoPool.Concedido -> atuacao.concedeu(embarcacaoId)
}

/**
 * As rotas que este escopo enxerga — ligações entre **dois portos concedidos**.
 *
 * *A F7 deixava a rota criável em qualquer par — "criar é povoar o mundo, ver é operar nele". A F8.3
 * desfez essa assimetria: com a lista recortada, criar fora do alcance produziria uma ligação que some no
 * instante seguinte. Ver e criar passaram a ter o mesmo limite (ver [operaNo]).*
 */
fun List<Rota>.noEscopo(escopo: EscopoDoPool): List<Rota> = when (escopo) {
    EscopoDoPool.Todo -> this
    EscopoDoPool.Nenhum -> emptyList()
    is EscopoDoPool.Concedido -> filter {
        escopo.atuacao.podeOfertar(it.portoOrigemId, it.portoDestinoId)
    }
}

/**
 * As viagens que este escopo enxerga — **embarcação concedida e rota concedida**, as duas.
 *
 * A rota chega por [rotasPorId] porque os portos moram nela: a viagem sabe em quê e quando, não onde.
 * Viagem cuja rota **não está no mapa** é descartada — é órfã (rota apagada ou ainda não carregada), e
 * mostrá-la seria oferecer uma travessia sem origem nem destino. Fail-closed, como o resto.
 */
fun List<Viagem>.noEscopo(escopo: EscopoDoPool, rotasPorId: Map<String, Rota>): List<Viagem> =
    when (escopo) {
        EscopoDoPool.Todo -> this
        EscopoDoPool.Nenhum -> emptyList()
        is EscopoDoPool.Concedido -> filter { viagem ->
            val rota = rotasPorId[viagem.rotaId] ?: return@filter false
            escopo.atuacao.podeOfertar(viagem, rota)
        }
    }