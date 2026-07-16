package dev.matheus.fluviapp.services.repository.firebase

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Espelha uma coleção do Firestore no Room via snapshot listener (ADR-0003), como um Flow gerenciado
 * (estudo sincronizacao-firestore-room.md, D2/D3):
 * - `callbackFlow` + `awaitClose`: a `ListenerRegistration` é removida quando o [scope] é cancelado
 *   — sem vazamento (o padrão antigo descartava a registration a cada chamada).
 * - grava em LOTE (`salvarTodos`) no [scope] — sem `runBlocking` bloqueando a thread do listener.
 * - no erro, apenas loga (NÃO fecha o flow), deixando o Firestore reconectar sozinho.
 *
 * Devolve o [Job] para o repositório controlar idempotência (não re-anexar) e cancelamento.
 */
fun <T> FirebaseFirestore.sincronizarColecao(
    colecao: String,
    tag: String,
    scope: CoroutineScope,
    paraModelo: (DocumentSnapshot) -> T?,
    salvarTodos: suspend (List<T>) -> Unit,
    onErro: (Throwable) -> Unit = {},
): Job = callbackFlow {
    val registration = collection(colecao).addSnapshotListener { value, error ->
        if (error != null) {
            Log.e(tag, "sincronizar($colecao): ${error.message}", error)
            onErro(error)
            return@addSnapshotListener
        }
        value?.let { trySend(it.documents.mapNotNull(paraModelo)) }
    }
    awaitClose { registration.remove() }
}.onEach { salvarTodos(it) }.launchIn(scope)
