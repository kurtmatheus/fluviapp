package br.com.gruponaveg.database.dao.cadastro.viagem

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import br.com.gruponaveg.model.viagem.Viagem
import kotlinx.coroutines.flow.Flow

@Dao
interface ViagemDao {

    @Insert(onConflict = REPLACE)
    suspend fun salvar(viagem: Viagem)

    @Query("SELECT * FROM Viagem")
    fun obterTodas(): Flow<List<Viagem>>

    @Query("SELECT * FROM Viagem WHERE id = :id")
    fun obterPorId(id: String): Flow<Viagem>

    @Delete
    suspend fun deletar(viagem: Viagem)

    @Insert(onConflict = REPLACE)
    suspend fun salvarTodas(vararg viagem: Viagem)

    @Query("SELECT COUNT(*) From Viagem")
    fun obterContagem(): Flow<Int>

    @Query("SELECT * FROM Viagem WHERE codigo = :codigo")
    fun obterPorCodigo(codigo: String): Flow<Viagem>
}