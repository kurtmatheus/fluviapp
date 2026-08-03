package dev.matheus.fluviapp.domain.passagem

/**
 * Subtipo de gratuidade como **tipo de domínio** (ADR-0013), no lugar da String solta pendurada no
 * catálogo genérico `Constante.Descricao` — mesmo movimento que o [StatusPassagem] fez no ADR-0012.
 * São as quatro gratuidades **legais**: idoso, PcD, criança até 5 anos (faixa 0–5, inclui o 5) e passe
 * federal. `CORTESIA` foi **aposentada** (era redução comercial, não gratuidade — cabe como desconto).
 *
 * String só na fronteira: `de()` converte na leitura (tolerante à grafia legada), `name` é o valor
 * **canônico** gravado e `rotulo()` formata para exibição.
 */
enum class TipoGratuidade {
    IDOSO,
    PCD,
    CRIANCA_ATE_5,
    PASSE_FEDERAL;

    /** Valor de exibição/impressão. */
    fun rotulo(): String = when (this) {
        IDOSO -> "Idoso"
        PCD -> "PcD"
        CRIANCA_ATE_5 -> "Criança até 5 anos"
        PASSE_FEDERAL -> "Passe Federal"
    }

    companion object {
        /**
         * Converte o subtipo persistido (String) no enum canônico; `null` se desconhecido (fail-closed).
         * Tolerante à grafia legada: normaliza espaços→underscore e caixa.
         */
        fun de(valor: String?): TipoGratuidade? {
            val normalizado = valor?.trim()?.uppercase()?.replace(" ", "_") ?: return null
            return entries.firstOrNull { it.name == normalizado }
        }
    }
}