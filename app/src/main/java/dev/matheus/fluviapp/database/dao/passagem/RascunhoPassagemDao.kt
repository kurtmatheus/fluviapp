package dev.matheus.fluviapp.database.dao.passagem

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.matheus.fluviapp.database.RascunhoPassagemEntity

@Dao
interface RascunhoPassagemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvar(rascunho: RascunhoPassagemEntity)

    @Query("SELECT * FROM rascunho_passagem WHERE id = :id LIMIT 1")
    suspend fun recuperar(id: Int = RascunhoPassagemEntity.ID_UNICO): RascunhoPassagemEntity?

    @Query("DELETE FROM rascunho_passagem")
    suspend fun remover()
}