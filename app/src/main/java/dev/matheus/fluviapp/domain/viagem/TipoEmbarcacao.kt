package dev.matheus.fluviapp.domain.viagem

import dev.matheus.fluviapp.domain.passagem.ClasseVeiculo

/**
 * **O que um casco admite** ([ADR-0031] D4) — e a forma importa tanto quanto o conteúdo.
 *
 * Enumerar classe a classe funcionava com seis. Com dezessete, cada casco novo ou classe nova cobraria uma
 * decisão de pertencimento escrita à mão, e sobre elas o código já confessava não ter critério: *"leitura
 * minha sobre o que cada casco carrega, não decisão registrada"*. Vinte e duas apostas que ninguém confere.
 *
 * Por exclusão, some a lista: a balsa leva **tudo** por definição do que uma balsa é, e o que se enumera é
 * a **exceção** — o navio, que leva carro e moto. É isso que faz **classe nova nascer vendável sem que
 * ninguém decida por ela**, e é o oposto do valor que nasce inerte.
 */
sealed interface CargaAdmitida {

    /** A balsa: leva o que couber, e *o que couber* é a **capacidade** (ADR-0018 D8), não o tipo. */
    data object Todas : CargaAdmitida

    /** Só passageiro. A ausência é da natureza do casco, não configuração. */
    data object Nenhuma : CargaAdmitida

    /** A exceção enumerada — hoje, só o navio. */
    data class Apenas(val classes: Set<ClasseVeiculo>) : CargaAdmitida

    fun admite(classe: ClasseVeiculo): Boolean = when (this) {
        Todas -> true
        Nenhuma -> false
        is Apenas -> classe in classes
    }
}

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
 * ### A admissão mudou de forma em 2026-09-03 (ADR-0031 D4)
 *
 * Era um `Set<ClasseVeiculo>` enumerado; passou a ser [CargaAdmitida], de três estados. O efeito prático
 * está no que **não** se escreve: com as dezessete classes, a forma antiga pediria vinte e duas decisões de
 * pertencimento à mão, e a nova pede **duas** — o conjunto do navio.
 *
 * O preço, assumido: a balsa passa a admitir classe nova **sem decisão humana**. É *fail-open*, e é
 * deliberado — o risco que sobra não é vender o impossível, é vender o que não cabe, e esse eixo tem dono.
 *
 * ### Onde se aplica
 *
 * Desde que virou campo de [Embarcacao], o tipo age **duas vezes**. Primeiro no **cadastro**: escolhida a
 * lancha, o formulário não pergunta capacidade de veículo — a contradição *"lancha com doze vagas de
 * carro"* não chega a nascer. Depois na **emissão** (ADR-0016 §8): sabida a viagem, sabe-se a embarcação,
 * e o form não oferece o veículo que ela não leva.
 *
 * > **A segunda ainda não acontece.** A medição de 2026-09-03 encontrou `admite()` **sem um único chamador
 * > de produção**: a emissão oferece `ClasseVeiculo.entries` cru, e o tipo do casco sequer chega até lá.
 * > É dívida declarada, e o passo que a paga vem em seguida.
 *
 * > **Gênero e espécie.** `Embarcacao` é a entidade — o gênero; `NAVIO` é um dos valores deste enum — uma
 * > espécie. Foi essa distinção que motivou o rename de `Navio` para `Embarcacao` (ADR-0020 D4): "o navio
 * > é do tipo lancha" é a frase que denuncia o nome errado. E `LANCHA` aqui é **casco**, não a
 * > `ClasseVeiculo.LANCHA`, que é a embarcação transportada sobre carretilha (ADR-0031 D5).
 */
enum class TipoEmbarcacao(
    val rotulo: String,
    val cargaAdmitida: CargaAdmitida,
) {
    FERRY_BOAT(rotulo = "Ferry Boat", cargaAdmitida = CargaAdmitida.Todas),

    // A única exceção enumerada — e ela encolheu com o D4: eram carro, moto, van e SUV.
    NAVIO(
        rotulo = "Navio",
        cargaAdmitida = CargaAdmitida.Apenas(setOf(ClasseVeiculo.CARRO, ClasseVeiculo.MOTO)),
    ),

    LANCHA(rotulo = "Lancha", cargaAdmitida = CargaAdmitida.Nenhuma);

    /** Regra pura: esta embarcação leva esta classe? */
    fun admite(classe: ClasseVeiculo?): Boolean = classe != null && cargaAdmitida.admite(classe)

    /** `false` = embarcação só de passageiro; o modo veículo nem se oferece. */
    val levaVeiculo: Boolean get() = cargaAdmitida != CargaAdmitida.Nenhuma

    /** As classes que este casco leva, **derivadas** — é o que a escolha da emissão vai oferecer. */
    val classesAdmitidas: List<ClasseVeiculo> get() = ClasseVeiculo.entries.filter(::admite)

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
