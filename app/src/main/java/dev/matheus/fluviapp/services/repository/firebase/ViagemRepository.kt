package dev.matheus.fluviapp.services.repository.firebase

import dev.matheus.fluviapp.model.viagem.Viagem

/**
 * Porta do repositório de viagens (DIP) — os ViewModels dependem desta interface, não da impl
 * Firestore. Testes usam um fake; produção usa [ViagemFirestoreRepository].
 */
interface ViagemRepository {
    fun sincronizar()
    suspend fun salvar(viagem: Viagem)
    suspend fun obterPorId(id: String): Viagem
    suspend fun obterTodas(): List<Viagem>
    suspend fun deletar(id: String)
}
