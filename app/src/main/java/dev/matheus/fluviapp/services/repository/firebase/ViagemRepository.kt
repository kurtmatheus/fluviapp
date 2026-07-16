package dev.matheus.fluviapp.services.repository.firebase

import dev.matheus.fluviapp.model.viagem.Viagem
import kotlinx.coroutines.flow.Flow

/**
 * Porta do repositório de viagens (DIP) — os ViewModels dependem desta interface, não da impl
 * Firestore. Testes usam um fake; produção usa [ViagemFirestoreRepository].
 */
interface ViagemRepository {
    fun sincronizar()
    suspend fun salvar(viagem: Viagem)
    suspend fun obterPorId(id: String): Viagem
    suspend fun obterTodas(): List<Viagem>
    /** Observação reativa do espelho Room (SSOT — estudo sincronizacao-firestore-room.md, D1). */
    fun observarTodas(): Flow<List<Viagem>>
    /** Pull-to-refresh (D5): força busca no servidor e grava no Room; o Flow reativo reflete. */
    suspend fun atualizarDoServidor()
    suspend fun deletar(id: String)
}
