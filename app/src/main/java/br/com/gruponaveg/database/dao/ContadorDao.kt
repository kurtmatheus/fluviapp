package br.com.gruponaveg.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.gruponaveg.model.ContadorBilhete
import kotlinx.coroutines.flow.Flow

@Dao
interface ContadorDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun atualizarContagem(contadorBilhete: ContadorBilhete)

    @Query("SELECT contagem FROM ContadorBilhete")
    fun obterContagem(): Flow<Int>
}
