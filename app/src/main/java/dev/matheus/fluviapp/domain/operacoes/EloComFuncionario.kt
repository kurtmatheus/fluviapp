package dev.matheus.fluviapp.domain.operacoes

/**
 * **O elo entre quem acessa e quem opera** (ADR-0015 §8.3, [ADR-0032] D5/Q1) — e a regra que a linguagem do
 * servidor não consegue escrever.
 *
 * `Usuario.funcionarioId` é **1-1**: um perfil aponta para um funcionário, e um funcionário responde por um
 * perfil. É desse elo que sai a posse das passagens (§8.4), então dois perfis apontando para o mesmo
 * funcionário seriam duas pessoas donas das mesmas passagens — e nenhuma das duas saberia.
 *
 * ### Por que a unicidade mora aqui, e não na regra
 *
 * Porque **regra não consulta coleção**. Para saber se algum outro `users/{uid}` já usa aquele
 * `funcionarioId`, a regra teria de varrer a coleção, e a linguagem não varre — a mesma limitação que
 * deixou a unicidade de rota e de viagem fora do servidor (ADR-0011). Ali a saída foi a mesma: **o cadastro
 * impede o acidente, não a corrida**.
 *
 * É uma assimetria declarada, não um esquecimento: o servidor garante *quem pode escrever o elo*, o app
 * garante *que o elo não colide*. Duas corridas simultâneas no mesmo funcionário passariam — e o custo
 * disso é conhecido e pequeno, porque quem escreve é uma pessoa só, o `ADM`, numa tela de gestão.
 */

/**
 * Os funcionários que podem receber o elo: **os que nenhum perfil já usa**, mais o do próprio perfil.
 *
 * O segundo termo é o que faz a lista servir para exibir, e não só para escolher: sem ele, abrir a tela de
 * um perfil já ligado mostraria o campo vazio, como se o elo não existisse.
 *
 * Ordena por nome porque a lista é lida por gente, e é o único critério que não muda quando alguém entra.
 */
fun funcionariosElegiveis(
    funcionarios: List<Funcionario>,
    usuarios: List<Usuario>,
    eloAtual: String = "",
): List<Funcionario> {
    val jaLigados = usuarios
        .map { it.funcionarioId }
        .filter { it.isNotBlank() && it != eloAtual }
        .toSet()

    return funcionarios
        .filterNot { it.id in jaLigados }
        .sortedBy { it.descricaoNome.lowercase() }
}
