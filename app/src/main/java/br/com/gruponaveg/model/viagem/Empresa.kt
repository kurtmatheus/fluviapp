package br.com.gruponaveg.model.viagem

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index("id")])
data class Empresa(
    @PrimaryKey
    val id: String,
    val nome: String,
    val razaoSocial: String,
    val cnpj: String,
    val endereco: String,
    val telefone1: String,
    val telefone2: String,
)