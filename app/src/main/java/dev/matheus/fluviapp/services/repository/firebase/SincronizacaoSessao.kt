package dev.matheus.fluviapp.services.repository.firebase

import dev.matheus.fluviapp.di.module.SyncScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ciclo de vida da sincronização de sessão (estudo sincronizacao-firestore-room.md, D2). Os
 * repositórios lançam seus listeners (`callbackFlow`) no [SyncScope]; [parar] cancela os filhos do
 * escopo → cada `awaitClose` remove a `ListenerRegistration`. O escopo em si permanece vivo, então
 * um novo login re-inicia o sync (o `sincronizar()` de cada repo é idempotente).
 */
@Singleton
class SincronizacaoSessao @Inject constructor(
    @SyncScope private val scope: CoroutineScope,
) {
    fun parar() {
        scope.coroutineContext.cancelChildren()
    }
}
