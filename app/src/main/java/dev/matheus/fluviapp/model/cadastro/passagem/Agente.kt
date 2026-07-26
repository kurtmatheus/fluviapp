package dev.matheus.fluviapp.model.cadastro.passagem

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.matheus.fluviapp.model.IObjetoSimplificado
import dev.matheus.fluviapp.services.repository.firebase.documents.AgenteDocumento

@Entity(indices = [Index("id")])
data class Agente(
    @PrimaryKey
    override val id: String,
    override val descricaoNome: String,
    val agencia: String,
    var lotacao: String,
) : IObjetoSimplificado {
    enum class Agencia {
        MATRIZ,
    }

    enum class Lotacao {
        PORTO_NORTE,
        ILHA_CENTRAL,
        PORTO_SUL
    }
}

fun Agente.toDocumento(): AgenteDocumento {
    return AgenteDocumento(
        nome = descricaoNome,
        agencia = agencia,
        lotacao = lotacao
    )
}
