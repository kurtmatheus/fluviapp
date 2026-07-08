package dev.matheus.fluviapp.model.operacoes

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.matheus.fluviapp.model.operacoes.Usuario.Cargo.ADM
import dev.matheus.fluviapp.model.operacoes.Usuario.Cargo.COLABORADOR_MASTER
import dev.matheus.fluviapp.model.operacoes.Usuario.Cargo.DIRETOR

@Entity(indices = [Index("id")])
data class Usuario(
    @PrimaryKey
    val id: String,
    val email: String,
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