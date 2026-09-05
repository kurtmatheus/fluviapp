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
 * ### O que cada valor declara
 *
 * A [NaturezaVeiculo] — o eixo — e **uma exceção**: a cilindrada, que é da moto e de mais ninguém.
 *
 * O que morreu no caminho:
 *
 * - `exigeModelo` (decisão de 2026-09-03): o modelo é sempre oferecido e nunca cobrado. Onze classes novas
 *   exigiriam onze arbitragens sobre uma pergunta que o *data-intensive* já respondia — guarda-se o que se
 *   tem, não se cobra o que não se sabe;
 * - `ehPesado`, por outra razão: nunca teve um leitor. O recorte da balsa acabou sendo feito por
 *   pertencimento explícito, e desde o D4 é o casco que responde, por exclusão.
 *
 * `exigeCilindrada` **chegou a derivar da natureza** e voltou a ser declarada em 2026-09-05, quando a
 * operação corrigiu dois fatos: o quadriciclo não a exige, e o jet-ski — que a derivação usava como prova
 * de que o traço era de família — é **rebocado**. Sobrou um `true` numa tabela de dezessete linhas, e é
 * assim que uma exceção deve parecer: visível, e só ela.
 */
enum class ClasseVeiculo(
    val rotulo: String,
    val natureza: NaturezaVeiculo,
    /**
     * O cc é o que distingue uma moto de outra na travessia — e **só a moto**. Onde é `false`, a cilindrada
     * não é opcional: é **sem sentido**, e por isso o formulário não a pergunta.
     */
    val exigeCilindrada: Boolean = false,
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

    // --- Motociclo: duas ou quatro rodas de porte pequeno, que entram andando ---
    //
    // Só a **moto** exige cilindrada (correção da operação, 2026-09-05): no quadriciclo o cc não é o que a
    // distingue na travessia, e cobrá-lo seria pedir um dado que ninguém usa.
    MOTO("Moto", NaturezaVeiculo.MOTOCICLO, exigeCilindrada = true),
    QUADRICICLO("Quadriciclo", NaturezaVeiculo.MOTOCICLO),

    // --- Máquina: trabalha, não transporta ---
    TRATOR("Trator", NaturezaVeiculo.MAQUINA),
    EMPILHADEIRA("Empilhadeira", NaturezaVeiculo.MAQUINA),
    RETROESCAVADEIRA("Retroescavadeira", NaturezaVeiculo.MAQUINA),

    // --- Rebocado: não entra andando ---
    CARRETA("Carreta", NaturezaVeiculo.REBOCADO),
    TRAILER("Trailer", NaturezaVeiculo.REBOCADO),
    CARRETILHA("Carretilha", NaturezaVeiculo.REBOCADO),

    // O nome diz *moto aquática*; a doca diz **rebocado** — ele chega sobre a carretilha, como a lancha
    // (correção da operação, 2026-09-05). A natureza descreve **como embarca**, não o que é.
    JET_SKI("Jet-Ski", NaturezaVeiculo.REBOCADO),

    // `Lancha` **como classe** — a embarcação transportada sobre carretilha, não o casco que transporta.
    // O nome colide com `TipoEmbarcacao.LANCHA` e o analista recusou renomear (ADR-0031 D5): o rename de
    // `Navio` → `Embarcacao` já separou gênero de espécie, e os três papéis são explícitos.
    LANCHA("Lancha", NaturezaVeiculo.REBOCADO);

    companion object {
        /** Fronteira String→enum; `null` se desconhecido (fail-closed). Tolerante à grafia legada. */
        fun de(valor: String?): ClasseVeiculo? {
            val normalizado = valor?.trim()?.uppercase()?.replace(" ", "_") ?: return null
            return entries.firstOrNull { it.name == normalizado }
        }
    }
}
