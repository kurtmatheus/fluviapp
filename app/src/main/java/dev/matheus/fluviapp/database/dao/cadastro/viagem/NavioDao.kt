package dev.matheus.fluviapp.database.dao.cadastro.viagem

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import dev.matheus.fluviapp.model.viagem.Navio
import kotlinx.coroutines.flow.Flow

@Dao
interface NavioDao {

    @Insert(onConflict = REPLACE)
    suspend fun salvar(navio: Navio)

    @Query("SELECT * FROM Navio")
    fun obterTodos(): Flow<List<Navio>>

    @Query("SELECT * FROM Navio WHERE id = :id")
    fun obterPorId(id: String): Flow<Navio>

    @Insert(onConflict = REPLACE)
    suspend fun salvarTodos(vararg navio: Navio)

    @Query("SELECT * FROM Navio WHERE descricaoNome = :nome")
    fun obterPorNome(nome: String): Flow<Navio>
}
