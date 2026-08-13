package dev.matheus.fluviapp.services.repository.pool

import dev.matheus.fluviapp.domain.cliente.Cliente

/**
 * Porta do **pool de clientes** ([ADR-0018] D2/D3, [ADR-0025] D6).
 *
 * Como a porta da passagem, esta se define também pelo que **não** tem: **sem `salvar` e sem `deletar`**. A
 * escrita da agência tem exatamente dois direitos — criar a entrada que não existe e assinar a que existe —,
 * e **corrigir conteúdo é curadoria da plataforma**, feita no painel. Um `salvar` genérico aqui seria a porta
 * por onde a correção alheia entraria: uma agência reescrevendo o nome de alguém que outra cadastrou.
 *
 * E **sem `observarTodos`**: pool cresce sem limite. Onde as sete entidades revitalizadas têm um `StateFlow`
 * da coleção inteira, aqui há [consultarDaAgencia] — a consulta recortada pela assinatura.
 */
interface ClienteRepository {

    /**
     * Registra a pessoa no pool e devolve o **id** (que é a chave natural: `TIPO:numero`).
     *
     * Idempotente por construção: chamar duas vezes com a mesma credencial não cria duas entradas, e assinar
     * duas vezes é o mesmo que assinar uma.
     */
    suspend fun criarOuAssinar(cliente: Cliente, agenciaId: String): String

    suspend fun obterPorId(id: String): Cliente?

    /** Em lote, para a junção — particiona internamente (o `whereIn` aceita 30 por consulta). */
    suspend fun obterPorIds(ids: List<String>): List<Cliente>

    /** Quem **esta agência** já atendeu, por nome. Substitui o `getListaNome()` que devolvia vazio. */
    suspend fun consultarDaAgencia(agenciaId: String): List<Cliente>
}