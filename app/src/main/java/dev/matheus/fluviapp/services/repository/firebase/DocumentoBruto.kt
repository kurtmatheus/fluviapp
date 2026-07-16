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
    fun booleano(chave: String): Boolean = dados[chave] as? Boolean ?: false
}
