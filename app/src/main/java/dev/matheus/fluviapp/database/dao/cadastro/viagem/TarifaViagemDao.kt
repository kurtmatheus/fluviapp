package dev.matheus.fluviapp.database.dao.cadastro.viagem

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import dev.matheus.fluviapp.model.viagem.TarifaViagem
import kotlinx.coroutines.flow.Flow

/**
 * Acesso à tabela-filha de tarifas da Viagem (ADR-0013). Espelho local do mapa de tarifas do
 * `ViagemDocumento`. Substituir o conjunto de uma viagem = [deletarPorViagem] + [salvarTodas]
 * (REPLACE cobre re-cadastro da mesma célula, mas não remove célula que saiu do mapa).
 */
@Dao
interface TarifaViagemDao {

    @Insert(onConflict = REPLACE)
    suspend fun salvarTodas(vararg tarifas: TarifaViagem)

    @Query("SELECT * FROM TarifaViagem WHERE viagemId = :viagemId")
    fun obterPorViagem(viagemId: String): Flow<List<TarifaViagem>>

    @Query("SELECT * FROM TarifaViagem WHERE viagemId = :viagemId")
    suspend fun obterPorViagemAgora(viagemId: String): List<TarifaViagem>

    @Query("DELETE FROM TarifaViagem WHERE viagemId = :viagemId")
    suspend fun deletarPorViagem(viagemId: String)

    @Query("SELECT * FROM TarifaViagem")
    fun obterTodas(): Flow<List<TarifaViagem>>
}