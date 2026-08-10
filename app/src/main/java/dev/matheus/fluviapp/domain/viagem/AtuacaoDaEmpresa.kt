package dev.matheus.fluviapp.domain.viagem

import dev.matheus.fluviapp.domain.operacoes.Atuacao
import dev.matheus.fluviapp.domain.rota.Rota

/**
 * **O que uma [Empresa] faz** — o par `(parte, atuação)` do eixo do [ADR-0016] §4, com as concessões que
 * o §7 pendura nele. No Firestore é `empresas/{empresaId}/atuacoes/{ATUACAO}`: um documento por atuação,
 * e **o id é o nome da atuação** — o que só é possível porque [Atuacao] é tipo fechado (ADR-0020 D5).
 *
 * Por que não é subtipo de empresa nem campo dela: **uma parte exerce várias atuações ao mesmo tempo e
 * muda de conjunto ao longo do tempo**. É exatamente nessas duas coisas que "tipo de empresa" e herança
 * fracassam — e é por isso que agência não é entidade: agência é a atuação `AGENCIAMENTO` de uma empresa,
 * `agenciaId` e `empresaId` são o mesmo id.
 *
 * **Concessão é allow-list, e é de segurança** (§7.1): o que não foi concedido não se vende. Distinta da
 * deny-list de rotas, que é conforto de tela.
 *
 * Ela tem **duas dimensões** (§7.1), e a segunda entrou na F7 junto com a Rota: **onde** ([portoIds]) e
 * **em quê** ([embarcacaoIds]). A linha que a empresa pode vender deixa de ser concedida diretamente e
 * passa a ser *consequência* — quem tem os portos de Manaus e de Parintins pode ofertar a travessia
 * entre eles; quem não tem, não pode. É o mecanismo que faz o pool ser compartilhado sem ser irrestrito.
 */
data class AtuacaoDaEmpresa(
    val atuacao: Atuacao,
    /** Embarcações que esta parte pode **vender** (§7, 7ª rodada: concede-se a embarcação, não o armador). */
    val embarcacaoIds: Set<String> = emptySet(),
    /** Portos em que esta parte pode **operar** — a outra metade da concessão (§7.1, F7). */
    val portoIds: Set<String> = emptySet(),
) {
    /**
     * A checagem que o §7 tornou **direta**: o id está na concessão, sem ler o dono da embarcação.
     * Enquanto a concessão era por armador, descobrir isto exigia um `get()` em
     * `embarcacoes/{id}.empresaId` — um salto a mais na UI e outro por escrita na regra do servidor.
     *
     * Id ausente é negado: **frota nova nasce não-concedida**, que é o preço aceito no ADR.
     */
    fun concedeu(embarcacaoId: String?): Boolean =
        !embarcacaoId.isNullOrBlank() && embarcacaoId in embarcacaoIds

    /** Mesma pergunta, do outro eixo: este porto está concedido? Ausente é negado (fail-closed). */
    fun operaNoPorto(portoId: String?): Boolean =
        !portoId.isNullOrBlank() && portoId in portoIds

    /**
     * **A rota é ofertável por esta parte?** — os dois portos concedidos.
     *
     * É aqui que a concessão deixa de recortar *o que ela cria* e passa a recortar *o que ela pode
     * vender* (§7.1): a rota é do pool e qualquer um a monta, mas ofertá-la exige ter os dois lados.
     */
    fun podeOfertar(portoOrigemId: String?, portoDestinoId: String?): Boolean =
        operaNoPorto(portoOrigemId) && operaNoPorto(portoDestinoId)

    /**
     * **A viagem é ofertável por esta parte?** — a pergunta completa, e a que a F7 deixou pela metade.
     *
     * Os dois eixos da concessão se encontram aqui, e é a viagem que os junta porque é ela que tem os
     * dois: **onde** vem dos portos da [rota], **em quê** é a embarcação da partida. Nem a rota sozinha
     * (não sabe o navio) nem a embarcação sozinha (não sabe o trajeto) respondem.
     *
     * Desde a decisão do analista de 2026-08-10, esta função responde também *o que a empresa **vê***, e
     * não só o que ela vende — ver e vender viraram a mesma pergunta (ver [EscopoDoPool]).
     */
    fun podeOfertar(viagem: Viagem, rota: Rota): Boolean =
        concedeu(viagem.embarcacaoId) && podeOfertar(rota.portoOrigemId, rota.portoDestinoId)
}

/** Esta parte exerce esta atuação? */
fun Collection<AtuacaoDaEmpresa>.exerce(atuacao: Atuacao): Boolean = any { it.atuacao == atuacao }

/** A atuação pedida, ou `null` se a parte não a exerce (fail-closed). */
fun Collection<AtuacaoDaEmpresa>.de(atuacao: Atuacao): AtuacaoDaEmpresa? =
    firstOrNull { it.atuacao == atuacao }

/**
 * As atuações que **produzem operação** hoje — as dormentes existem no modelo porque a concessão já as
 * referencia, mas não abrem seção nenhuma (ADR-0016 §5/§8).
 */
fun Collection<AtuacaoDaEmpresa>.operantes(): List<AtuacaoDaEmpresa> = filter { it.atuacao.operante }