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
 * ### O que ela **não** decide: a cilindrada
 *
 * A primeira versão derivava `exigeCilindrada` daqui, supondo que motor medido em cilindrada fosse traço de
 * família. **A operação corrigiu em 2026-09-05, com dois fatos:** o jet-ski é moto aquática mas **é
 * rebocado**, e o quadriciclo **não tem cilindrada obrigatória** — só a moto tem.
 *
 * Com isso a cilindrada volta a ser declarada em [ClasseVeiculo], e é honesto que seja: ela não é
 * propriedade da espécie, é da **moto**. Uma natureza com duas classes em que só uma exige o campo não
 * pode responder pelas duas — derivar ali seria escolher a forma bonita contra o fato.
 *
 * A natureza fica sendo o que o D2 dizia que ela era: **o eixo analítico** que, de quebra, organiza os
 * passos, os ícones e o recorte do casco.
 */
enum class NaturezaVeiculo(val rotulo: String) {
    /** Roda em estrada e entra andando: do carro ao ônibus, incluindo a unidade tratora. */
    AUTOMOTOR(rotulo = "Automotor"),

    /** Duas ou quatro rodas de porte pequeno, que entram andando. */
    MOTOCICLO(rotulo = "Moto e similares"),

    /** Equipamento autopropelido que trabalha, não transporta: trator, empilhadeira, retroescavadeira. */
    MAQUINA(rotulo = "Máquina"),

    /**
     * **Não entra andando.** É a natureza que a lista da operação trouxe e que o domínio não sabia
     * registrar — e ela é fato operacional antes de ser dado: manobra, rampa e quem conduz mudam quando o
     * que embarca não tem propulsão própria.
     *
     * É onde mora o **jet-ski**, e a correção vale registrar: ele é *moto aquática* no nome e **carga
     * rebocada** na doca. O nome descreve o que ele é; a natureza descreve **como ele embarca**, que é o
     * que a operação precisa saber.
     */
    REBOCADO(rotulo = "Rebocado");

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
