package dev.matheus.fluviapp.database.dao.cadastro

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante
import kotlinx.coroutines.flow.Flow

@Dao
interface ConstanteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvar(constante: Constante)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvarTodas(vararg constante: Constante)

    @Query("SELECT * FROM Constante WHERE id = :id")
    fun obterPorId(id: String): Flow<Constante?>

    @Query("SELECT * FROM Constante")
    fun obterTodos(): Flow<List<Constante>>

    @Query("SELECT * FROM Constante WHERE categoria = :categoria")
        fun obterTodosPorCategoria(categoria: String): Flow<List<Constante>>

    @Query("SELECT * FROM Constante WHERE descricaoNome = :descricao")
    fun obterPorDescricao(descricao: String): Flow<Constante>
}