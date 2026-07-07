package dev.matheus.fluviapp.model.viagem

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.matheus.fluviapp.services.repository.firebase.documents.ViagemDocumento

@Entity(indices = [Index("id")])
data class Viagem(
    @PrimaryKey
    val id: String,
    val codigo: String,
    val empresa: String,
    val navio: String,
    val origem: String,
    val destino: String
)

fun Viagem.toDocumento(): ViagemDocumento {
    return ViagemDocumento(
        codigo = codigo,
        empresa = empresa,
        navio = navio,
        origem = origem,
        destino = destino
    )
}