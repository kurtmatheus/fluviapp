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
 * coleção passa a existir só no Firestore, com o `StateFlow` do listener como fonte reativa. E a saída do
 * Room não é só arrumação — é o que **destrava a forma nova**: [vinculos] é uma lista, e lista em tabela
 * exigiria `TypeConverter` e migração para um formato que muda de novo na fatia seguinte.
 *
 * ### Os vínculos, e o que sobrou
 *
 * [vinculos] é a forma do ADR-0016 §6, e desde a F6.3 é **a fonte**: a pessoa serve uma ou mais empresas,
 * com cargo em cada uma.
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
     * No aplicativo ninguém mais o lê: o cargo em vigor é o do **vínculo ativo**
     * ([ContextoUsuario.cargo]), porque a mesma pessoa pode ser supervisora numa empresa e agente em
     * outra. Aqui ele é escrito derivado do primeiro vínculo.
     */
    val cargo: String = Cargo.AGENTE.name,
    /**
     * **A chave que liga o pré-cadastro à pessoa** (ADR-0015 §2.1): no primeiro acesso ainda não existe
     * `users/{uid}`, então é o e-mail que casa a conta do Auth com este registro. Depois disso o elo
     * permanente é o id ([Usuario.funcionarioId], §8.3) — o e-mail é chave de **descoberta**, uma vez só.
     */
    val email: String = "",
    /** Onde a pessoa atua e como, um por empresa (ADR-0016 §6). Ver [Vinculo]. */
    val vinculos: List<Vinculo> = emptyList(),
) : IObjetoSimplificado {

    /**
     * As empresas em que atua — **derivado**, nunca campo desta classe.
     *
     * No documento ele existe como array chato, e é denormalização deliberada: o Firestore não consulta
     * campo de dentro de elemento de array, então *"quem trabalha na empresa X"* não sai de [vinculos].
     * Em memória não há essa limitação, e manter as duas verdades é que seria o erro.
     */
    val empresaIds: List<String> get() = vinculos.empresaIds

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