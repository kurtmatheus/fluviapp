package dev.matheus.fluviapp.domain.viagem

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.matheus.fluviapp.domain.IObjetoSimplificado
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
    // Vínculo N-1 com Empresa por id estável (ADR-0008, Fase 3 — o nome foi aposentado). O nome de
    // exibição é resolvido na leitura contra a lista de empresas em cache.
    val empresaId: String,
): IObjetoSimplificado

/** Model → documento do Firestore (espelho ADR-0003). Vínculo N-1 por id (`empresaId`, ADR-0008). */
fun Navio.toDocumento() = NavioDocumento(
    nome = descricaoNome,
    capacidadeVeiculo = capacidadeVeiculo,
    capacidadeSuite2 = capacidadeSuite2,
    capacidadeSuite3 = capacidadeSuite3,
    capacidadeCamarote = capacidadeCamarote,
    empresaId = empresaId,
)