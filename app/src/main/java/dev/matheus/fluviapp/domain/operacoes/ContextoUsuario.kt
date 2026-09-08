package dev.matheus.fluviapp.domain.operacoes

/**
 * Os **dois contextos do logado**, juntos (ADR-0015 §8.1): quem acessa ([usuario]) e quem é a pessoa na
 * operação ([funcionario]). Existe porque a pergunta "o que este usuário pode fazer aqui" passou a
 * precisar dos dois lados — e sem um lugar só para respondê-la, cada ViewModel refazia o caminho
 * `usuário → funcionarioId → funcionário` com uma variação ligeiramente diferente.
 *
 * [funcionario] é `null` para papel puro de plataforma (`ADM`/`GESTOR` não têm registro na operação), e
 * isso é estado **válido**: ali quem decide é o papel.
 *
 * Desde a [ADR-0032] D5 os dois lados podem existir na mesma pessoa, e aí ela tem **dois perfis**. O
 * contexto passa a dizer por qual deles se está olhando ([perfilAtivo]) — e a lente aparece num ponto só,
 * [vinculoAtivo], que é de onde saem atuação, agência e escopo.
 */
data class ContextoUsuario(
    val usuario: Usuario,
    val funcionario: Funcionario?,
    /**
     * O perfil **escolhido** por quem opera ([ADR-0032] D5), lido de onde ele foi guardado. `null` = ainda
     * não escolheu — e continua sendo `null` para quem não tem o que escolher.
     *
     * É preferência, não credencial: quem decide se ela vale é [perfilAtivo], revalidando-a contra papel e
     * vínculo a cada leitura.
     */
    val perfilEscolhido: Perfil? = null,
    /**
     * O **nome** da empresa do vínculo, resolvido por quem monta o contexto.
     *
     * Ele existe porque há um lugar em que o id não serve: o **bilhete**, que é lido por gente. Resolver
     * a empresa aqui, uma vez, é o que evita cada tela que imprime alguma coisa ter de fazer a mesma
     * consulta — e é a mesma razão que fez esta classe existir (ADR-0015 §8.1).
     *
     * Chamava-se `empresaAtivaNome` e é **do vínculo, não da lente** ([ADR-0032] D5): o menu precisa dele
     * para dizer *"operar como Navegação Norte"* justamente enquanto o perfil de empresa **não** está
     * ativo. Quem imprime bilhete usa [agencia], que é a versão com a lente aplicada — e o nome novo
     * existe para que essa diferença não dependa de ninguém lembrar dela.
     */
    val empresaDoVinculo: String = "",
) {
    val papel: String get() = usuario.papel

    /**
     * **Qual dos dois perfis está em vigor** ([ADR-0032] D5) — plataforma ou empresa. Ver [Perfil].
     *
     * Pergunta ao domínio (`resolverPerfilAtivo`) em vez de comparar aqui: a regra é a mesma que decide se
     * a preferência guardada ainda vale, e ela tem de existir num lugar só.
     */
    val perfilAtivo: Perfil get() = resolverPerfilAtivo(papel, funcionario?.vinculo, perfilEscolhido)

    /** Se a opção de trocar de perfil aparece no menu — quem tem os dois, e só `ADM`/`GESTOR`. */
    val podeTrocarPerfil: Boolean get() = PermissoesUsuario.podeTrocarPerfil(papel, funcionario?.vinculo)

    /**
     * **O vínculo em vigor** (ADR-0016 §6): em nome de qual empresa esta pessoa está operando.
     *
     * Duas coisas o decidem, e nenhuma delas é uma escolha entre empresas. A primeira é o cadastro: desde
     * a [ADR-0032] Q2 há um vínculo no máximo, e `resolverVinculoAtivo(vinculos, empresaAtivaId)` — uma
     * função inteira para escolher entre vários — perdeu o assunto. A segunda é a **lente** (D5): sob o
     * perfil de plataforma o vínculo existe e **não está em vigor**, e é daí que o painel do `ADM` volta a
     * ser o da plataforma sem que ele perca nada.
     *
     * `null`, então, tem dois significados que se comportam igual: *não tem empresa* e *não está olhando
     * pela empresa*. Todo leitor daqui — atuação, agência, escopo — já tratava o primeiro.
     */
    val vinculoAtivo: Vinculo? get() = funcionario?.vinculo?.takeIf { perfilAtivo == Perfil.EMPRESA }

    /**
     * O cargo em vigor — o **do vínculo ativo** desde a F6.5, e não mais um campo do funcionário.
     *
     * A diferença continua valendo depois da [ADR-0032] Q2: o campo `Funcionario.cargo` é legado com um
     * leitor só (a regra de *passagem* no servidor), e quem responde *o que esta pessoa pode fazer* é o
     * cargo do vínculo. `null` quando não há vínculo — e a política trata ausência como caso normal
     * (§8.2), fail-closed.
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
     * Vazia sem vínculo **em vigor** — o que inclui quem tem vínculo e está sob o perfil de plataforma
     * ([ADR-0032] D5). É a leitura certa: ninguém emite bilhete em nome de uma agência pela qual não está
     * olhando. Quem precisa do nome fora da lente lê [empresaDoVinculo], e é uma pergunta só do menu.
     */
    val agencia: String get() = if (vinculoAtivo != null) empresaDoVinculo else ""

    /** Nome da pessoa; sem funcionário, o `username` — o `Usuario` não tem nome (§8.1). */
    val nomeExibicao: String
        get() = funcionario?.descricaoNome?.takeIf { it.isNotBlank() } ?: usuario.username
}