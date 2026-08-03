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
) {
    val papel: String get() = usuario.papel

    /** `null` quando não há vínculo — a política trata ausência como caso normal (§8.2). */
    val cargo: String? get() = funcionario?.cargo

    /**
     * **Em que atuação esta pessoa trabalha** (ADR-0016 §2, ADR-0020 F4) — o que decide qual família de
     * seções o painel oferece.
     *
     * Vem do cargo, que já a declara (`Funcionario.Cargo.atuacao`): enquanto o vínculo
     * `(empresa, atuação)` não existir como dado, o cargo é a fonte exata, porque o conjunto de cargos de
     * cada atuação é disjunto.
     *
     * `null` para papel puro de plataforma — e isso **não é ausência de informação**, é a informação:
     * `ADM`/`GESTOR` não atuam num segmento, administram a plataforma inteira. Quem lê isto é
     * `PermissoesUsuario.secoesVisiveis`, que trata os dois casos.
     */
    val atuacao: Atuacao? get() = Funcionario.Cargo.de(cargo)?.atuacao

    /** Agência de quem opera; vazia sem vínculo (e aí não há recorte por agência a aplicar). */
    val agencia: String get() = funcionario?.agencia.orEmpty()

    /** Nome da pessoa; sem funcionário, o `username` — o `Usuario` não tem nome (§8.1). */
    val nomeExibicao: String
        get() = funcionario?.descricaoNome?.takeIf { it.isNotBlank() } ?: usuario.username
}