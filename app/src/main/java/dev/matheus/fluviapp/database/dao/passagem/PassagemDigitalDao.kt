package dev.matheus.fluviapp.database.dao.passagem

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import dev.matheus.fluviapp.domain.passagem.PassagemDigital
import kotlinx.coroutines.flow.Flow

@Dao
interface PassagemDigitalDao {

    @Insert(onConflict = REPLACE)
    suspend fun salvar(passagemDigital: PassagemDigital)

    @Query("SELECT * FROM PassagemDigital WHERE idPassagem = :idPassagem")
    fun obterPorPassagem(idPassagem: String): Flow<PassagemDigital?>
}