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
 * ### Os vínculos, e o que ainda não morreu
 *
 * [vinculos] é a forma alvo (ADR-0016 §6): a pessoa serve uma ou mais empresas, com cargo em cada uma.
 * Ele entra **aditivo** e vazio por padrão, ao lado dos campos que ele vai substituir — [agencia],
 * [lotacao] e [cargo] —, e essa convivência é deliberada: são 96 leituras espalhadas por 28 arquivos, boa
 * parte delas em Passagem, que é código não revitalizado (F9). Trocar tudo de uma vez seria reescrever a
 * emissão junto com o cadastro.
 *
 * A ordem é a mesma que o ADR-0008 usou para relacionar por id: **acrescenta, migra os leitores, remove**.
 * Quem remove é a F6.3 (cadastro) e a F6.5 (o mínimo da Passagem).
 */
data class Funcionario(
    override val id: String,
    override val descricaoNome: String,
    /** **Legado** — vira o `empresaId` do vínculo (F6.3). */
    val agencia: String,
    /** **Legado** — morre com `Funcionario.Lotacao` (F6.3): lotação não é dimensão do modelo novo. */
    var lotacao: String,
    /**
     * Cargo de **negócio** (§8.1) — nasce [Cargo.AGENTE], o menor privilégio. Promover é decisão da
     * gestão (ADR-0015 §8.5), nunca do próprio.
     *
     * **Legado**: o cargo passa a ser por vínculo (§6.1), porque a mesma pessoa pode ter cargos
     * diferentes em empresas diferentes — e um campo só não tem como dizer isso.
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

    /** **Legado** — morre na F6.3 (ADR-0016 §6): a lotação some junto com a agência como String. */
    enum class Lotacao {
        PORTO_NORTE,
        ILHA_CENTRAL,
        PORTO_SUL
    }
}