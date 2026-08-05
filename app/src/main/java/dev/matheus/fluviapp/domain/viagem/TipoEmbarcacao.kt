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
 * O tipo diz **o que** cabe; a capacidade do embarcacao diz **quanto** (ADR-0018 D8) — os dois são
 * complementares e não se substituem.
 *
 * Ponto de aplicação: a **emissão**. Escolhida a viagem sabe-se a embarcação, e o form não oferece o
 * veículo que ela não leva — o erro morre na origem, em vez de virar validação depois.
 *
 * > O rename `Embarcacao` → `Embarcacao` está decidido (ADR-0020 D4) e adiado para quando a estrutura de
 * > embarcações for mexida. Este tipo já nasce com o nome certo.
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
    EMBARCACAO(
        rotulo = "Embarcacao",
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
    }
}