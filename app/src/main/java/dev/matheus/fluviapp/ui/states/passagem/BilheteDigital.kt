package dev.matheus.fluviapp.ui.states.passagem

/**
 * **O bilhete — o documento que vai para a mão do passageiro.**
 *
 * É a segunda projeção da Passagem ([ADR-0025] D4), e a diferença em relação à primeira é de
 * **destinatário**, não de conteúdo: a [ConfirmacaoDaEmissao] é a conferência **do operador**, lida numa tela
 * de balcão voltada para a fila; este é o comprovante **de quem comprou**, guardado no telefone dele.
 *
 * ### Por isso o documento aqui **não é mascarado**
 *
 * Decisão do analista (2026-08-13), e a assimetria com o detalhamento é coerente: mascarar protege o dado de
 * **quem passa por perto** — a fila, a foto de tela alheia. No bilhete não há terceiro: ele é entregue a quem
 * já sabe o próprio número, e um comprovante com o documento cortado deixa de servir para o que comprovante
 * serve, que é ser conferido contra a identidade na doca.
 *
 * ### O que ele carrega além do óbvio
 *
 * O [idPassagem] não é enfeite de rodapé: é **o que o QR codifica** (ADR-0012). O QR é ponteiro — quem valida
 * lê o documento ao vivo no servidor —, então o bilhete impresso ou fotografado nunca fica "velho": o que ele
 * carrega é o endereço, não o estado.
 */
data class BilheteDigital(
    val idPassagem: String,
    /** "#41" — a identidade exibida, por ocorrência. */
    val numero: String,
    val agencia: String,
    /**
     * "Porto de Val-de-Cães · Belém/PA → Porto de Parintins · Parintins/AM".
     *
     * Chamava-se `travessia`, e o nome mudou junto com o desenho: no bilhete este valor **leva rótulo**, e o
     * rótulo é "Trajeto". Campo e rótulo com o mesmo nome é o que permite ler o documento e o DTO como a mesma
     * coisa — a divergência é o que faz alguém procurar no código um campo que a tela não tem.
     *
     * A [ConferenciaDeEmbarque] segue com `travessia`, e sem rótulo: ali o valor é o assunto da tela, não um
     * campo dela.
     */
    val trajeto: String,
    /** "Terça-feira, 18/08 · 18:00". */
    val partida: String,
    val embarcacao: String,
    /** "Suíte · 2 pessoas", "Veículo · Moto". */
    val bilhete: String,
    /** Uma linha por pessoa, com o documento **por inteiro**. */
    val passageiros: List<PassageiroDoBilhete>,
    val veiculo: VeiculoConferido? = null,
    val total: String,
    val observacao: String? = null,
    /** Presente só quando o bilhete é gratuito — é o que a fiscalização confere. */
    val gratuidade: String? = null,
)

/** Quem viaja, como o bilhete o apresenta: nome e documento completos. */
data class PassageiroDoBilhete(
    val papel: String,
    val nome: String,
    val documento: String,
)