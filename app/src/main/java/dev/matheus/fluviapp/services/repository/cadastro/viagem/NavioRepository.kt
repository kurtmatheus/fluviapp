package dev.matheus.fluviapp.services.repository.cadastro.viagem

import dev.matheus.fluviapp.model.viagem.Navio

/**
 * Porta do repositório de navios (DIP). Testes usam um fake; produção usa
 * [NavioFirestoreRepository].
 */
interface NavioRepository {
    fun sincronizar()
    suspend fun obterTodos(): List<Navio>
    suspend fun obterPorId(id: String): Navio
    suspend fun salvar(navio: Navio)
    suspend fun obterPorNome(nome: String): Navio
}
