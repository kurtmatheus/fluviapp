package dev.matheus.fluviapp.domain.operacoes

/**
 * **Quantos, e em que estado** — as métricas de acesso da plataforma ([ADR-0032] D6, decisão do analista em
 * 2026-09-08: *"as métricas de acesso no Início do painel da plataforma"*).
 *
 * ### Cinco números, e cada um com um gesto atrás
 *
 * Não é um painel de indicadores: é a lista do que está pendente de alguém fazer algo. [desativados] e
 * [expirados] têm correções diferentes (reativar × prazo novo), [convitesPendentes] é quem foi chamado e
 * não veio, e [aVencer] é o único **preventivo** — o número que evita a pessoa descobrir o vencimento
 * tentando trabalhar.
 *
 * ### Por que contagem, e não evento
 *
 * A D4 decidiu que o evento é **agregado**, nunca identidade: *o evento conta, o documento prova*. A issue
 * #19 supunha que a tela leria aquele evento, e ela não pode — `analytics.logEvent` é só escrita, e os
 * eventos vivem no console.
 *
 * A régua sobreviveu à troca de fonte, e ficou mais forte: **contar não devolve documento**. Estes cinco
 * números não carregam nome, e-mail nem uid, e não é por disciplina de quem escreveu — é por construção do
 * tipo. Um `Int` não tem como vazar PII.
 *
 * O que **não** está aqui, e por decisão: nada que responda *"o que a Ana fez às 14h"*. Isso é do carimbo
 * dentro do documento, onde a auditoria mora.
 */
data class MetricasDeAcesso(
    val ativos: Int = 0,
    val desativados: Int = 0,
    val expirados: Int = 0,
    /**
     * Convidado que **nunca entrou** — medido pela ausência de `users/{uid}`, e não pelo `usado` do
     * convite.
     *
     * A diferença é de autoridade: `usado` é escrito pelo primeiro acesso, e um convite marcado sem que o
     * perfil exista (escrita que falhou no meio) contaria como pessoa dentro. O perfil é o fato; o convite
     * é a intenção.
     */
    val convitesPendentes: Int = 0,
    /** Acessos ativos cujo prazo vence dentro da janela de aviso — o único número preventivo. */
    val aVencer: Int = 0,
) {
    /** Quem tem perfil, em qualquer estado. Derivado: guardá-lo seria manter duas verdades. */
    val comAcesso: Int get() = ativos + desativados + expirados

    /** Se há algo a fazer. É o que decide se o card fala ou fica quieto. */
    val temPendencia: Boolean get() = desativados > 0 || expirados > 0 || convitesPendentes > 0 || aVencer > 0
}

/** A janela do aviso: uma semana. Prazo que vence hoje já está dentro dela desde ontem. */
const val DIAS_DE_AVISO_DE_PRAZO: Long = 7

/**
 * As métricas, **puras** — entram as duas coleções que a plataforma já lê, sai a contagem.
 *
 * A situação de cada acesso vem de [situacaoDoAcesso], a mesma função que a lista da seção Usuários usa e
 * que a regra do servidor espelha. Contar aqui com um critério próprio faria o card e a lista discordarem
 * sobre a mesma pessoa — e o card é justamente onde a discordância não seria notada.
 *
 * [aVencer] conta **só entre os ativos**, e não entre todos: um desativado com prazo a vencer não precisa
 * de renovação, precisa de decisão. Somá-lo aqui inflaria o número preventivo com casos que já são
 * pendência de outro tipo.
 *
 * A janela é fechada dos dois lados — de agora até `agora + dias` —, então **expirado não conta como a
 * vencer**: são estados sucessivos, e o card mostra os dois lado a lado.
 */
fun metricasDeAcesso(
    usuarios: List<Usuario>,
    convites: List<Convite>,
    agora: Long,
    dias: Long = DIAS_DE_AVISO_DE_PRAZO,
): MetricasDeAcesso {
    val porSituacao = usuarios.groupingBy { situacaoDoAcesso(it.ativo, it.expiraEm, agora) }.eachCount()
    val limiteDoAviso = agora + dias * MILLIS_POR_DIA

    val emailsComPerfil = usuarios.map { it.email.lowercase() }.toSet()

    return MetricasDeAcesso(
        ativos = porSituacao[SituacaoAcesso.ATIVO] ?: 0,
        desativados = porSituacao[SituacaoAcesso.DESATIVADO] ?: 0,
        expirados = porSituacao[SituacaoAcesso.EXPIRADO] ?: 0,
        convitesPendentes = convites.count { it.email.lowercase() !in emailsComPerfil },
        aVencer = usuarios.count { usuario ->
            val prazo = usuario.expiraEm
            situacaoDoAcesso(usuario.ativo, prazo, agora) == SituacaoAcesso.ATIVO &&
                prazo != null && prazo <= limiteDoAviso
        },
    )
}

private const val MILLIS_POR_DIA: Long = 24 * 60 * 60 * 1000
