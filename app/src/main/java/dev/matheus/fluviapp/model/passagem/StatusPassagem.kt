package dev.matheus.fluviapp.model.passagem

/**
 * Status do ciclo de vida da passagem como **tipo de domínio** (ADR-0012), no lugar da String solta
 * pendurada no catálogo genérico `Constante.Descricao` — que gerava grafia à deriva
 * ("A EMITIR" / "A_EMITIR" / "EMITIDA"). String só na fronteira: `de()` converte na leitura (tolerante
 * à grafia legada), `name` é o valor **canônico** gravado e `rotulo()` formata para exibição.
 *
 * Máquina de estados fail-closed: `A_EMITIR → EMITIDA → EMBARCADA`. `EMBARCADA` é **terminal**
 * (embarque irreversível). Transição ilegal é recusada — é o que dá semântica à confirmação por QR
 * (não embarca bilhete não emitido; não reembarca o já usado).
 */
enum class StatusPassagem {
    A_EMITIR,
    EMITIDA,
    EMBARCADA;

    /** Estados alcançáveis a partir deste. Vazio = terminal. */
    private val proximos: Set<StatusPassagem>
        get() = when (this) {
            A_EMITIR -> setOf(EMITIDA)
            EMITIDA -> setOf(EMBARCADA)
            EMBARCADA -> emptySet()
        }

    fun podeTransicionarPara(destino: StatusPassagem): Boolean = destino in proximos

    fun ehTerminal(): Boolean = proximos.isEmpty()

    /** Valor de exibição/impressão (ex.: `A_EMITIR` → "A EMITIR"). */
    fun rotulo(): String = name.replace("_", " ")

    companion object {
        /**
         * Converte o status persistido (String) no enum canônico; `null` se desconhecido (fail-closed).
         * Tolerante à grafia legada: normaliza espaços→underscore e caixa ("A EMITIR" → `A_EMITIR`).
         */
        fun de(valor: String?): StatusPassagem? {
            val normalizado = valor?.trim()?.uppercase()?.replace(" ", "_") ?: return null
            return entries.firstOrNull { it.name == normalizado }
        }
    }
}