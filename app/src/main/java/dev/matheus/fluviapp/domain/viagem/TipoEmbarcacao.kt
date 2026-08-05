package dev.matheus.fluviapp.domain.viagem

import dev.matheus.fluviapp.domain.passagem.ClasseVeiculo

/**
 * Tipo da embarcação como tipo de domínio ([ADR-0020 D4]), no lugar da categoria de catálogo
 * `TIPO_EMBARCACAO` que o [ADR-0016] §8 havia previsto.
 *
 * O §8 chamava esta categoria de *"a exceção nomeada"* — catálogo **e** comportamento — e a resolvia com
 * *"o catálogo guarda a lista, a capacidade é código"*, assumindo que um valor novo **nasce inerte** (só
 * passageiro, até o código dizer). Essa resolução era o argumento contra si mesma: **se o código precisa
 * falar antes de o valor significar alguma coisa, a lista não é a fonte — o tipo é.** Na formulação do
 * analista: *não se vende veículo para uma lancha se a cadastrarmos.*
 *
 * O tipo diz **o que** cabe; a capacidade da embarcação diz **quanto** (ADR-0018 D8) — os dois são
 * complementares e não se substituem.
 *
 * ### Onde se aplica
 *
 * Desde que virou campo de [Embarcacao], o tipo age **duas vezes**. Primeiro no **cadastro**: escolhida a
 * lancha, o formulário não pergunta capacidade de veículo — a contradição *"lancha com doze vagas de
 * carro"* não chega a nascer. Depois na **emissão** (ADR-0016 §8): sabida a viagem, sabe-se a embarcação,
 * e o form não oferece o veículo que ela não leva. O erro morre na origem, em vez de virar validação
 * depois.
 *
 * > **Gênero e espécie.** `Embarcacao` é a entidade — o gênero; `NAVIO` é um dos valores deste enum — uma
 * > espécie. Foi essa distinção que motivou o rename de `Navio` para `Embarcacao` (ADR-0020 D4): "o navio
 * > é do tipo lancha" é a frase que denuncia o nome errado.
 */
enum class TipoEmbarcacao(
    val rotulo: String,
    /** Classes de veículo que esta embarcação transporta. Vazio = só passageiro. */
    val classesAdmitidas: Set<ClasseVeiculo>,
) {
    FERRY_BOAT(
        rotulo = "Ferry Boat",
        classesAdmitidas = setOf(
            ClasseVeiculo.CARRO,
            ClasseVeiculo.MOTO,
            ClasseVeiculo.CAMINHAO,
            ClasseVeiculo.CARRETA,
        ),
    ),
    NAVIO(
        rotulo = "Navio",
        classesAdmitidas = setOf(ClasseVeiculo.CARRO, ClasseVeiculo.MOTO),
    ),
    LANCHA(
        rotulo = "Lancha",
        classesAdmitidas = emptySet(),
    );

    /** Regra pura: esta embarcação leva esta classe? */
    fun admite(classe: ClasseVeiculo?): Boolean = classe != null && classe in classesAdmitidas

    /** `false` = embarcação só de passageiro; o modo veículo nem se oferece. */
    val levaVeiculo: Boolean get() = classesAdmitidas.isNotEmpty()

    companion object {
        /** Fronteira String→enum; `null` se desconhecido (fail-closed). Tolerante à grafia legada. */
        fun de(valor: String?): TipoEmbarcacao? {
            val normalizado = valor?.trim()?.uppercase()?.replace(" ", "_") ?: return null
            return entries.firstOrNull { it.name == normalizado }
        }

        /**
         * Fronteira de **tela**: o dropdown mostra o [rotulo] e devolve o texto escolhido, e é aqui que ele
         * volta a ser tipo. Separada de [de] de propósito — aquela lê o que o **Firestore** gravou (o
         * `name`, estável), esta lê o que a **pessoa** escolheu (o rótulo, que pode ser reescrito sem
         * migrar dado). Confundir as duas é atar a persistência ao texto da interface.
         */
        fun porRotulo(rotulo: String?): TipoEmbarcacao? =
            entries.firstOrNull { it.rotulo.equals(rotulo?.trim(), ignoreCase = true) }
    }
}