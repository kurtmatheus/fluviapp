package br.com.gruponaveg.model.cadastro.passagem

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.gruponaveg.model.IObjetoSimplificado
import br.com.gruponaveg.services.repository.firebase.documents.AgenteDocumento

@Entity(indices = [Index("id")])
data class Agente(
    @PrimaryKey
    override val id: String,
    override val descricaoNome: String,
    val agencia: String,
    var lotacao: String,
) : IObjetoSimplificado {
    enum class Nome {
        ODAIR,
        ADRIELY
    }

    enum class Agencia {
        NAVEG,
    }

    enum class Lotacao {
        BELEM,
        SANTANA,
        BREVES
    }
}

fun Agente.toDocumento(): AgenteDocumento {
    return AgenteDocumento(
        nome = descricaoNome,
        agencia = agencia,
        lotacao = lotacao
    )
}
