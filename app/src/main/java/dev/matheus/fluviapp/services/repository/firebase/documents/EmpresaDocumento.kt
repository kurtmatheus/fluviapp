package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.domain.viagem.Empresa
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto

/**
 * A forma do documento em `empresas/{id}` — **documentação, não caminho** (ADR-0019 D2). A leitura e a
 * escrita passam direto por `Map`, abaixo; esta data class fica como registro de quais campos existem.
 *
 * As duas conversões que a acompanhavam — `EmpresaDocumento.toEmpresa(id)` e `Empresa.toDocumento()` —
 * saíram em 2026-09-07 sem chamador: eram as pontas do salto intermediário que o próprio comentário
 * abaixo já dava por encerrado.
 */
data class EmpresaDocumento(
    val nome: String = "",
    val razaoSocial: String = "",
    val cnpj: String = "",
    val endereco: String = "",
    val telefone1: String = "",
    val telefone2: String = "",
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
