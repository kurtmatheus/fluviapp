package dev.matheus.fluviapp.domain.operacoes

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Quem **acessa o app** — contexto de SISTEMA (ADR-0015 §8.1). Responde *o que compete no aplicativo*,
 * e só isso: identidade de acesso ([email]/[username]), [papel] e o elo com a operação
 * ([funcionarioId]).
 *
 * O que **não** mora aqui, e por quê: `nome` (é a pessoa, logo é do [Funcionario]), `agencia`/`lotacao`
 * (são onde a pessoa atua, logo também são do [Funcionario] — §8.1) e o `cargo` de negócio (§8.2). Este
 * documento não sabe nada sobre a operação; sabe quem entrou.
 */
@Entity(indices = [Index("id")])
data class Usuario(
    @PrimaryKey
    val id: String,
    val email: String,
    /**
     * Credencial, não identidade civil: alternativa ao e-mail no login (ADR-0015 §8.1). Único por
     * natureza — o Firebase Auth só loga por e-mail, então entrar por `username` é resolver
     * `username → e-mail` antes do `signIn`, e isso exige unicidade.
     */
    val username: String,
    /** Papel de **sistema** ([Papel]) persistido como String — a política converte na fronteira. */
    val papel: String,
    /**
     * Elo 1-1 com o [Funcionario] (ADR-0015 §8.3). Vazio para papel puro de plataforma: `ADM`/`GESTOR`
     * existem sem registro na operação — e, por isso mesmo, não emitem passagem (§8.4).
     */
    val funcionarioId: String = "",
    val ultimoUsuarioLogado: Boolean = false
) {
    /**
     * O eixo **fechado** da autorização (ADR-0015, revisão estrutural): três papéis, e a tendência é
     * continuar três. Quem cresce é o [Funcionario.Cargo], no ritmo da operação.
     *
     *
     * [OPERADOR] é o **coringa e o elo**: todo usuário que não é [ADM]/[GESTOR] é `OPERADOR`, e é ele
     * que corresponde a um funcionário.
     */
    enum class Papel {
        ADM,
        GESTOR,
        OPERADOR;

        companion object {
            /** Converte o papel persistido (String) no enum canônico; null se desconhecido. */
            fun de(valor: String?): Papel? = entries.firstOrNull { it.name == valor }
        }
    }

    companion object {
        const val GERAL = "Geral"
    }
}