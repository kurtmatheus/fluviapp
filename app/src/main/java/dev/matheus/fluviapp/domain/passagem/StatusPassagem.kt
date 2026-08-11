package dev.matheus.fluviapp.domain.passagem

/**
 * Status do ciclo de vida da passagem como **tipo de domínio** (ADR-0012), no lugar da String solta
 * pendurada no catálogo genérico `Constante.Descricao` — que gerava grafia à deriva
 * ("A EMITIR" / "A_EMITIR" / "EMITIDA"). String só na fronteira: `de()` converte na leitura (tolerante
 * à grafia legada), `name` é o valor **canônico** gravado e `rotulo()` formata para exibição.
 *
 * Máquina de estados fail-closed: `A_EMITIR → EMITIDA → EMBARCADA`. `EMBARCADA` é **terminal**
 * (embarque irreversível). Transição ilegal é recusada — é o que dá semântica à confirmação por QR
 * (não embarca bilhete não emitido; não reembarca o já usado).
 *
 * ### `CANCELADA` — o estado que substituiu o *delete* ([ADR-0018] D17, [ADR-0024] D11)
 *
 * Cancelar **era remoção física**, e deixou de ser porque **manter histórico é prioridade**: um bilhete apagado
 * leva consigo o fato de que existiu, quem o emitiu, quanto se cobrou e por que deixou de valer. Ela é
 * **terminal**, alcançável de `A_EMITIR` e de `EMITIDA` e **nunca de `EMBARCADA`** — quem já embarcou não cancela
 * a travessia; o que existe depois disso é acerto financeiro, e acerto é do módulo de faturamento.
 *
 * Estado, e não um segundo campo `ativa`, pelo mesmo motivo que o lançamento não ganhou um eixo "a receber":
 * **uma máquina de estados, não duas**.
 *
 * O que ela implica fora daqui (D18): cancelada **não ocupa** estoque, **não entra** na receita e **fica com o
 * número** — sequência com buraco é o normal de uma numeração que registra fatos.
 */
enum class StatusPassagem {
    A_EMITIR,
    EMITIDA,
    EMBARCADA,
    CANCELADA;

    /** Estados alcançáveis a partir deste. Vazio = terminal. */
    private val proximos: Set<StatusPassagem>
        get() = when (this) {
            A_EMITIR -> setOf(EMITIDA, CANCELADA)
            EMITIDA -> setOf(EMBARCADA, CANCELADA)
            EMBARCADA -> emptySet()
            CANCELADA -> emptySet()
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