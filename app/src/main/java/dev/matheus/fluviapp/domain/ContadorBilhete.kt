package dev.matheus.fluviapp.domain

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index("id")])
data class ContadorBilhete(
    @PrimaryKey
    val id: Int = 1,
    val contagem: Int
)
