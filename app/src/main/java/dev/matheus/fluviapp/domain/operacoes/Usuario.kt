package dev.matheus.fluviapp.domain.operacoes

/**
 * Quem **acessa o app** — contexto de SISTEMA (ADR-0015 §8.1). Responde *o que compete no aplicativo*,
 * e só isso: identidade de acesso ([email]/[username]), [papel] e o elo com a operação
 * ([funcionarioId]).
 *
 * O que **não** mora aqui, e por quê: `nome` (é a pessoa, logo é do [Funcionario]), `agencia`/`lotacao`
 * (são onde a pessoa atua, logo também são do [Funcionario] — §8.1) e o `cargo` de negócio (§8.2). Este
 * documento não sabe nada sobre a operação; sabe quem entrou.
 *
 * ### O que saiu daqui em 2026-09-07
 *
 * As anotações `@Entity`/`@PrimaryKey`/`@Index` — esta era **a última classe de domínio que declarava a
 * própria persistência** (ADR-0017 F6). E com elas foi embora o campo `ultimoUsuarioLogado`, que não era
 * do domínio coisa nenhuma: era uma coluna booleana para dizer qual das linhas da tabela valia. Guardado
 * o logado num slot só (`SessaoLocal`), **o último é o único**, e a pergunta que a coluna respondia deixa
 * de existir.
 */
data class Usuario(
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
    /**
     * **Se o acesso está ligado** ([ADR-0032] D6). Nasce `true`, e é o `ADM` quem desliga e religa.
     *
     * Mora aqui, e não numa coleção à parte, por um argumento de custo (Q1): a regra do servidor já faz
     * `get(users/{uid})` em **toda** autorização, para ler o papel — então este campo viaja num documento
     * que ela tem em mãos, e a verificação sai de graça. Guardá-lo fora cobraria um salto a mais em toda
     * operação autorizada do app, para sempre.
     */
    val ativo: Boolean = true,
    /**
     * **Quando o acesso deixa de valer**, em millis de época; `null` = sem prazo (ADR-0032 D6/Q1).
     *
     * É instante, e não data, porque quem o lê é uma regra do Firestore: ela compara `request.time`, e não
     * sabe formatar uma data. A tela escolhe um dia e a fronteira o converte — a decisão de *qual* instante
     * daquele dia é de quem monta o formulário, não deste campo.
     *
     * **Expirar impede a próxima operação; não derruba a sessão em curso** (Q1). É o que a regra dá
     * naturalmente, e a propriedade vale nomear: quem está no meio de um atendimento termina o
     * atendimento. Derrubar a sessão no relógio seria interromper uma emissão pela metade para provar
     * pontualidade.
     */
    val expiraEm: Long? = null,
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