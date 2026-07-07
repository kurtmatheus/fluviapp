package dev.matheus.fluviapp.model.viagem

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.matheus.fluviapp.model.IObjetoSimplificado

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