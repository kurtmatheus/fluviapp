package dev.matheus.fluviapp.domain.passagem

import java.text.Normalizer

/**
 * O **modo** da passagem — o que se está vendendo — como tipo de domínio ([ADR-0018] D6), no lugar do par
 * `acomodacao: String` (do catálogo) + `isVeiculoChecked: Boolean` e da categoria `CATEGORIA_PASSAGEM`,
 * que era este mesmo eixo com dois valores em vez de quatro (§11.3).
 *
 * São **quatro valores mutuamente exclusivos**: ou se vende uma rede, ou uma suíte, ou um camarote, ou uma
 * vaga de veículo. A unidade vendida é o **espaço**, e só na rede ela coincide com uma pessoa — que é a
 * razão de meia e gratuidade existirem só ali (ADR-0018 D7).
 *
 * **A divergência que este tipo elimina.** O catálogo semeava `"Rede"`, `"Suíte p/ 2 Pessoas"`,
 * `"Suíte p/ 3 Pessoas"` e `"Camarote"`; o código comparava com `REDE`, `SUITE` e `CAMAROTE`. **Nunca
 * casavam** — e como as comparações eram `==` contra String, falhavam em silêncio: a regra "tipo tarifário
 * só na rede" nunca disparava e a contagem de ocupação classificava tudo como nada. Com um só vocabulário,
 * o erro deixa de ser possível.
 *
 * A distinção suíte-de-2 × suíte-de-3 **não é modo, é capacidade**, e já vive onde deve: `Embarcacao` tem
 * `capacidadeSuite2` e `capacidadeSuite3`.
 *
 * > [VEICULO] existe aqui porque o eixo é este, mas o form ainda escolhe veículo por checkbox. Enquanto
 * > isso durar, o seletor de acomodação usa [acomodacoes] — a unificação do eixo é a rework do agregado.
 */
enum class ModoPassagem(
    val rotulo: String,
    /** `true` = espaço de acomodação de passageiro; `false` = vaga de veículo. */
    val ehAcomodacao: Boolean,
) {
    REDE("Rede", ehAcomodacao = true),
    SUITE("Suíte", ehAcomodacao = true),
    CAMAROTE("Camarote", ehAcomodacao = true),
    VEICULO("Veículo", ehAcomodacao = false);

    companion object {
        /** Os três modos de passageiro, na ordem em que o seletor os oferece. */
        fun acomodacoes(): List<ModoPassagem> = entries.filter { it.ehAcomodacao }

        /**
         * Converte o modo persistido no enum canônico; `null` se desconhecido (fail-closed).
         *
         * A tolerância aqui é maior que a dos outros tipos, e de propósito: o catálogo gravou rótulos
         * inteiros como `"Suíte p/ 2 Pessoas"`, então além de caixa e acento vale o **prefixo** — é o que
         * faz o dado já emitido continuar sendo lido depois que o catálogo sai.
         */
        fun de(valor: String?): ModoPassagem? {
            val canonico = valor?.let { canonico(it) }?.takeIf { it.isNotEmpty() } ?: return null
            return entries.firstOrNull { it.name == canonico }
                ?: entries.firstOrNull { canonico.startsWith(it.name) }
        }

        /** Caixa alta, sem acento e com espaço virando underscore: `"Suíte p/ 2"` → `"SUITE_P/_2"`. */
        private fun canonico(valor: String): String =
            Normalizer.normalize(valor.trim(), Normalizer.Form.NFD)
                .replace(MARCAS_DE_ACENTO, "")
                .uppercase()
                .replace(" ", "_")

        private val MARCAS_DE_ACENTO = Regex("\\p{Mn}+")
    }
}