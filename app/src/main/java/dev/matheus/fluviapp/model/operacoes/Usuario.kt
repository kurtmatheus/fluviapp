package dev.matheus.fluviapp.model.operacoes

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

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
        COLABORADOR_MASTER,
        OPERADOR;

        companion object {
            /** Converte o cargo persistido (String) no enum canônico; null se desconhecido. */
            fun de(valor: String?): Cargo? = entries.firstOrNull { it.name == valor }
        }
    }

    companion object {
        const val GERAL = "Geral"
    }
}

fun Usuario.Cargo.obterDescricaoFormatada(): String {
    return this.name.replace("_", " ")
}