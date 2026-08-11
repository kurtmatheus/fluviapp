package dev.matheus.fluviapp.domain.passagem

/**
 * O **espaço vendido** a um passageiro ([ADR-0023] D3): rede, suíte ou camarote.
 *
 * Substitui o eixo único `ModoPassagem`, que tinha quatro valores porque o **veículo** estava dentro dele. Com
 * a categoria como raiz (ADR-0023 D1), veículo virou sub-domínio e sobrou aqui só o que de fato é acomodação
 * de pessoa — dois níveis onde havia um.
 *
 * ### O tipo tarifário é limitado pela acomodação, e a regra mora aqui
 *
 * *"Inteira (suíte ou camarote), meia ou gratuidade (rede)"* — palavra do analista. Suíte e camarote são sempre
 * **inteira**; meia e gratuidade existem **só na rede**. Ter isso como propriedade da acomodação é o que
 * impede a tela de esconder um seletor por `if`: fora da rede o seletor **não existe**, e *meia numa suíte*
 * deixa de ser escrevível.
 *
 * ### A ocupação é limite, não fato
 *
 * Suíte e camarote são vendidos para uma, duas ou três pessoas; a rede é **uma por bilhete**. Aqui mora o
 * **máximo**; quem diz quantas pessoas há é a **lista de clientes** da passagem (ADR-0023 D3). Um campo de
 * ocupação ao lado da lista poderia discordar dela — e "suíte para três com dois clientes" é exatamente o
 * estado que o agregado novo não deve admitir.
 */
enum class Acomodacao(
    val rotulo: String,
    /** Quantos clientes cabem num bilhete desta acomodação. */
    val ocupacaoMaxima: Int,
    /** Tipos tarifários que esta acomodação admite. */
    val tiposPermitidos: Set<TipoPassagem>,
) {
    REDE(
        rotulo = "Rede",
        ocupacaoMaxima = 1,
        tiposPermitidos = setOf(TipoPassagem.INTEIRA, TipoPassagem.MEIA, TipoPassagem.GRATUIDADE),
    ),
    SUITE(
        rotulo = "Suíte",
        ocupacaoMaxima = 3,
        tiposPermitidos = setOf(TipoPassagem.INTEIRA),
    ),
    CAMAROTE(
        rotulo = "Camarote",
        ocupacaoMaxima = 3,
        tiposPermitidos = setOf(TipoPassagem.INTEIRA),
    );

    /** Regra pura: esta acomodação admite este tipo tarifário? */
    fun admite(tipo: TipoPassagem?): Boolean = tipo != null && tipo in tiposPermitidos

    /** `true` quando há escolha de tipo a fazer — hoje, só a rede. */
    val temEscolhaDeTipo: Boolean get() = tiposPermitidos.size > 1

    companion object {
        /** Fronteira String→enum; `null` se desconhecido (fail-closed). Tolerante à grafia legada. */
        fun de(valor: String?): Acomodacao? {
            val normalizado = valor?.trim()?.uppercase()?.replace(" ", "_") ?: return null
            return entries.firstOrNull { it.name == normalizado }
        }

        /**
         * Fronteira de **tela**: o dropdown mostra o [rotulo] e devolve o texto escolhido. Separada de [de]
         * pela mesma razão do `TipoEmbarcacao`: aquela lê o que o Firestore gravou (o `name`, estável), esta
         * lê o que a pessoa escolheu (o rótulo, reescrevível sem migrar dado).
         */
        fun porRotulo(rotulo: String?): Acomodacao? =
            entries.firstOrNull { it.rotulo.equals(rotulo?.trim(), ignoreCase = true) }
    }
}