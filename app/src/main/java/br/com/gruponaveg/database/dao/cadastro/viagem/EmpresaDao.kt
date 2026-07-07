package br.com.gruponaveg.database.dao.cadastro.viagem

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.gruponaveg.model.viagem.Empresa
import kotlinx.coroutines.flow.Flow

@Dao
interface EmpresaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvar(empresa: Empresa)

    @Query("SELECT * FROM Empresa")
    fun obterTodas(): Flow<List<Empresa>>

    @Query("SELECT * FROM Empresa WHERE id = :idEmpresa")
    fun obterPorId(idEmpresa: Int): Flow<Empresa>

    @Query("SELECT * FROM Empresa WHERE nome = :nome")
    fun obterPorNome(nome: String): Flow<Empresa>

}
