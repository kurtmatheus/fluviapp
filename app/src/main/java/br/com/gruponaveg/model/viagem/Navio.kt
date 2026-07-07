package br.com.gruponaveg.model.viagem

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.gruponaveg.model.IObjetoSimplificado

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