package dev.matheus.fluviapp.domain.operacoes

/**
 * Atuação de uma empresa num segmento, como tipo de domínio ([ADR-0020 D5]).
 *
 * **Precisão que evita ler o ADR além do que ele diz:** a atuação *de uma empresa* **continua sendo
 * cadastrada** — é o painel da plataforma que declara que esta empresa atua assim, criando
 * `empresas/{id}/atuacoes/{ATUACAO}` com as concessões dela. O que é tipo é o **conjunto de valores
 * possíveis**. Vocabulário é código; o fato é dado.
 *
 * O [ADR-0016] já tratava o valor como fechado em quatro lugares — o id do documento é o próprio nome da
 * atuação (§4), as famílias de menu **derivam** dela (§2), o cargo é **qualificado** por ela (§6.1) e o §8
 * admitia que *"valor novo no catálogo não ganha painel sozinho"* — contra uma única linha que a listava
 * como categoria de catálogo. O ADR-0020 resolve a favor das quatro.
 *
 * `PORTUARIA_*` nascem **dormentes** (ADR-0016 §5): existem no modelo porque a concessão já as referencia,
 * e é onde o módulo de check-in vai morar, mas nenhuma seção deriva delas ainda.
 */
enum class Atuacao(
    val rotulo: String,
    /** `false` = prevista no modelo, sem operação nem seções ainda (ADR-0016 §5). */
    val operante: Boolean,
) {
    AGENCIAMENTO("Agenciamento", operante = true),
    TRANSPORTE("Transporte", operante = true),
    PORTUARIA_OPERACAO("Operação portuária", operante = false),
    PORTUARIA_ARRENDAMENTO("Arrendamento portuário", operante = false);

    companion object {
        /** As que hoje produzem operação e menu. */
        fun operantes(): List<Atuacao> = entries.filter { it.operante }

        /** Fronteira String→enum; `null` se desconhecido (fail-closed). Tolerante à grafia legada. */
        fun de(valor: String?): Atuacao? {
            val normalizado = valor?.trim()?.uppercase()?.replace(" ", "_") ?: return null
            return entries.firstOrNull { it.name == normalizado }
        }
    }
}