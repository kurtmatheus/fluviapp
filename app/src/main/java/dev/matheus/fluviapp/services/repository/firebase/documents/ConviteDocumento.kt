package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.domain.operacoes.Convite
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto

/**
 * A forma do documento em `convites/{email}` — **documentação, não caminho** (ADR-0019 D2).
 *
 * O **id é o e-mail**, e isso não é atalho: é o que permite ao primeiro acesso achar o convite com um
 * `get` direto, antes de existir `users/{uid}` e sem consultar coleção. Também é o que impede dois
 * convites para a mesma pessoa — unicidade por construção, sem regra de servidor para isso.
 */
data class ConviteDocumento(
    /** Nome da pessoa — é dele que nasce o `Funcionario` quando o convite é de operador. */
    val nome: String = "",
    val papel: String = "",
    /** Só para operador — vazio para papel de plataforma. */
    val empresaId: String = "",
    /** Só para operador — vazio para papel de plataforma. */
    val cargo: String = "",
    /** `true` depois do primeiro acesso: o convite vira registro em vez de sumir. */
    val usado: Boolean = false,
)

/**
 * `DocumentoBruto` → domínio. Devolve `null` quando **não é um convite**: papel ilegível não vira convite
 * de papel padrão, porque um convite é exatamente o que concede papel (fail-closed, ADR-0010).
 */
fun DocumentoBruto.toConvite(): Convite? = Convite.de(
    // O id É o e-mail (o documento não repete o campo — dado repetido é dado que pode discordar).
    email = id,
    nome = texto("nome"),
    papel = texto("papel"),
    empresaId = texto("empresaId"),
    cargo = texto("cargo"),
    usado = booleano("usado"),
)

/** Domínio → `Map`. O e-mail não entra: ele é o nome do documento. */
fun Convite.paraMapa(): Map<String, Any?> = mapOf(
    "nome" to nome,
    "papel" to papel.name,
    "empresaId" to empresaId,
    "cargo" to cargo?.name.orEmpty(),
    "usado" to usado,
)