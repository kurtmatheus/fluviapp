package dev.matheus.fluviapp.database.dao.passagem

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import dev.matheus.fluviapp.database.PassagemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PassagemDao {

    @Insert(onConflict = REPLACE)
    suspend fun salvar(passagem: PassagemEntity)

    /** Gravação em lote — uma transação para o resultado inteiro da consulta, não uma por bilhete. */
    @Insert(onConflict = REPLACE)
    suspend fun salvarTodas(passagens: List<PassagemEntity>)

    @Query("SELECT * FROM Passagem WHERE id = :id")
    fun obterPorId(id: String): Flow<PassagemEntity>

    @Query("SELECT * FROM Passagem WHERE codigoViagem = :codigoViagem")
    fun obterTodasPorViagem(codigoViagem: String): Flow<List<PassagemEntity>>

    @Delete
    suspend fun deletar(passagem: PassagemEntity)
}