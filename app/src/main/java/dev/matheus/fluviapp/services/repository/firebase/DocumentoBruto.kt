package dev.matheus.fluviapp.services.repository.firebase

/**
 * Documento remoto NEUTRO (estudo sincronizacao-firestore-room.md, §10 Nível 2): id + dados, sem
 * nenhum tipo do Firebase. É o que a porta [FonteSnapshots] emite — assim o mapeamento (Map→modelo)
 * e o ciclo de vida do sync ficam testáveis sem Firebase. Os acessores tipados centralizam as coerções
 * (Firestore devolve inteiros como Number) e são triviais de testar.
 */
data class DocumentoBruto(
    val id: String,
    val dados: Map<String, Any?>,
) {
    fun texto(chave: String): String = dados[chave] as? String ?: ""
    fun inteiro(chave: String): Int = (dados[chave] as? Number)?.toInt() ?: 0

    /**
     * Mapa aninhado chave(String)→valor(Double) — a forma que o Firestore devolve um `map` de números
     * (ADR-0013, tabela de tarifas). Coage cada valor de Number→Double (Firestore devolve Long ou
     * Double); entradas com chave/valor de tipo inesperado são descartadas (defensivo, como os demais
     * acessores). Ausente ou tipo errado → mapa vazio.
     */
    fun mapaDeDoubles(chave: String): Map<String, Double> {
        val bruto = dados[chave] as? Map<*, *> ?: return emptyMap()
        return bruto.entries.mapNotNull { (k, v) ->
            val nome = k as? String ?: return@mapNotNull null
            val valor = (v as? Number)?.toDouble() ?: return@mapNotNull null
            nome to valor
        }.toMap()
    }
}
