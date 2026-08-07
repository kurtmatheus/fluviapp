package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.domain.operacoes.Funcionario
import dev.matheus.fluviapp.domain.operacoes.Vinculo
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto

/**
 * A forma do documento em `funcionarios/{id}` — **documentação, não caminho** (ADR-0019 D2).
 *
 * Os três campos legados (`agencia`, `lotacao`, `cargo`) continuam gravados enquanto a Passagem os lê
 * (F9). O que nasce aqui é `vinculos` + `empresaIds`, e o segundo é **denormalização deliberada**
 * (ADR-0016 §6): o Firestore não consulta campo de dentro de elemento de array — `array-contains` casa o
 * elemento inteiro —, então *"quem trabalha na empresa X"* precisa do array chato ao lado. É o caso mais
 * barato de dado derivado que existe: mesma escrita, mesmo documento, sem sincronia entre coleções.
 */
data class FuncionarioDocumento(
    val nome: String = "",
    /** **Legado** (F6.3). */
    val agencia: String = "",
    /** **Legado** (F6.3). */
    val lotacao: String = "",
    /** **Legado** (F6.3) — cargo de negócio; documento antigo sem o campo lê como vazio. */
    val cargo: String = "",
    /** Chave do primeiro acesso (ADR-0015 §2.1) — casa este registro com a conta do Auth. */
    val email: String = "",
    /** `[{ empresaId, cargo }, …]` — a atuação não entra: é derivada do cargo (§6.1). */
    val vinculos: List<Map<String, Any?>> = emptyList(),
    /** Derivado de `vinculos`, só para consulta. */
    val empresaIds: List<String> = emptyList(),
)

/**
 * `DocumentoBruto` → domínio.
 *
 * **Não recusa documento**, ao contrário da Embarcação e do Porto, e a diferença é de invariante: um
 * funcionário sem vínculo é um estado legítimo — é o pré-cadastro do §2.1, criado antes de a pessoa ter
 * acesso — enquanto um porto sem localidade não é um porto. Vínculo ilegível some da lista (é o
 * `mapNotNull` de [Vinculo.de]) sem levar a pessoa junto: perder o nome de quem existe seria pior do que
 * perder um vínculo que ninguém consegue interpretar.
 */
fun DocumentoBruto.toFuncionario(): Funcionario = Funcionario(
    id = id,
    descricaoNome = texto("nome"),
    agencia = texto("agencia"),
    lotacao = texto("lotacao"),
    // Ausente vira AGENTE (o menor privilégio), não "sem cargo": quem tem registro de funcionário já
    // está na operação. Valor DESCONHECIDO, esse, atravessa cru — quem nega é a política (fail-closed).
    cargo = texto("cargo").ifBlank { Funcionario.Cargo.AGENTE.name },
    email = texto("email"),
    vinculos = listaDeMapas("vinculos").mapNotNull {
        Vinculo.de(empresaId = it["empresaId"] as? String, cargo = it["cargo"] as? String)
    },
)

/**
 * Domínio → `Map`. O `id` não entra: ele é o nome do documento.
 *
 * `empresaIds` é gravado **a partir de** `vinculos`, na mesma escrita — nunca recebido de fora. É o que
 * impede o derivado de divergir da origem: não há caminho em que alguém grave um sem o outro.
 */
fun Funcionario.paraMapa(): Map<String, Any?> = mapOf(
    "nome" to descricaoNome,
    "agencia" to agencia,
    "lotacao" to lotacao,
    "cargo" to cargo,
    "email" to email,
    "vinculos" to vinculos.map { mapOf("empresaId" to it.empresaId, "cargo" to it.cargo.name) },
    "empresaIds" to empresaIds,
)