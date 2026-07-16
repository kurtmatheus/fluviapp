package dev.matheus.fluviapp.database.dao.cadastro.passagem

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.matheus.fluviapp.model.cadastro.passagem.Agente
import kotlinx.coroutines.flow.Flow

@Dao
interface AgenteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvar(agente: Agente)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvarTodos(vararg agente: Agente)

    @Query("SELECT * FROM Agente WHERE id = :id")
    fun obterPorId(id: String): Flow<Agente?>

    @Query("SELECT * FROM Agente")
    fun obterTodos(): Flow<List<Agente>>

    @Query("SELECT * FROM Agente WHERE agencia = :agencia")
    fun obterTodosPorAgencia(agencia: String): Flow<List<Agente>>

    @Query("SELECT agencia From Agente")
    fun obterTodasAgencias(): Flow<List<String>>

    @Query("DELETE FROM Agente WHERE id = :id")
    suspend fun deletar(id: String)
}
