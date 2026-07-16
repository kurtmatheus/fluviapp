package dev.matheus.fluviapp.services.repository.firebase

import kotlinx.coroutines.flow.Flow

/**
 * Porta sobre a fonte de snapshots em tempo real (estudo sincronizacao-firestore-room.md, §10 Nível 2).
 * Abstrai o `addSnapshotListener` do Firestore para que o ciclo de vida do sync (idempotência, lote,
 * parada, erro) seja testável sem Firebase: produção usa [FonteSnapshotsFirestore]; testes, um fake.
 *
 * Emite [DocumentoBruto] (neutro) — sem tipos Firebase na assinatura. Erro NÃO encerra o Flow (o
 * Firestore reconecta); vira um [ResultadoColecao.Falha]/[ResultadoDocumento.Falha].
 */
interface FonteSnapshots {
    /** Observa uma coleção inteira. */
    fun observar(colecao: String): Flow<ResultadoColecao>

    /** Observa um único documento (ex.: contador de bilhete). */
    fun observarDocumento(colecao: String, documento: String): Flow<ResultadoDocumento>
}

sealed interface ResultadoColecao {
    data class Dados(val documentos: List<DocumentoBruto>, val doCache: Boolean) : ResultadoColecao
    data class Falha(val causa: Throwable) : ResultadoColecao
}

sealed interface ResultadoDocumento {
    data class Dados(val documento: DocumentoBruto?, val doCache: Boolean) : ResultadoDocumento
    data class Falha(val causa: Throwable) : ResultadoDocumento
}
