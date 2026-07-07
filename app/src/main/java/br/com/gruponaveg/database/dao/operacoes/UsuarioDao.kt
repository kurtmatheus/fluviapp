package br.com.gruponaveg.database.dao.operacoes

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import br.com.gruponaveg.model.operacoes.Usuario
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {

    @Insert(onConflict = REPLACE)
    suspend fun salvar(usuario: Usuario)

    @Query("SELECT * FROM Usuario WHERE email = :email")
    fun obterPorEmail(email: String): Flow<Usuario?>

    @Query("SELECT * FROM Usuario WHERE ultimoUsuarioLogado = 1")
    fun obterUltimoUsuarioLogado(): Flow<Usuario?>

    @Query("UPDATE Usuario SET ultimoUsuarioLogado = 0")
    suspend fun limparUltimoUsuarioLogado()

    @Query("SELECT * FROM Usuario WHERE email = :email AND senha = :senha")
    fun obterPorUsuarioESenha(email: String, senha: String): Flow<Usuario?>

    @Query("SELECT * FROM Usuario")
    fun obterTodos(): Flow<List<Usuario>>
}
