package dev.matheus.fluviapp.domain.veiculo

import dev.matheus.fluviapp.domain.passagem.ClasseVeiculo

/**
 * O veículo embarcado — **entidade de pool**, com a **placa** como chave natural ([ADR-0018] D5).
 *
 * A placa é chave melhor que documento de pessoa: **única por construção**, sem o par CPF × RG que faz o pool de
 * clientes acumular duplicata legítima. Então este pool **não polui** — duplicata aqui só nasce de digitação
 * errada, e é contra isso que existe a máscara na entrada (D15).
 *
 * ### O que se exige vem da **natureza**, e é uma coisa só
 *
 * A cilindrada, e nada mais ([ADR-0031] D2): ela é de quem tem motor medido em cilindrada — moto,
 * quadriciclo, jet-ski —, e quem sabe disso é a `NaturezaVeiculo`, não o formulário. Nas outras naturezas
 * ela não é opcional: é **sem sentido**, e por isso o campo não existe em vez de ficar em branco.
 *
 * O **modelo saiu da conta** em 2026-09-03. A regra antiga (*"carreta e caminhão já são o modelo"*,
 * ADR-0023 D4) corrigia no tipo o que o validador de então errava exigindo modelo **sempre**; ela cumpriu
 * o papel e caiu quando as classes passaram de seis para dezessete — mantê-la obrigaria a arbitrar onze
 * vezes uma pergunta que o *data-intensive* já responde: **guarda-se o que se tem, não se cobra o que não
 * se sabe**. O campo continua sendo oferecido a toda classe; o que saiu foi a cobrança.
 *
 * ### O responsável pela retirada não mora aqui
 *
 * É **pessoa**, então é `Cliente`, e o vínculo é da **passagem**: quem retira muda a cada travessia, enquanto o
 * veículo é o mesmo. Guardá-lo no veículo faria o último responsável parecer o dono.
 */
data class Veiculo(
    val id: String = "",
    /** Chave natural. Canônica na grafia oficial do padrão — máscara na entrada (ADR-0018 D15). */
    val placa: String,
    val tipo: ClasseVeiculo,
    /** **Sempre opcional** — oferecido a toda classe, cobrado em nenhuma (ADR-0031). */
    val modelo: String? = null,
    val cor: String = "",
    /** Só na natureza `MOTOCICLO`: é o cc que distingue uma moto de outra na travessia. */
    val cilindrada: Int? = null,
    /** Assinatura das agências que já o atenderam (ADR-0018 D3), como no pool de clientes. */
    val agenciaIds: Set<String> = emptySet(),
) {
    /**
     * O que **falta** para este veículo estar completo, segundo o próprio tipo. Vazio = completo.
     *
     * Devolve o que falta em vez de um booleano porque quem chama precisa dizer **qual** campo cobrar — e porque
     * um `Boolean` obrigaria o validador a repetir a regra para descobrir o motivo, que é exatamente o
     * espalhamento que o D4 desfaz.
     */
    fun pendencias(): Set<Pendencia> = buildSet {
        if (placa.isBlank()) add(Pendencia.PLACA)
        if (tipo.exigeCilindrada && (cilindrada == null || cilindrada <= 0)) add(Pendencia.CILINDRADA)
    }

    val completo: Boolean get() = pendencias().isEmpty()

    /**
     * O que pode faltar num veículo — nomeado, para a tela apontar o campo certo.
     *
     * **O `MODELO` saiu em 2026-09-03** (ADR-0031): o modelo é sempre oferecido e nunca cobrado. Ele era a
     * única exigência que não derivava da natureza, e mantê-lo obrigaria a arbitrar, uma a uma, se cada
     * das onze classes novas o exige — decisão que o *data-intensive* já respondia: guarda-se o que se
     * tem, não se cobra o que não se sabe.
     */
    enum class Pendencia { PLACA, CILINDRADA }

    /** Como o veículo se anuncia num bilhete: o modelo quando existe, senão o próprio tipo. */
    val descricao: String get() = modelo?.takeIf { it.isNotBlank() } ?: tipo.rotulo
}