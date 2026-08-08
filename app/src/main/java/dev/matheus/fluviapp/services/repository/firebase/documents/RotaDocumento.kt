package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.domain.rota.Rota
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto

/**
 * A forma do documento em `rotas/{id}` — **documentação, não caminho** (ADR-0019 D2).
 */
data class RotaDocumento(
    val portoOrigemId: String = "",
    val portoDestinoId: String = "",
    val distanciaMn: Double = 0.0,
    val tempoMedioH: Double = 0.0,
    /** Num pool sem dono, a assinatura é o que resta de responsabilidade (§7.1). */
    val criadoPor: String = "",
    val criadoEm: String = "",
    /** Delete lógico: rota fora de uso some dos seletores e continua resolvendo o que aponta para ela. */
    val ativo: Boolean = true,
)

/**
 * `DocumentoBruto` → domínio. Devolve `null` quando **não é uma rota**: sem os dois portos não há
 * ligação nenhuma — é a mesma recusa estrutural do Porto sem localidade, um nível acima.
 *
 * Ela não checa o **sentido** (origem ≠ destino): documento com os dois portos iguais é dado ruim, não
 * "não é uma rota", e escondê-lo da lista esconderia justamente o que alguém precisa inativar.
 */
fun DocumentoBruto.toRota(): Rota? {
    val origem = texto("portoOrigemId")
    val destino = texto("portoDestinoId")
    if (origem.isBlank() || destino.isBlank()) return null

    return Rota(
        id = id,
        portoOrigemId = origem,
        portoDestinoId = destino,
        distanciaMn = decimal("distanciaMn"),
        tempoMedioH = decimal("tempoMedioH"),
        criadoPor = texto("criadoPor"),
        criadoEm = texto("criadoEm"),
        ativo = booleano("ativo", padrao = true),
    )
}

/** Domínio → `Map`. O `id` não entra: ele é o nome do documento. */
fun Rota.paraMapa(): Map<String, Any?> = mapOf(
    "portoOrigemId" to portoOrigemId,
    "portoDestinoId" to portoDestinoId,
    "distanciaMn" to distanciaMn,
    "tempoMedioH" to tempoMedioH,
    "criadoPor" to criadoPor,
    "criadoEm" to criadoEm,
    "ativo" to ativo,
)