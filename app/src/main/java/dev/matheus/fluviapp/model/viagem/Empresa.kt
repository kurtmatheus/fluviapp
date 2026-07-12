package dev.matheus.fluviapp.model.viagem

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.matheus.fluviapp.services.repository.firebase.documents.EmpresaDocumento

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

fun Empresa.toDocumento() = EmpresaDocumento(
    nome = nome,
    razaoSocial = razaoSocial,
    cnpj = cnpj,
    endereco = endereco,
    telefone1 = telefone1,
    telefone2 = telefone2,
)