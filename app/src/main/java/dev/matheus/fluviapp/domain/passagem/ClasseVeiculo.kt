package dev.matheus.fluviapp.domain.passagem

/**
 * A classe do veículo embarcado — **um registro, e o enum é o registro** ([ADR-0031] D1).
 *
 * ### Por que continua sendo tipo, e não virou cadastro
 *
 * O pedido da operação foi que o supervisor pudesse cadastrar classes novas, e a resposta é a mesma de
 * 2026-08-01, alcançada por outro caminho: **a classe é a chave de uma série histórica** — o produto do
 * modo veículo é a série *contagem × classe × preço* — e série não convive com identidade que se renomeia.
 * O `name` do enum é essa identidade, e é o que o Firestore grava.
 *
 * O mecanismo veio do `Cargo`, **sem a segunda metade dele**: o que a régua do [ADR-0020] exigia era que
 * **poder e registro deixassem de morar na mesma palavra**. Ali, tirar o poder fez o cargo virar dado —
 * mas isso foi consequência, não requisito. Aqui a separação bastou.
 *
 * ### O que cada valor declara: uma coisa só
 *
 * A [NaturezaVeiculo]. Todo o resto **deriva** dela ou deixou de existir:
 *
 * - `exigeCilindrada` deriva — é a natureza que sabe se há motor a medir;
 * - `exigeModelo` **morreu** (decisão de 2026-09-03): o modelo é sempre oferecido e nunca cobrado. Onze
 *   classes novas exigiriam onze arbitragens sobre uma pergunta que o *data-intensive* já respondia —
 *   guarda-se o que se tem, não se cobra o que não se sabe;
 * - `ehPesado` **morreu** por outra razão: nunca teve um leitor. O recorte da balsa acabou sendo feito por
 *   pertencimento explícito, e desde o D4 é o casco que responde, por exclusão.
 *
 * Sobrou uma tabela de dezessete linhas e **uma coluna de comportamento**, que é o que o estudo prometeu.
 */
enum class ClasseVeiculo(
    val rotulo: String,
    val natureza: NaturezaVeiculo,
) {
    // --- Automotor: roda em estrada e entra andando ---
    CARRO("Carro", NaturezaVeiculo.AUTOMOTOR),
    VAN("Van", NaturezaVeiculo.AUTOMOTOR),
    SUV("SUV", NaturezaVeiculo.AUTOMOTOR),
    CAMINHAO("Caminhão", NaturezaVeiculo.AUTOMOTOR),
    MOTORHOME("Motorhome", NaturezaVeiculo.AUTOMOTOR),
    ONIBUS("Ônibus", NaturezaVeiculo.AUTOMOTOR),

    // "Cavalinho" é o cavalo mecânico: a unidade **tratora**, motorizada — e não o semirreboque que o
    // nome também evoca. Foi o único valor da lista que o estudo não conseguiu classificar sozinho, e a
    // definição veio do analista. É o caso que prova por que a natureza precisa ser declarada.
    CARRETA_CAVALINHO("Carreta Cavalinho", NaturezaVeiculo.AUTOMOTOR),

    // --- Motociclo: o motor é o que distingue um do outro ---
    MOTO("Moto", NaturezaVeiculo.MOTOCICLO),
    QUADRICICLO("Quadriciclo", NaturezaVeiculo.MOTOCICLO),
    JET_SKI("Jet-Ski", NaturezaVeiculo.MOTOCICLO),

    // --- Máquina: trabalha, não transporta ---
    TRATOR("Trator", NaturezaVeiculo.MAQUINA),
    EMPILHADEIRA("Empilhadeira", NaturezaVeiculo.MAQUINA),
    RETROESCAVADEIRA("Retroescavadeira", NaturezaVeiculo.MAQUINA),

    // --- Rebocado: não entra andando ---
    CARRETA("Carreta", NaturezaVeiculo.REBOCADO),
    TRAILER("Trailer", NaturezaVeiculo.REBOCADO),
    CARRETILHA("Carretilha", NaturezaVeiculo.REBOCADO),

    // `Lancha` **como classe** — a embarcação transportada sobre carretilha, não o casco que transporta.
    // O nome colide com `TipoEmbarcacao.LANCHA` e o analista recusou renomear (ADR-0031 D5): o rename de
    // `Navio` → `Embarcacao` já separou gênero de espécie, e os três papéis são explícitos.
    LANCHA("Lancha", NaturezaVeiculo.REBOCADO);

    /** A cilindrada é da **natureza**, não da classe: o que distingue uma moto de outra é o motor. */
    val exigeCilindrada: Boolean get() = natureza.exigeCilindrada

    companion object {
        /** Fronteira String→enum; `null` se desconhecido (fail-closed). Tolerante à grafia legada. */
        fun de(valor: String?): ClasseVeiculo? {
            val normalizado = valor?.trim()?.uppercase()?.replace(" ", "_") ?: return null
            return entries.firstOrNull { it.name == normalizado }
        }
    }
}
