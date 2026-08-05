package dev.matheus.fluviapp.services.repository.cadastro.viagem

import dev.matheus.fluviapp.domain.viagem.Embarcacao
import kotlinx.coroutines.flow.StateFlow

/**
 * Porta do repositório de embarcações (DIP) — os ViewModels dependem desta interface, não da impl
 * Firestore. Testes usam um fake; produção usa [EmbarcacaoFirestoreRepository].
 *
 * [observarTodas] é a **fonte reativa** que o ADR-0017 D1 põe no lugar do `Flow` do DAO. [obterTodos]
 * continua para quem lê uma vez só, e espera o primeiro snapshot — não devolve vazio quando o dado apenas
 * ainda não chegou.
 *
 * `obterPorNome` **saiu**: não tinha chamador de produção desde que as relações passaram a ser por id
 * (ADR-0008) — casar por nome era justamente o que estourava quando a embarcação era renomeada. É a mesma
 * remoção que a Empresa já havia sofrido, pelo mesmo motivo.
 */
interface EmbarcacaoRepository {
    fun sincronizar()
    fun observarTodas(): StateFlow<List<Embarcacao>>

    /** Salva e devolve **o id** — necessário porque a criação o gera. */
    suspend fun salvar(embarcacao: Embarcacao): String

    suspend fun obterTodos(): List<Embarcacao>
    suspend fun obterPorId(id: String): Embarcacao?
    suspend fun deletar(id: String)
}