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
    val funcionarioId: String = "",
    /** Acesso ligado (ADR-0032 D6). Ausente = **ligado**: documento anterior à decisão é registro em uso. */
    val ativo: Boolean = true,
    /**
     * Quando o acesso deixa de valer, em millis de época; **`0` = sem prazo** (ADR-0032 Q1).
     *
     * Nunca `null`, e a razão é do servidor: a regra compara o valor com `request.time`, e um `null` no
     * meio da comparação derruba a avaliação inteira — o que, dependendo do lado em que a regra falha,
     * trancaria quem tem acesso ou liberaria quem não tem. Zero é comparável e diz a mesma coisa.
     */
    val expiraEm: Long = 0L,
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
    "ativo" to ativo,
    // `null` do domínio ("sem prazo") vira **zero** na fronteira, e não campo ausente nem nulo: é o que a
    // regra do servidor sabe comparar (ADR-0032 Q1).
    "expiraEm" to (expiraEm ?: 0L),
)

/**
 * `DocumentoBruto` → [Usuario]: o caminho que a **gestão de acesso** usa (ADR-0032 D6), e não o login.
 *
 * A distinção não é detalhe: o login lê o perfil de **quem entrou** e o traduz em `PerfilAutenticado`,
 * juntando o segundo salto (`funcionarios/{id}`); aqui se lê o registro de **outra pessoa**, para
 * administrá-lo — sem cargo, sem nome, sem salto nenhum.
 *
 * `ativo` ausente é **ligado** (documento anterior à decisão é registro em uso) e `expiraEm` zero volta a
 * ser `null`, que é como o domínio diz "sem prazo".
 *
 * **Antecipação declarada**: quem a consome é a seção Usuários deixando de ser somente-leitura, que é a
 * fatia seguinte. Ela nasce nesta porque a convenção de leitura dos dois campos é decisão desta — e uma
 * convenção sem teste é uma convenção que a próxima fatia reinventa.
 */
fun DocumentoBruto.toUsuario(): Usuario = Usuario(
    id = id,
    email = texto("email"),
    username = texto("username"),
    papel = texto("papel"),
    funcionarioId = texto("funcionarioId"),
    ativo = booleano("ativo", padrao = true),
    expiraEm = instante("expiraEm").takeIf { it > 0L },
)