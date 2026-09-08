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
     * Decimal — o Firestore devolve número como `Long` ou `Double` conforme o que foi gravado, então
     * coagir de `Number` é a única leitura que funciona nos dois casos (distância e tempo da Rota).
     */
    fun decimal(chave: String): Double = (dados[chave] as? Number)?.toDouble() ?: 0.0

    /**
     * Instante em millis de época — `Long`, e não [inteiro], porque millis não cabem em `Int`: 2026 já
     * passa dos 1,7 trilhão, e `toInt()` devolveria um número truncado sem avisar.
     *
     * `0` é o padrão de ausência, e quem chama declara o que ele significa. No `expiraEm` do acesso
     * (ADR-0032 Q1) zero quer dizer **sem prazo** — e é por isso que o campo nunca é gravado como `null`:
     * a regra do servidor compara números, e um `null` no meio de uma comparação derruba a avaliação.
     */
    fun instante(chave: String): Long = (dados[chave] as? Number)?.toLong() ?: 0L

    /**
     * Booleano com **padrão explícito**, e não `false` fixo: ausente pode significar coisas opostas
     * conforme o campo. No `ativo` do delete lógico, documento antigo sem o campo é um registro **em uso**
     * — assumir `false` esconderia dado bom. Quem chama declara o que a ausência quer dizer.
     */
    fun booleano(chave: String, padrao: Boolean = false): Boolean = dados[chave] as? Boolean ?: padrao

    /**
     * Mapa — a forma que o Firestore devolve um objeto aninhado (o `vinculo` do funcionário, ADR-0016
     * §6, [ADR-0032] Q2).
     *
     * Substituiu `listaDeMapas`, que existia porque o vínculo era um `array` e perdeu o único chamador
     * quando ele deixou de ser. E a troca não é só de forma: **campo dentro de mapa o Firestore
     * consulta** (`vinculo.empresaId`), campo dentro de elemento de array não — é o que dispensou o
     * derivado `empresaIds` ao lado.
     *
     * Chave de tipo estranho é **descartada**, não coagida; ausente ou tipo errado → mapa vazio, como os
     * demais acessores. Quem recusa vínculo incompleto é a fronteira do domínio ([Vinculo.de]), não este
     * acessor: aqui só se lê a forma.
     */
    fun mapa(chave: String): Map<String, Any?> {
        val bruto = dados[chave] as? Map<*, *> ?: return emptyMap()
        return bruto.entries.mapNotNull { (k, v) -> (k as? String)?.let { it to v } }.toMap()
    }

}
