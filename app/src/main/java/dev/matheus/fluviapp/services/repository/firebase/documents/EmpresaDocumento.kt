package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.model.viagem.Empresa

data class EmpresaDocumento(
    val nome: String = "",
    val razaoSocial: String = "",
    val cnpj: String = "",
    val endereco: String = "",
    val telefone1: String = "",
    val telefone2: String = "",
)

fun EmpresaDocumento.toEmpresa(id: String): Empresa {
    return Empresa(
        id = id,
        nome = nome,
        razaoSocial = razaoSocial,
        cnpj = cnpj,
        endereco = endereco,
        telefone1 = telefone1,
        telefone2 = telefone2
    )
}