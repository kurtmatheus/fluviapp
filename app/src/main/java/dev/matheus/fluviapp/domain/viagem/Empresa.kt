package dev.matheus.fluviapp.domain.viagem

/**
 * A **parte**: quem existe no mundo, com identidade e CNPJ (ADR-0016 §4). É superentidade — não é
 * "empresa de navegação" nem "agência": o que ela *faz* é a [AtuacaoDaEmpresa], e uma parte exerce
 * várias ao mesmo tempo.
 *
 * **Primeira entidade de domínio livre de framework** (ADR-0017 D1, ADR-0020 F5): saiu o `@Entity` do
 * Room, saiu o import do `EmpresaDocumento` (ADR-0019 D2). Não sobrou anotação nem dependência de
 * infraestrutura — é uma data class que o negócio entende inteira, e o resto do domínio segue por aqui.
 */
data class Empresa(
    val id: String,
    val nome: String,
    val razaoSocial: String,
    val cnpj: String,
    val endereco: String,
    val telefone1: String,
    val telefone2: String,
)