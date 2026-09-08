package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.domain.operacoes.Funcionario
import dev.matheus.fluviapp.domain.operacoes.Vinculo
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto

/**
 * A forma do documento em `funcionarios/{id}` — **documentação, não caminho** (ADR-0019 D2).
 *
 * Os três campos legados (`agencia`, `lotacao`, `cargo`) continuam gravados enquanto a Passagem os lê
 * (F9). O que nasce aqui é `vinculo`.
 *
 * ### O array virou mapa, e o derivado morreu com ele ([ADR-0032] Q2)
 *
 * Era `vinculos: [{empresaId, cargo}, …]` com um `empresaIds` chato ao lado, e o segundo era
 * denormalização **deliberada**: o Firestore não consulta campo de dentro de elemento de array —
 * `array-contains` casa o elemento inteiro —, então *"quem trabalha na empresa X"* precisava do array de
 * ids do lado.
 *
 * Com **mapa** essa limitação não existe: `vinculo.empresaId` é caminho de campo, indexado e consultável
 * como qualquer outro. O derivado perdeu a única justificativa que tinha, e sai — o que é derivável não
 * vira campo. A regra do servidor lê o mesmo caminho (`data.vinculo.empresaId`) e deixa de depender de os
 * dois campos serem gravados juntos.
 */
data class FuncionarioDocumento(
    val nome: String = "",
    /**
     * **Legado com dono**: derivado do vínculo, gravado só para a regra de *passagem* no servidor
     * (`cargoDoAutor`), que a F9 reescreve. O aplicativo lê o cargo do vínculo.
     */
    val cargo: String = "",
    /** Chave do primeiro acesso (ADR-0015 §2.1) — casa este registro com a conta do Auth. */
    val email: String = "",
    /** `{ empresaId, cargo }` — a atuação não entra: é derivada do cargo (§6.1). Ausente no pré-cadastro. */
    val vinculo: Map<String, Any?>? = null,
)

/**
 * `DocumentoBruto` → domínio.
 *
 * **Não recusa documento**, ao contrário da Embarcação e do Porto, e a diferença é de invariante: um
 * funcionário sem vínculo é um estado legítimo — é o pré-cadastro do §2.1, criado antes de a pessoa ter
 * acesso — enquanto um porto sem localidade não é um porto. Vínculo ilegível vira `null` (é o fail-closed
 * de [Vinculo.de]) sem levar a pessoa junto: perder o nome de quem existe seria pior do que perder um
 * vínculo que ninguém consegue interpretar.
 */
fun DocumentoBruto.toFuncionario(): Funcionario = Funcionario(
    id = id,
    descricaoNome = texto("nome"),
    // Ausente vira AGENTE (o menor privilégio), não "sem cargo": quem tem registro de funcionário já
    // está na operação. Valor DESCONHECIDO, esse, atravessa cru — quem nega é a política (fail-closed).
    cargo = texto("cargo").ifBlank { Funcionario.Cargo.AGENTE.name },
    email = texto("email"),
    vinculo = mapa("vinculo").let {
        Vinculo.de(empresaId = it["empresaId"] as? String, cargo = it["cargo"] as? String)
    },
)

/**
 * Domínio → `Map`. O `id` não entra: ele é o nome do documento.
 *
 * Sem vínculo o campo vai a `null` **explícito**, e não omitido: omitir na atualização deixaria o vínculo
 * antigo no documento, de modo que "tirar o vínculo" não teria como acontecer.
 */
fun Funcionario.paraMapa(): Map<String, Any?> = mapOf(
    "nome" to descricaoNome,
    "cargo" to cargo,
    "email" to email,
    "vinculo" to vinculo?.let { mapOf("empresaId" to it.empresaId, "cargo" to it.cargo.name) },
)
