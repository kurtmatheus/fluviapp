package br.com.gruponaveg.services.repository.firebase.documents

import br.com.gruponaveg.model.viagem.Empresa

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