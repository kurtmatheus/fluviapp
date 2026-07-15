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
    val empresa: String
): IObjetoSimplificado

/** Model → documento do Firestore (espelho ADR-0003). Vínculo N-1 por nome (`empresa`). */
fun Navio.toDocumento() = NavioDocumento(
    nome = descricaoNome,
    capacidadeVeiculo = capacidadeVeiculo,
    capacidadeSuite2 = capacidadeSuite2,
    capacidadeSuite3 = capacidadeSuite3,
    capacidadeCamarote = capacidadeCamarote,
    empresa = empresa,
)