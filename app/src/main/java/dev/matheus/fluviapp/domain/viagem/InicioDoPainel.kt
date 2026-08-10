package dev.matheus.fluviapp.domain.viagem

import dev.matheus.fluviapp.domain.rota.Rota
import java.time.LocalDateTime

/**
 * **O que a tela inicial mostra, e para quem** (decisão do analista, 2026-08-10).
 *
 * Antes da revitalização a home listava viagens para todo mundo. A lista volta — mas agora **o domínio é
 * quem estabelece a divisão** entre plataforma e empresa, em vez de a tela decidir por conta própria:
 *
 * - quem **administra a plataforma** monta o universo (partes, ativos, portos, o pool). Ela não vende, e
 *   uma lista de saídas disponíveis não responde nenhuma pergunta dela. O sumário do painel dela é a
 *   **F10**, e até lá o Início é o painel vazio que já existe;
 * - quem **opera numa empresa** vende travessia. A pergunta dela ao abrir o app é literalmente *"o que
 *   está saindo?"* — e a resposta é [ViagemSemana], recortada pela concessão como todo o resto.
 *
 * A divisão não é um `if` novo: é o [EscopoDoPool], o mesmo tipo que recorta busca e cadastro. Isso é o
 * que impede o Início de discordar das outras telas sobre o que a empresa alcança.
 */
sealed interface InicioDoPainel {

    /** Painel da plataforma: o sumário dela é a F10, e sumário vem depois do que resume (ADR-0022 D5). */
    data object DaPlataforma : InicioDoPainel

    /** Painel da empresa: as saídas que ela pode ofertar nos próximos dias. */
    data class DaEmpresa(val disponiveis: List<ViagemSemana>) : InicioDoPainel

    /**
     * Sem concessão — e é **estado próprio**, não uma [DaEmpresa] vazia. A diferença importa na tela: uma
     * lista vazia diz "não há saída esta semana" e manda esperar; esta diz "falta provisionar" e manda
     * procurar a plataforma. São recados opostos para quem está tentando trabalhar.
     */
    data object SemConcessao : InicioDoPainel
}

/**
 * O Início de quem está operando.
 *
 * As rotas entram porque é delas que vêm os portos, e é por eles que a concessão responde — a viagem sabe
 * *em quê* e *quando*, não *onde*. O [agora] é parâmetro, e não `LocalDateTime.now()` lá dentro: é o que
 * torna "a saída das 06:00 já partiu" um caso de teste em vez de uma aposta sobre o relógio.
 */
fun inicioDoPainel(
    escopo: EscopoDoPool,
    viagens: List<Viagem>,
    rotasPorId: Map<String, Rota>,
    agora: LocalDateTime,
    dias: Long = DIAS_DA_JANELA,
): InicioDoPainel = when (escopo) {
    EscopoDoPool.Todo -> InicioDoPainel.DaPlataforma
    EscopoDoPool.Nenhum -> InicioDoPainel.SemConcessao
    is EscopoDoPool.Concedido -> InicioDoPainel.DaEmpresa(
        viagens.noEscopo(escopo, rotasPorId).disponiveisAPartirDe(agora, dias),
    )
}