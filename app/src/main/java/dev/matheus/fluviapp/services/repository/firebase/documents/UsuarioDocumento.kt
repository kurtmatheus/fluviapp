package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.domain.operacoes.Usuario
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto
import dev.matheus.fluviapp.services.repository.firebase.autenticacao.PerfilAutenticado

/**
 * A forma do documento em `users/{id}` — **documentação, não caminho** (ADR-0019 D2), desde 2026-09-07.
 *
 * Até então esta data class era o caminho de verdade, nas duas direções: o login lia por
 * `toObject(UsuarioDocumento::class.java)` e o primeiro acesso gravava por `.set(UsuarioDocumento(...))`.
 * Eram as **últimas leituras e escritas por reflexão** do app, e a reflexão é justamente o que o R8 não
 * enxerga — ele renomeia o campo, e o Firestore, que casa por nome, passa a devolver (ou gravar) vazio
 * sem erro nenhum. Ambas viraram `Map`, como o resto da casa.
 */
data class UsuarioDocumento(
    val email: String = "",
    /** Credencial alternativa ao e-mail (ADR-0015 §8.1) — o nome da pessoa é do `Funcionario`. */
    val username: String = "",
    /** Papel de sistema (`ADM`/`GESTOR`/`OPERADOR`). Antes se chamava `cargo`, que agora é do negócio. */
    val papel: String = "",
    /** Elo 1-1 com `funcionarios/{id}` (ADR-0015 §8.3). Vazio em papel puro de plataforma. */
    val funcionarioId: String = ""
)

/**
 * `DocumentoBruto` → [PerfilAutenticado], **sem passar pelo DTO** (ADR-0019 D2) e sem tocar no Firebase.
 *
 * Ela existe por causa do teste, e a razão vale registro: trocar o `toObject` por leitura de `Map` fez a
 * conversão nascer **dentro** do repositório de autenticação, que precisa do Firebase para ser
 * instanciado — e o caso que guardava a assimetria do papel (desconhecido atravessa cru, ADR-0010) ficou
 * sem onde morar. Extraída, ela é função pura: entra mapa, sai perfil.
 *
 * O [funcionario] é o segundo salto (`funcionarios/{id}`), e é `null` para papel puro de plataforma —
 * estado válido, em que cargo e nome ficam vazios porque quem decide ali é o papel.
 */
fun DocumentoBruto.toPerfilAutenticado(uid: String, funcionario: DocumentoBruto?) = PerfilAutenticado(
    id = uid,
    email = texto("email"),
    username = texto("username"),
    // Sem default: papel desconhecido/ausente tem que virar "sem permissão" na política (fail-closed,
    // ADR-0010) — não um valor de conveniência. Contraste deliberado com o `cargo` do funcionário, que
    // ausente cai no menor privilégio.
    papel = texto("papel"),
    funcionarioId = texto("funcionarioId"),
    cargo = funcionario?.texto("cargo").orEmpty(),
    nome = funcionario?.texto("nome").orEmpty(),
)

/**
 * Domínio → `Map`, que é o que o Firestore grava. O `id` **não entra**: ele é o nome do documento (o
 * `uid` do Auth), não um campo dele.
 *
 * Sem default em `papel`: papel ausente ou desconhecido tem de virar "sem permissão" na política
 * (fail-closed, ADR-0010), e não um valor de conveniência.
 */
fun Usuario.paraMapa(): Map<String, Any?> = mapOf(
    "email" to email,
    "username" to username,
    "papel" to papel,
    "funcionarioId" to funcionarioId,
)