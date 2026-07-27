package dev.matheus.fluviapp.database.dao.operacoes

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.matheus.fluviapp.model.operacoes.Funcionario
import kotlinx.coroutines.flow.Flow

@Dao
interface FuncionarioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvar(funcionario: Funcionario)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvarTodos(vararg funcionario: Funcionario)

    @Query("SELECT * FROM Funcionario WHERE id = :id")
    fun obterPorId(id: String): Flow<Funcionario?>

    @Query("SELECT * FROM Funcionario")
    fun obterTodos(): Flow<List<Funcionario>>

    @Query("SELECT * FROM Funcionario WHERE agencia = :agencia")
    fun obterTodosPorAgencia(agencia: String): Flow<List<Funcionario>>

    @Query("SELECT agencia From Funcionario")
    fun obterTodasAgencias(): Flow<List<String>>

    @Query("DELETE FROM Funcionario WHERE id = :id")
    suspend fun deletar(id: String)
}
