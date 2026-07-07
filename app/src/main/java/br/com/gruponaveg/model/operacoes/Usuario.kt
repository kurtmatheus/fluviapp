package br.com.gruponaveg.model.operacoes

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.gruponaveg.model.operacoes.Usuario.Cargo.ADM
import br.com.gruponaveg.model.operacoes.Usuario.Cargo.COLABORADOR_MASTER
import br.com.gruponaveg.model.operacoes.Usuario.Cargo.DIRETOR

@Entity(indices = [Index("id")])
data class Usuario(
    @PrimaryKey
    val id: String,
    val email: String,
    val senha: String,
    val nome: String,
    val cargo: String,
    val ultimoUsuarioLogado: Boolean = false
) {
    enum class Cargo {
        ADM,
        DIRETOR,
        COLABORADOR_MASTER
    }

    companion object {
        const val GERAL = "Geral"
    }
}

fun Usuario.Cargo.obterDescricaoFormatada(): String {
    return this.name.replace("_", " ")
}

fun Usuario.temPermissaoEspecialPassagem(): Boolean {
    return cargo == ADM.name ||
            cargo == DIRETOR.name ||
            cargo == COLABORADOR_MASTER.obterDescricaoFormatada()
}