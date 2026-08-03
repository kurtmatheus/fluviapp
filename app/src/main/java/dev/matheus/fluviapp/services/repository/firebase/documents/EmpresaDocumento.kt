package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.domain.viagem.Empresa
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto

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

/**
 * `DocumentoBruto` → domínio, **sem passar pelo DTO** (ADR-0019 D2). O salto intermediário
 * `DocumentoBruto → EmpresaDocumento → Empresa` existia porque a fronteira era uma data class; com a
 * fronteira em `Map`, ele só repetia os mesmos seis campos mais uma vez.
 */
fun DocumentoBruto.toEmpresa() = Empresa(
    id = id,
    nome = texto("nome"),
    razaoSocial = texto("razaoSocial"),
    cnpj = texto("cnpj"),
    endereco = texto("endereco"),
    telefone1 = texto("telefone1"),
    telefone2 = texto("telefone2"),
)

/**
 * Domínio → `Map`, que é o que o Firestore grava (ADR-0019 D2). O `id` **não entra no mapa**: ele é o
 * nome do documento, não um campo dele — duplicá-lo criaria duas fontes para a mesma identidade.
 */
fun Empresa.paraMapa(): Map<String, Any?> = mapOf(
    "nome" to nome,
    "razaoSocial" to razaoSocial,
    "cnpj" to cnpj,
    "endereco" to endereco,
    "telefone1" to telefone1,
    "telefone2" to telefone2,
)
