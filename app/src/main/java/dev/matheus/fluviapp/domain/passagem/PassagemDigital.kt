package dev.matheus.fluviapp.domain.passagem

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index("id")])
data class PassagemDigital(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val idPassagem: String,
    val caminho: String
)
