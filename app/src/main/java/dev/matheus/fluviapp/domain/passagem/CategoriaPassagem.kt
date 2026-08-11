package dev.matheus.fluviapp.domain.passagem

/**
 * A **categoria** da passagem — o eixo raiz do agregado ([ADR-0023] D1).
 *
 * Ela não é "mais um campo": é o que decide **qual sub-domínio** a passagem é. Antes disso, *"esta passagem é
 * de veículo"* era uma dedução sobre a presença de um campo (`ehVeiculo = !placaVeiculo.isNullOrEmpty()`), e
 * daí vinham três coisas ruins — estado misto representável, regra espalhada por tela, e uma categoria nova
 * entrando **em silêncio**.
 *
 * Este enum é o **vocabulário** do eixo; quem carrega a forma é a [Passagem] selada. Os dois existem porque a
 * fronteira precisa de um valor gravável (o discriminador do documento, [ADR-0024] D1) e o código precisa de
 * um tipo que o compilador saiba esgotar.
 *
 * ### Por que `CARGA` não está aqui
 *
 * A carga é o terceiro sub-domínio **previsto** (ADR-0023 D9), e declará-la agora criaria um valor de enum sem
 * ninguém que o carregue — exatamente o que o [ADR-0020] D4 corrigiu no tipo de embarcação: *"se o código
 * precisa falar antes de o valor significar alguma coisa, a lista não é a fonte"*. A prontidão para ela é o
 * **formato** (um `when` exaustivo que passa a acusar cada lugar a decidir), não uma linha reservada.
 */
enum class CategoriaPassagem(val rotulo: String) {
    PASSAGEIRO("Passageiro"),
    VEICULO("Veículo");

    companion object {
        /** Fronteira String→enum; `null` se desconhecido (fail-closed). Tolerante à grafia legada. */
        fun de(valor: String?): CategoriaPassagem? {
            val normalizado = valor?.trim()?.uppercase()?.replace(" ", "_") ?: return null
            return entries.firstOrNull { it.name == normalizado }
        }
    }
}