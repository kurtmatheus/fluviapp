package dev.matheus.fluviapp.domain.viagem

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * A **travessia concreta** para onde a passagem aponta: `(viagemId, data)` — ADR-0016 §7.1, [ADR-0023] D2.
 *
 * A [Viagem] é uma saída **semanal** ("terça às 18h"); a ocorrência é uma dessas saídas **numa data**. É sobre a
 * ocorrência que se vende bilhete, se numera e se conta ocupação — e é por isso que o agregado aponta para ela e
 * não para a viagem: sem a data, dois bilhetes de terças diferentes seriam indistinguíveis.
 *
 * ### A diferença entre isto e a `ViagemSemana`
 *
 * A [ViagemSemana] é a ocorrência **com a viagem inteira dentro**, calculada para montar a tela de saídas
 * disponíveis. Esta é a **chave** — só os dois campos que identificam a travessia —, e é o que a passagem
 * guarda. Uma serve para mostrar; a outra, para referenciar.
 *
 * ### A data é texto ISO na fronteira, e isso não é convenção: é consulta
 *
 * `yyyy-MM-dd` ordena lexicograficamente na mesma ordem em que ordena cronologicamente, compara por igualdade
 * **sem normalização** (data de viagem é calendário, não instante) e **serve de id de documento** — que é o que
 * um `Timestamp` não pode ser ([ADR-0024] D2). Aqui dentro ela é [LocalDate]; o texto é a borda.
 */
data class OcorrenciaViagem(
    val viagemId: String,
    val data: LocalDate,
) {
    /** A data como a fronteira a grava e a consulta a compara (ISO-8601). */
    val dataIso: String get() = data.format(FORMATO_ISO)

    /**
     * A chave legível da ocorrência. Não é id de documento — o contador mora numa **subcoleção** da viagem
     * (ADR-0024 D6), onde o caminho já carrega o `viagemId` e o nome do documento é a [dataIso].
     */
    val chave: String get() = "$viagemId@$dataIso"

    companion object {
        private val FORMATO_ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

        /**
         * Fronteira texto→tipo. `null` quando falta viagem ou a data é ilegível — fail-closed, como os codecs:
         * uma ocorrência sem data não é uma ocorrência "de hoje", não é nada.
         */
        fun de(viagemId: String?, dataIso: String?): OcorrenciaViagem? {
            if (viagemId.isNullOrBlank()) return null
            val data = runCatching { LocalDate.parse(dataIso, FORMATO_ISO) }.getOrNull() ?: return null
            return OcorrenciaViagem(viagemId = viagemId, data = data)
        }

        /**
         * O caminho de volta da [chave] — é ela que atravessa a **navegação** quando o card de saída leva à
         * emissão (F9.5).
         *
         * Existe para que a rota carregue **uma** coordenada em vez de dois argumentos que podem chegar
         * desemparelhados. Fail-closed como o [de]: chave malformada não vira ocorrência de hoje, não vira
         * nada — e a emissão que a recebesse não teria para onde vender.
         */
        fun deChave(chave: String?): OcorrenciaViagem? {
            val partes = chave?.split(SEPARADOR) ?: return null
            if (partes.size != 2) return null
            return de(partes[0], partes[1])
        }

        private const val SEPARADOR = "@"
    }
}