package dev.matheus.fluviapp.domain.viagem

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.matheus.fluviapp.services.repository.firebase.documents.ViagemDocumento

@Entity(indices = [Index("id")])
data class Viagem(
    @PrimaryKey
    val id: String,
    val codigo: String,
    val origem: String,
    val destino: String,
    // Vínculo N-1 com Empresa/Navio só por id (ADR-0008, Fase 3). Os nomes são resolvidos na fronteira
    // de escrita: derivação do `codigo` (repositório) e snapshot da Passagem (emissão).
    val empresaId: String,
    val navioId: String,
)

fun Viagem.toDocumento(): ViagemDocumento {
    // empresa/navio (nomes) ficam vazios no doc de /viagens — o vínculo é por id. Os nomes só
    // aparecem no ViagemDocumento *embutido* na Passagem (snapshot), montado à parte na emissão.
    return ViagemDocumento(
        codigo = codigo,
        origem = origem,
        destino = destino,
        empresaId = empresaId,
        navioId = navioId,
    )
}