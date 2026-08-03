package dev.matheus.fluviapp.domain.passagem

/**
 * Forma de pagamento de um **lançamento** como tipo de domínio ([ADR-0020 D3]), no lugar da linha de
 * catálogo `Constante.Categoria.PAGAMENTO`. O [ADR-0018] D11 já havia notado que, com lançamentos
 * `{id, forma, valor}`, a `forma` "quer ser tipo".
 *
 * O contra-argumento registrado no estudo da E3 era que *"meio de pagamento novo é fato de mercado, não de
 * código"*. Ele não sobrevive ao PIX, que chegou trazendo QR, conciliação e liquidação imediata — tudo
 * código. Uma linha `VOUCHER` num catálogo dá uma entrada de dropdown e nada mais: não diz se o valor
 * equivale a caixa nem quando ele entra, que é justamente o que o balanço financeiro (ADR-0014) precisa
 * saber para conciliar.
 */
enum class FormaPagamento(
    val rotulo: String,
    /** O valor entra no caixa no ato da emissão (PIX, dinheiro, débito) ou depois (crédito). */
    val liquidacaoImediata: Boolean,
    /** Espécie física — a única que o fechamento de caixa conta na gaveta. */
    val ehEspecie: Boolean,
) {
    DINHEIRO("Dinheiro", liquidacaoImediata = true, ehEspecie = true),
    PIX("PIX", liquidacaoImediata = true, ehEspecie = false),
    DEBITO("Débito", liquidacaoImediata = true, ehEspecie = false),
    CREDITO("Crédito", liquidacaoImediata = false, ehEspecie = false);

    companion object {
        /** Fronteira String→enum; `null` se desconhecido (fail-closed). Tolerante à grafia legada. */
        fun de(valor: String?): FormaPagamento? {
            val normalizado = valor?.trim()?.uppercase()?.replace(" ", "_") ?: return null
            return entries.firstOrNull { it.name == normalizado }
        }
    }
}