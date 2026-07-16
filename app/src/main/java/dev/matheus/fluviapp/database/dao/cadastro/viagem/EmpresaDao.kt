package dev.matheus.fluviapp.database.dao.cadastro.viagem

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.matheus.fluviapp.model.viagem.Empresa
import kotlinx.coroutines.flow.Flow

@Dao
interface EmpresaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvar(empresa: Empresa)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvarTodas(vararg empresa: Empresa)

    @Query("SELECT * FROM Empresa")
    fun obterTodas(): Flow<List<Empresa>>

    @Query("SELECT * FROM Empresa WHERE id = :id")
    fun obterPorId(id: String): Flow<Empresa?>

    @Query("SELECT * FROM Empresa WHERE nome = :nome")
    fun obterPorNome(nome: String): Flow<Empresa>

    @Query("DELETE FROM Empresa WHERE id = :id")
    suspend fun deletar(id: String)
}
