package dev.matheus.fluviapp.services.repository.firebase

import dev.matheus.fluviapp.model.viagem.TarifaViagem
import dev.matheus.fluviapp.model.viagem.Viagem
import kotlinx.coroutines.flow.Flow

/**
 * Porta do repositório de viagens (DIP) — os ViewModels dependem desta interface, não da impl
 * Firestore. Testes usam um fake; produção usa [ViagemFirestoreRepository].
 */
interface ViagemRepository {
    fun sincronizar()
    /** Salva a viagem e sua tabela de tarifas (ADR-0013) — os dois juntos, como agregado. */
    suspend fun salvar(viagem: Viagem, tarifas: List<TarifaViagem>)
    suspend fun obterPorId(id: String): Viagem
    /** Tarifas cadastradas da viagem (ADR-0013), p/ prefill na edição. */
    suspend fun obterTarifas(viagemId: String): List<TarifaViagem>
    suspend fun obterTodas(): List<Viagem>
    /** Observação reativa do espelho Room (SSOT — estudo sincronizacao-firestore-room.md, D1). */
    fun observarTodas(): Flow<List<Viagem>>
    /** Pull-to-refresh (D5): força busca no servidor e grava no Room; o Flow reativo reflete. */
    suspend fun atualizarDoServidor()
    suspend fun deletar(id: String)
}
