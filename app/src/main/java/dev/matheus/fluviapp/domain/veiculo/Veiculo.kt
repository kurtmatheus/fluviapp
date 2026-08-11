package dev.matheus.fluviapp.domain.veiculo

import dev.matheus.fluviapp.domain.passagem.ClasseVeiculo

/**
 * O veículo embarcado — **entidade de pool**, com a **placa** como chave natural ([ADR-0018] D5).
 *
 * A placa é chave melhor que documento de pessoa: **única por construção**, sem o par CPF × RG que faz o pool de
 * clientes acumular duplicata legítima. Então este pool **não polui** — duplicata aqui só nasce de digitação
 * errada, e é contra isso que existe a máscara na entrada (D15).
 *
 * ### O tipo governa o que se exige
 *
 * *"Carreta ou caminhão já equivalentes ao modelo; outros tipos são van, SUV, que têm modelo nomeado"* — palavra
 * do analista ([ADR-0023] D4). Quem sabe se há modelo a pedir é o [ClasseVeiculo], não o formulário: é assim que
 * a primeira divergência do D19 se corrige **no tipo**, e não com um `if` no validador que exigia modelo
 * **sempre** — de modo que carreta e caminhão não passavam.
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
    /** Ausente quando o tipo **já é** o modelo (carreta, caminhão). */
    val modelo: String? = null,
    val cor: String = "",
    /** Só moto: é o cc que distingue uma moto de outra na travessia. */
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
        if (tipo.exigeModelo && modelo.isNullOrBlank()) add(Pendencia.MODELO)
        if (tipo.exigeCilindrada && (cilindrada == null || cilindrada <= 0)) add(Pendencia.CILINDRADA)
    }

    val completo: Boolean get() = pendencias().isEmpty()

    /** O que pode faltar num veículo — nomeado, para a tela apontar o campo certo. */
    enum class Pendencia { PLACA, MODELO, CILINDRADA }

    /** Como o veículo se anuncia num bilhete: o modelo quando existe, senão o próprio tipo. */
    val descricao: String get() = modelo?.takeIf { it.isNotBlank() } ?: tipo.rotulo
}