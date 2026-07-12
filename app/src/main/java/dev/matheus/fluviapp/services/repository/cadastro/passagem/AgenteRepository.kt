package dev.matheus.fluviapp.services.repository.cadastro.passagem

import dev.matheus.fluviapp.model.cadastro.passagem.Agente

/**
 * Porta do repositório de agentes (DIP) — os ViewModels dependem desta interface, não da impl
 * Firestore. Testes usam um fake; produção usa [AgenteFirestoreRepository].
 */
interface AgenteRepository {
    fun sincronizar()
    suspend fun salvar(agente: Agente)
    suspend fun obterPorId(id: String): Agente?
    suspend fun obterTodasAgencias(): List<String>
    suspend fun obterTodosAgentes(): List<Agente>
    suspend fun obterAgentesPorAgencia(agencia: String): List<Agente>

    companion object {
        const val COLLECTION_AGENTS = "agents"
    }
}
