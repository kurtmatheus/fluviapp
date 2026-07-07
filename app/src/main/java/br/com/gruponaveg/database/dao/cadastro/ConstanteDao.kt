package br.com.gruponaveg.database.dao.cadastro

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.gruponaveg.model.cadastro.constantes.Constante
import kotlinx.coroutines.flow.Flow

@Dao
interface ConstanteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvar(constante: Constante)

    @Query("SELECT * FROM Constante WHERE id = :id")
    fun obterPorId(id: String): Flow<Constante?>

    @Query("SELECT * FROM Constante")
    fun obterTodos(): Flow<List<Constante>>

    @Query("SELECT * FROM Constante WHERE categoria = :categoria")
        fun obterTodosPorCategoria(categoria: String): Flow<List<Constante>>

    @Query("SELECT * FROM Constante WHERE descricaoNome = :descricao")
    fun obterPorDescricao(descricao: String): Flow<Constante>
}