package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.domain.viagem.Empresa

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
/**
 * Domínio → documento. Mora **aqui**, e não no arquivo da entidade, porque quem conhece a forma do
 * documento é a camada de dados — o domínio não importa DTO (ADR-0019 D2).
 */
fun Empresa.toDocumento() = EmpresaDocumento(
    nome = nome,
    razaoSocial = razaoSocial,
    cnpj = cnpj,
    endereco = endereco,
    telefone1 = telefone1,
    telefone2 = telefone2,
)
