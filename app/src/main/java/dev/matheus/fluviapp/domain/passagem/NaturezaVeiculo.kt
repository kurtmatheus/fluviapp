package dev.matheus.fluviapp.domain.passagem

/**
 * **O que o veículo é** — a única propriedade que a [ClasseVeiculo] declara ([ADR-0031] D2).
 *
 * ### Por que ela existe, e não é arrumação de código
 *
 * A razão registrada é do analista, e é maior que a higiene do enum: *"o sentido de ter natureza como
 * propriedade vai ao encontro da futura **capacidade analítica** do sistema, bem como do
 * **data-intensive**: guardar todas as informações possíveis."*
 *
 * Ou seja, ela não está aqui para encurtar uma tabela. Está aqui porque *"quantos rebocados atravessaram
 * em agosto"* é pergunta que o negócio vai fazer, e que sem esta coluna exigiria alguém reclassificar
 * dezessete nomes à mão — que é o mesmo erro que o [ADR-0008] evita ao relacionar por id em vez de nome.
 *
 * ### O que ela resolve de imediato
 *
 * O custo de um enum não cresce com o número de valores: cresce com o produto **valores × comportamentos**.
 * Com seis classes já havia só **três** assinaturas de comportamento distintas — metade da tabela era
 * repetição —, e com dezessete seriam quatro quintos. Com a natureza no meio, cada classe declara **uma**
 * coisa, e o comportamento mora aqui, onde há quatro valores em vez de dezessete.
 *
 * É a mesma forma que a [Acomodacao] já usa (um enum cujos valores declaram propriedade de outro e derivam
 * a regra dela) — o que muda é a altitude.
 *
 * ### A cilindrada
 *
 * É o único comportamento que sobrou, e ele **pertence à natureza, não à classe**: o que distingue uma moto
 * de outra na travessia é o motor, e isso vale igual para o quadriciclo e para o jet-ski — que, em
 * português, é literalmente uma *moto aquática*. Nas outras três naturezas a cilindrada não é opcional: é
 * **sem sentido**, e por isso o formulário não a pergunta em vez de deixá-la em branco.
 */
enum class NaturezaVeiculo(
    val rotulo: String,
    /**
     * O que se pergunta a mais no formulário — e, no futuro, o eixo que o motor dá à análise.
     *
     * Não confundir com *opcional*: onde é `false`, a cilindrada **não se aplica**, e o campo não existe.
     * O modelo, ao contrário, é sempre oferecido e nunca cobrado (ADR-0031, alternativa aceita em
     * 2026-09-03).
     */
    val exigeCilindrada: Boolean,
) {
    /** Roda em estrada e entra andando: do carro ao ônibus, incluindo a unidade tratora. */
    AUTOMOTOR(rotulo = "Automotor", exigeCilindrada = false),

    /** Motor medido em cilindrada — em terra ou na água. */
    MOTOCICLO(rotulo = "Moto e similares", exigeCilindrada = true),

    /** Equipamento autopropelido que trabalha, não transporta: trator, empilhadeira, retroescavadeira. */
    MAQUINA(rotulo = "Máquina", exigeCilindrada = false),

    /**
     * **Não entra andando.** É a natureza que a lista da operação trouxe e que o domínio não sabia
     * registrar — e ela é fato operacional antes de ser dado: manobra, rampa e quem conduz mudam quando o
     * que embarca não tem propulsão própria.
     */
    REBOCADO(rotulo = "Rebocado", exigeCilindrada = false);

    companion object {
        /** Fronteira String→enum; `null` se desconhecido (fail-closed), como em toda a casa. */
        fun de(valor: String?): NaturezaVeiculo? {
            val normalizado = valor?.trim()?.uppercase()?.replace(" ", "_") ?: return null
            return entries.firstOrNull { it.name == normalizado }
        }
    }

    /** As classes desta natureza, na ordem do enum. É o que o passo da emissão oferece (ADR-0031 D8). */
    val classes: List<ClasseVeiculo> get() = ClasseVeiculo.entries.filter { it.natureza == this }
}
