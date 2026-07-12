package dev.matheus.fluviapp.services.repository.firebase

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking

/**
 * Espelha uma coleção do Firestore no Room via snapshot listener (ADR-0003). Extrai o boilerplate
 * que era triplicado nos repositórios. Diferente do padrão antigo, **não lança dentro do listener**
 * (thread async) — só loga; o `onErro` opcional permite ao chamador reagir sem derrubar o app.
 */
fun <T> FirebaseFirestore.sincronizarColecao(
    colecao: String,
    tag: String,
    paraModelo: (DocumentSnapshot) -> T?,
    salvarLocal: suspend (T) -> Unit,
    onErro: (Throwable) -> Unit = {},
) {
    collection(colecao).addSnapshotListener { value, error ->
        value?.documents?.mapNotNull(paraModelo)?.forEach { modelo ->
            runBlocking { salvarLocal(modelo) }
        }
        if (error != null) {
            Log.e(tag, "sincronizar($colecao): ${error.message}", error)
            onErro(error)
        }
    }
}
