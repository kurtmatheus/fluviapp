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
    // empresa/navio (nomes) são mantidos: alimentam a derivação do `codigo` e o snapshot da Passagem
    // na emissão (ADR-0008 — Viagem é fonte de snapshot). O vínculo vivo é por id (empresaId/navioId).
    val empresa: String,
    val navio: String,
    val origem: String,
    val destino: String,
    val empresaId: String = "",
    val navioId: String = "",
)

fun Viagem.toDocumento(): ViagemDocumento {
    return ViagemDocumento(
        codigo = codigo,
        empresa = empresa,
        navio = navio,
        origem = origem,
        destino = destino,
        empresaId = empresaId,
        navioId = navioId,
    )
}