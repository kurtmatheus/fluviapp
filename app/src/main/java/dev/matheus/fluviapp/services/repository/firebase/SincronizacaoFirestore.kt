package dev.matheus.fluviapp.services.repository.firebase

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import dev.matheus.fluviapp.telemetry.RegistroSincronizacao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Espelha uma coleção do Firestore no Room via snapshot listener (ADR-0003), como um Flow gerenciado
 * (estudo sincronizacao-firestore-room.md, D2/D3) e observável (§10):
 * - `callbackFlow` + `awaitClose`: a `ListenerRegistration` é removida quando o [scope] é cancelado
 *   — sem vazamento (o padrão antigo descartava a registration a cada chamada).
 * - grava em LOTE (`salvarTodos`) no [scope] — sem `runBlocking` bloqueando a thread do listener.
 * - no erro, apenas registra (NÃO fecha o flow), deixando o Firestore reconectar sozinho.
 * - emite o ciclo de vida via [registro]: iniciado / snapshot (cache×servidor) / gravado / parado / erro.
 *
 * Devolve o [Job] para o repositório controlar idempotência (não re-anexar) e cancelamento.
 */
fun <T> FirebaseFirestore.sincronizarColecao(
    colecao: String,
    scope: CoroutineScope,
    registro: RegistroSincronizacao,
    paraModelo: (DocumentSnapshot) -> T?,
    salvarTodos: suspend (List<T>) -> Unit,
): Job = callbackFlow {
    registro.iniciado(colecao)
    val registration = collection(colecao).addSnapshotListener { value, error ->
        if (error != null) {
            registro.erro(colecao, error)
            return@addSnapshotListener
        }
        value?.let {
            registro.snapshotRecebido(colecao, it.size(), it.metadata.isFromCache)
            trySend(it.documents.mapNotNull(paraModelo))
        }
    }
    awaitClose {
        registration.remove()
        registro.parado(colecao)
    }
}.onEach { modelos ->
    salvarTodos(modelos)
    registro.gravado(colecao, modelos.size)
}.launchIn(scope)
