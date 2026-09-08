package dev.matheus.fluviapp.domain.operacoes

/**
 * Os **dois contextos do logado**, juntos (ADR-0015 §8.1): quem acessa ([usuario]) e quem é a pessoa na
 * operação ([funcionario]). Existe porque a pergunta "o que este usuário pode fazer aqui" passou a
 * precisar dos dois lados — e sem um lugar só para respondê-la, cada ViewModel refazia o caminho
 * `usuário → funcionarioId → funcionário` com uma variação ligeiramente diferente.
 *
 * [funcionario] é `null` para papel puro de plataforma (`ADM`/`GESTOR` não têm registro na operação), e
 * isso é estado **válido**: ali quem decide é o papel.
 */
data class ContextoUsuario(
    val usuario: Usuario,
    val funcionario: Funcionario?,
    /**
     * A empresa **escolhida** por quem opera (F6.4), lida de onde ela foi guardada. `null` = ainda não
     * escolheu — e continua sendo `null` para quem não tem o que escolher.
     *
     * É preferência, não credencial: quem decide se ela vale é [vinculoAtivo], revalidando-a contra os
     * vínculos atuais a cada leitura.
     */
    val empresaAtivaId: String? = null,
    /**
     * O **nome** da empresa do vínculo em vigor, resolvido por quem monta o contexto.
     *
     * Ele existe porque há um lugar em que o id não serve: o **bilhete**, que é lido por gente. Resolver
     * a empresa aqui, uma vez, é o que evita cada tela que imprime alguma coisa ter de fazer a mesma
     * consulta — e é a mesma razão que fez esta classe existir (ADR-0015 §8.1).
     */
    val empresaAtivaNome: String = "",
) {
    val papel: String get() = usuario.papel

    /**
     * **O vínculo em vigor** (ADR-0016 §6): em nome de qual empresa esta pessoa está operando agora.
     *
     * Um vínculo só, é ele. Vários, é o escolhido — e `null` enquanto a escolha não existir ou não valer
     * mais, porque adivinhar seria decidir em nome de quem opera, e o efeito apareceria no recorte das
     * listas e na agência do bilhete.
     */
    val vinculoAtivo: Vinculo? get() = resolverVinculoAtivo(vinculos, empresaAtivaId)

    /** Os vínculos desta pessoa; vazio para papel puro de plataforma. */
    val vinculos: List<Vinculo> get() = funcionario?.vinculos.orEmpty()

    /**
     * A pergunta que a entrada precisa fazer: **mais de uma opção e nenhuma escolha em vigor**. Quem tem
     * zero ou uma nunca cai aqui.
     */
    val precisaEscolherVinculo: Boolean get() = precisaEscolherVinculo(vinculos, empresaAtivaId)

    /**
     * O cargo em vigor — o **do vínculo ativo** desde a F6.5, e não mais um campo do funcionário.
     *
     * A diferença aparece em quem serve a duas empresas: a mesma pessoa é supervisora numa e agente na
     * outra, e o que ela pode fazer depende de em nome de quem está operando. `null` quando não há
     * vínculo em vigor — e a política trata ausência como caso normal (§8.2), fail-closed.
     */
    val cargo: String? get() = vinculoAtivo?.cargo?.name

    /**
     * **Em que atuação esta pessoa trabalha** (ADR-0016 §2, ADR-0020 F4) — o que decide qual família de
     * seções o painel oferece.
     *
     * **Pergunta à política** ([ADR-0032] D1), e não deriva por conta própria. Até 2026-09-07 esta linha
     * era `Funcionario.Cargo.de(cargo)?.atuacao`, o mesmo cálculo que `atuacaoEmVigor` faz — que existia
     * exatamente para isto e **não tinha um único chamador**, com o KDoc dizendo que deveria substituir
     * esta derivação. A substituição documentada só não tinha acontecido.
     *
     * A troca também apaga uma volta inútil: `cargo` já é `vinculoAtivo?.cargo?.name`, então a expressão
     * antiga convertia o enum em texto e o texto de volta em enum. Uma ida e volta pelo `String` só pode
     * perder informação — nunca ganhar.
     *
     * `null` para papel puro de plataforma — e isso **não é ausência de informação**, é a informação:
     * `ADM`/`GESTOR` não atuam num segmento, administram a plataforma inteira. Quem lê isto é
     * `PermissoesUsuario.secoesVisiveis`, que trata os dois casos.
     */
    val atuacao: Atuacao? get() = PermissoesUsuario.atuacaoEmVigor(vinculoAtivo)

    /**
     * A **agência de quem opera**, como ela aparece para gente: o nome da empresa do vínculo ativo.
     *
     * O nome do campo continua sendo "agência" porque é a palavra do bilhete e da operação — no modelo
     * ela é a empresa na atuação de agenciamento (ADR-0016 §4), e é justamente por isso que o §4 existe.
     * Vazia sem vínculo em vigor, e aí não há recorte por agência a aplicar.
     */
    val agencia: String get() = if (vinculoAtivo != null) empresaAtivaNome else ""

    /** Nome da pessoa; sem funcionário, o `username` — o `Usuario` não tem nome (§8.1). */
    val nomeExibicao: String
        get() = funcionario?.descricaoNome?.takeIf { it.isNotBlank() } ?: usuario.username
}