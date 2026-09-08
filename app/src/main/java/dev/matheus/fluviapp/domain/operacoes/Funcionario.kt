package dev.matheus.fluviapp.domain.operacoes

import dev.matheus.fluviapp.domain.IObjetoSimplificado

/**
 * A pessoa na **operação** — contexto de NEGÓCIO (ADR-0015 §8.1). Era `Agente`: não morreu, mudou de
 * pergunta. O [Usuario] responde *quem acessa o app e o que pode nele*; o `Funcionario` responde *quem é a
 * pessoa na operação*.
 *
 * O [descricaoNome] é o **nome da pessoa**: o `Usuario` não tem nome, tem `username` (credencial). Os dois
 * lados se ligam 1-1 por [Usuario.funcionarioId] (§8.3).
 *
 * ### Fora do Room (F6.2)
 *
 * Esta é a **quinta entidade a perder o espelho** (ADR-0017 D1) e a última do caminho vivo a tê-lo: a
 * coleção passa a existir só no Firestore, com o `StateFlow` do listener como fonte reativa.
 *
 * ### O vínculo, e o que sobrou
 *
 * [vinculo] é a forma do ADR-0016 §6 e desde a F6.3 é **a fonte**: onde a pessoa atua, e com que cargo.
 *
 * Era uma **lista** até a [ADR-0032] D5, que descartou o caso de servir a duas empresas. A Q2 levou a
 * decisão até a estrutura: lista de no máximo um elemento admite o que o domínio não reconhece, e
 * estrutura assim é convite a estado inválido — cada leitor tinha de decidir por conta própria o que
 * fazer com o segundo elemento. Agora não há segundo. É o mesmo princípio que o ADR-0031 aplicou à
 * cilindrada: **quando o fato tem uma forma, a estrutura segue a forma.**
 *
 * A troca seguiu a ordem que o ADR-0008 usou para relacionar por id — **acrescenta, migra os leitores,
 * remove** — e chegou ao fim aqui: `lotacao` saiu na F6.3 (ninguém a lia fora do cadastro), `agencia` saiu
 * na F6.5 (o bilhete passou a tirar a agência do vínculo ativo) e [cargo] ficou, com um leitor só e com
 * dono: a regra de *passagem* no servidor, que a F9 vai reescrever.
 */
data class Funcionario(
    override val id: String,
    override val descricaoNome: String,
    /**
     * Cargo de **negócio** (§8.1) — nasce [Cargo.AGENTE], o menor privilégio.
     *
     * **Legado com um leitor conhecido, e é por isso que ele ainda existe**: a regra do Firestore para
     * *passagem* pergunta "quem edita qualquer uma?" lendo este campo (`cargoDoAutor`), e a linguagem de
     * regras não sabe procurar "algum vínculo com cargo SUPERVISOR" sem saber a empresa. Trocar isso
     * exigiria um campo derivado novo, que a F9 (Passagem) descartaria em seguida — então ele fica, e
     * sai lá.
     *
     * No aplicativo ninguém mais o lê: o cargo em vigor é o do **vínculo** ([ContextoUsuario.cargo]).
     * Aqui ele é escrito derivado dele — e desde a [ADR-0032] Q2 a derivação não tem mais nada de
     * arbitrário, porque não há "primeiro" vínculo a escolher.
     */
    val cargo: String = Cargo.AGENTE.name,
    /**
     * **A chave que liga o pré-cadastro à pessoa** (ADR-0015 §2.1): no primeiro acesso ainda não existe
     * `users/{uid}`, então é o e-mail que casa a conta do Auth com este registro. Depois disso o elo
     * permanente é o id ([Usuario.funcionarioId], §8.3) — o e-mail é chave de **descoberta**, uma vez só.
     */
    val email: String = "",
    /**
     * Onde a pessoa atua e como (ADR-0016 §6, [ADR-0032] D5). Ver [Vinculo].
     *
     * `null` é estado **legítimo**, e há dois casos: o pré-cadastro do §2.1 (a pessoa existe, o vínculo
     * vem depois) e o vínculo ilegível descartado na fronteira. Nos dois, a pessoa não enxerga seção
     * alguma — a política trata ausência como caso normal (ADR-0015 §8.2), fail-closed.
     */
    val vinculo: Vinculo? = null,
) : IObjetoSimplificado {

    /**
     * O eixo **aberto** da autorização (ADR-0015, revisão estrutural): hoje supervisor e agente, amanhã
     * quem faz check-in, quem valida embarque, quem responde por uma embarcação. Cresce sem tocar no
     * [Usuario.Papel], que é fechado por natureza.
     *
     * Sem default na fronteira, de propósito: cargo desconhecido → sem permissão (fail-closed, ADR-0010).
     *
     * **Cada valor declara a sua [Atuacao]** (ADR-0016, 8ª rodada): `SUPERVISOR` e `AGENTE` não são
     * cargos "do sistema" — são cargos **do agenciamento**. O transporte terá os seus, a portuária terá
     * os dela quando acordar, e é por isso que o cargo é o eixo aberto e o papel não.
     *
     * É daqui que sai a atuação de um [Vinculo], e é por isso que o vínculo não guarda a atuação ao lado
     * do cargo: os conjuntos são disjuntos por construção.
     */
    enum class Cargo(val atuacao: Atuacao) {
        SUPERVISOR(Atuacao.AGENCIAMENTO),
        AGENTE(Atuacao.AGENCIAMENTO);

        companion object {
            /** Converte o cargo persistido (String) no enum canônico; null se desconhecido. */
            fun de(valor: String?): Cargo? = entries.firstOrNull { it.name == valor }
        }
    }

}