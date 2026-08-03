package dev.matheus.fluviapp.database.dao.passagem

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import dev.matheus.fluviapp.domain.passagem.Passagem
import kotlinx.coroutines.flow.Flow

@Dao
interface PassagemDao {

    @Insert(onConflict = REPLACE)
    suspend fun salvar(passagem: Passagem)

    /** Gravação em lote — uma transação para o resultado inteiro da consulta, não uma por bilhete. */
    @Insert(onConflict = REPLACE)
    suspend fun salvarTodas(passagens: List<Passagem>)

    @Query("SELECT * FROM Passagem WHERE id = :id")
    fun obterPorId(id: String): Flow<Passagem>

    @Query("SELECT * FROM Passagem WHERE codigoViagem = :codigoViagem")
    fun obterTodasPorViagem(codigoViagem: String): Flow<List<Passagem>>

    @Delete
    suspend fun deletar(passagem: Passagem)
}