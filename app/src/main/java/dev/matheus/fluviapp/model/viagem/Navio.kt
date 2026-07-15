package dev.matheus.fluviapp.model.viagem

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.matheus.fluviapp.model.IObjetoSimplificado
import dev.matheus.fluviapp.services.repository.firebase.documents.NavioDocumento

@Entity(indices = [Index("id")])
data class Navio(
    @PrimaryKey
    override val id: String,
    override val descricaoNome: String,
    val capacidadeVeiculo: Int,
    val capacidadeSuite2: Int,
    val capacidadeSuite3: Int,
    val capacidadeCamarote: Int,
    val empresa: String,
    // Link estável para Empresa (ADR-0008, Fase 0/1). Dormente: `empresa` (nome) segue sendo o
    // campo lido; `empresaId` é gravado mas ainda não consultado. Ler por id = Fase 2.
    val empresaId: String = "",
): IObjetoSimplificado

/** Model → documento do Firestore (espelho ADR-0003). Vínculo N-1 por nome (`empresa`) + id (ADR-0008). */
fun Navio.toDocumento() = NavioDocumento(
    nome = descricaoNome,
    capacidadeVeiculo = capacidadeVeiculo,
    capacidadeSuite2 = capacidadeSuite2,
    capacidadeSuite3 = capacidadeSuite3,
    capacidadeCamarote = capacidadeCamarote,
    empresa = empresa,
    empresaId = empresaId,
)