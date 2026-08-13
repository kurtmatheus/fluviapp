package dev.matheus.fluviapp.ui.states.passagem

/**
 * **O detalhamento que precede a emissão** — a conferência dos dados inseridos, antes de o bilhete existir.
 *
 * ### Ela não é um passo, e a distinção é de natureza
 *
 * Os passos ([PassoDaEmissao]) são **perguntas**: cada um coleta uma decisão e o roteiro os conta. Este
 * detalhamento não coleta nada — ele **devolve** o que já foi respondido, para o operador olhar antes de
 * confirmar. Por isso não entra no roteiro nem na trilha: contá-lo como passo faria "5 de 6" virar "5 de 7"
 * sem que houvesse mais uma coisa a decidir.
 *
 * *(Isto corrige o [ADR-0028] D4, que dizia que a confirmação **não** existiria por custar um toque a mais.
 * O argumento era sobre velocidade e ignorava o que a emissão é: **irreversível** — cancelar mantém o número
 * e o registro. Conferir antes é mais barato do que cancelar depois, e é o que o operador de balcão faz de
 * qualquer jeito, lendo a tela em voz alta para o passageiro.)*
 *
 * ### Tudo aqui vem do que está **em edição**
 *
 * Nenhum campo desta projeção exige I/O: nome, documento, placa e valores estão no formulário; travessia e
 * partida vieram no cabeçalho. É por isso que o detalhamento aparece **instantâneo** — e é também o que o
 * torna honesto: ele mostra o que vai ser gravado, não o que já foi.
 */
data class ConfirmacaoDaEmissao(
    val cabecalho: CabecalhoDaViagem,
    /** "Suíte · 2 pessoas", "Veículo · Moto" — o que se está vendendo. */
    val bilhete: String,
    /** Uma linha por pessoa: nome e documento formatado. Vazia no bilhete de veículo sem responsável. */
    val pessoas: List<PessoaConferida>,
    /** Presente só no bilhete de veículo. */
    val veiculo: VeiculoConferido? = null,
    /** Uma linha por forma de pagamento com valor. Vazia na gratuidade. */
    val lancamentos: List<LancamentoConferido>,
    /** A soma, já em moeda — o que o operador confere contra o que recebeu. */
    val total: String,
    val observacao: String?,
    /**
     * O nome da **agência emissora**, que é quem assina o bilhete. É por ele que a marca se resolve
     * (`marcaDaAgencia`), e é ele que aparece no detalhamento e no bilhete digital.
     */
    val agencia: String,
) {
    val ehDeVeiculo: Boolean get() = veiculo != null
}

/** Quem viaja, como o operador vai conferir em voz alta: nome e documento. */
data class PessoaConferida(
    val papel: String,
    val nome: String,
    val documento: String,
    val nascimento: String,
)

data class VeiculoConferido(
    val placa: String,
    val classe: String,
    val modelo: String?,
    val cor: String?,
    val cilindrada: String?,
)

data class LancamentoConferido(
    val forma: String,
    val valor: String,
)