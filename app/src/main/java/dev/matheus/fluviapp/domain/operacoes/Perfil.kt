package dev.matheus.fluviapp.domain.operacoes

/**
 * **Qual dos dois perfis está em vigor** ([ADR-0032] D5).
 *
 * Uma pessoa tem no máximo dois: um com a **plataforma** (o [Usuario.papel]) e, se precisar, um com uma
 * **empresa** (o [Vinculo] do funcionário) — ou o inverso. Quem tem os dois troca por opção no menu, e a
 * capacidade é restrita a `ADM` e `GESTOR`.
 *
 * ### Isto é lente, não redução de poder
 *
 * A troca **não** tira permissão de ninguém, e é importante que não pareça tirar: com dois perfis no mesmo
 * uid, o servidor lê os dois — papel de `users/{uid}`, cargo de `funcionarios/{funcionarioId}` — e
 * **concede a união**, sem saber que existe troca alguma. O que muda no app é *qual família de seções o
 * painel oferece*.
 *
 * É por isso que a capacidade é de `ADM`/`GESTOR` e de mais ninguém: prometer a um `OPERADOR` uma
 * separação que o servidor não faz seria mentir sobre segurança. Para quem administra a plataforma, a
 * promessa é outra e verdadeira — *estou olhando o app pelo lado da empresa agora*.
 *
 * ### O que ele substituiu
 *
 * A escolha **entre empresas** (ADR-0016 §6, F6.4), que a D5 descartou junto com o caso de servir a duas.
 * A forma é a mesma — preferência guardada no aparelho, revalidada a cada leitura, **nunca consultada como
 * autoridade** —, e o eixo é outro: era *em nome de qual empresa opero*, é *qual perfil está ativo*.
 */
enum class Perfil {
    /** Administrar a plataforma: o painel do papel (ADR-0016 §2). */
    PLATAFORMA,

    /** Operar numa empresa: o painel da atuação em que a pessoa tem vínculo. */
    EMPRESA;

    companion object {
        /** Fronteira (String → domínio): valor ilegível não vira perfil, devolve `null`. */
        fun de(valor: String?): Perfil? = entries.firstOrNull { it.name == valor }
    }
}

/**
 * **Qual perfil vale agora** — função pura, e é ela que os testes cobrem sem DataStore nem tela.
 *
 * | papel | vínculo | escolha | resultado |
 * |---|---|---|---|
 * | de operação (ou ausente) | qualquer | qualquer | `EMPRESA` — não há perfil de plataforma a ativar |
 * | de plataforma | ausente | qualquer | `PLATAFORMA` — não há segundo perfil, então não há escolha |
 * | de plataforma | presente | ausente | `PLATAFORMA` — o padrão de quem administra é administrar |
 * | de plataforma | presente | válida | a escolhida |
 *
 * As duas primeiras linhas são o que faz da preferência uma preferência: **a escolha só é consultada de
 * quem tem os dois perfis**. Um `EMPRESA` guardado no aparelho de quem perdeu o vínculo simplesmente
 * deixa de casar — e isso é o mesmo mecanismo que a escolha entre empresas usava, pela mesma razão:
 * dado que não é autoridade não precisa de invalidação ativa.
 */
fun resolverPerfilAtivo(papel: String?, vinculo: Vinculo?, escolha: Perfil?): Perfil = when {
    !PermissoesUsuario.ehPapelPlataforma(papel) -> Perfil.EMPRESA
    vinculo == null -> Perfil.PLATAFORMA
    else -> escolha ?: Perfil.PLATAFORMA
}
