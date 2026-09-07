package dev.matheus.fluviapp.services.repository.firebase

import kotlinx.coroutines.flow.Flow

/**
 * Porta sobre a fonte de snapshots em tempo real (estudo sincronizacao-firestore-room.md, §10 Nível 2).
 * Abstrai o `addSnapshotListener` do Firestore para que o ciclo de vida do sync (idempotência, lote,
 * parada, erro) seja testável sem Firebase: produção usa [FonteSnapshotsFirestore]; testes, um fake.
 *
 * Emite [DocumentoBruto] (neutro) — sem tipos Firebase na assinatura. Erro NÃO encerra o Flow (o
 * Firestore reconecta); vira uma [ResultadoColecao.Falha].
 *
 * A porta tinha um segundo método, `observarDocumento`, para o caso do documento único — e o exemplo que
 * o justificava era o contador de bilhete. O contador acabou indo para **incremento atômico em
 * subcoleção** (ADR-0024 D6), e o método ficou sem chamador desde então. Saiu em 2026-09-07, com o
 * `ResultadoDocumento` que ele arrastava: uma porta com um método que ninguém chama não é extensibilidade,
 * é uma promessa que o fake de teste também precisa cumprir.
 */
interface FonteSnapshots {
    /** Observa uma coleção inteira. */
    fun observar(colecao: String): Flow<ResultadoColecao>
}

sealed interface ResultadoColecao {
    data class Dados(val documentos: List<DocumentoBruto>, val doCache: Boolean) : ResultadoColecao
    data class Falha(val causa: Throwable) : ResultadoColecao
}
