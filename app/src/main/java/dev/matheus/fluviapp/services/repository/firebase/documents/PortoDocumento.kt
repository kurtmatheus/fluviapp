package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.domain.porto.Porto
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto

/**
 * A forma do documento em `portos/{id}` — **documentação, não caminho** (ADR-0019 D2).
 */
data class PortoDocumento(
    val nome: String = "",
    /** Id da `Localidade` — referência, não cópia (ADR-0016 §5). */
    val localidadeId: String = "",
    /** Delete lógico: `false` some das listas, mas continua resolvível por id. */
    val ativo: Boolean = true,
)

/**
 * `DocumentoBruto` → domínio. Devolve `null` quando **não é um porto**: sem `localidadeId` não há porto,
 * há um nome solto. A recusa é do mesmo tipo da Embarcação sem tipo — o invariante é estrutural, e um
 * documento que não o cumpre não vira porto de localidade padrão, não vira nada.
 *
 * O `nome` em branco **não** recusa, e a assimetria é proposital: nome vazio é um cadastro malfeito, que
 * a tela mostra e alguém corrige; localidade ausente é uma referência quebrada, que nenhuma tela
 * conserta e que faria a lista exibir um porto sem lugar nenhum.
 *
 * O `ativo` ausente é lido como **`true`**, como na Localidade: o campo governa visibilidade, não
 * permissão, e assumir `false` esconderia dado bom.
 */
fun DocumentoBruto.toPorto(): Porto? {
    val localidadeId = texto("localidadeId")
    if (localidadeId.isBlank()) return null

    return Porto(
        id = id,
        nome = texto("nome"),
        localidadeId = localidadeId,
        ativo = booleano("ativo", padrao = true),
    )
}

/** Domínio → `Map`. O `id` não entra: ele é o nome do documento. */
fun Porto.paraMapa(): Map<String, Any?> = mapOf(
    "nome" to nome,
    "localidadeId" to localidadeId,
    "ativo" to ativo,
)