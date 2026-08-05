package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.domain.localidade.Localidade
import dev.matheus.fluviapp.domain.localidade.Uf
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto

/**
 * A forma do documento em `localidades/{id}` — **documentação, não caminho** (ADR-0019 D2).
 */
data class LocalidadeDocumento(
    val municipio: String = "",
    /** Sigla — o `name` do [Uf], que é o valor canônico. */
    val uf: String = "",
    /** 7 dígitos do IBGE; chave natural da dimensão (ADR-0016 §5). */
    val codigoIbge: String = "",
    /** Delete lógico: `false` some das listas, mas continua resolvível por id. */
    val ativo: Boolean = true,
)

/**
 * `DocumentoBruto` → domínio. Devolve `null` quando **não é uma localidade** — mesma família de recusa da
 * Embarcação: a `Uf` é tipo fechado, e um documento com uf ilegível não vira localidade de UF padrão.
 *
 * O `ativo` ausente é lido como **`true`**, e essa é a assimetria certa: um documento gravado antes do
 * delete lógico existir é uma localidade em uso, não uma apagada. Fail-*open* aqui porque o campo governa
 * visibilidade, não permissão — o contrário esconderia dado bom.
 */
fun DocumentoBruto.toLocalidade(): Localidade? {
    val uf = Uf.de(texto("uf")) ?: return null

    return Localidade(
        id = id,
        municipio = texto("municipio"),
        uf = uf,
        codigoIbge = texto("codigoIbge"),
        ativo = booleano("ativo", padrao = true),
    )
}

/** Domínio → `Map`. O `id` não entra: ele é o nome do documento. */
fun Localidade.paraMapa(): Map<String, Any?> = mapOf(
    "municipio" to municipio,
    "uf" to uf.sigla,
    "codigoIbge" to codigoIbge,
    "ativo" to ativo,
)