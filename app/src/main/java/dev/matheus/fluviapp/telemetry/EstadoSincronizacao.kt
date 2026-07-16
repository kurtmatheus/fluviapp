package dev.matheus.fluviapp.telemetry

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Saúde corrente da sincronização, consumível pela UI (estudo sincronizacao-firestore-room.md, D4/§10).
 * É a face observável do ciclo de vida: alimentada pelo [RegistroSincronizacao] (erro → [comErro]=true;
 * snapshot do servidor → false), lida por quem mostra o banner offline-first (MainScreenViewModel).
 *
 * Offline-first: [comErro]=true NÃO troca a tela — os dados do cache (Room) continuam; a UI só
 * sobrepõe um aviso não-bloqueante. `@Singleton` para ser a mesma instância entre quem reporta e quem observa.
 */
@Singleton
class EstadoSincronizacao @Inject constructor() {

    private val _comErro = MutableStateFlow(false)
    val comErro: StateFlow<Boolean> = _comErro.asStateFlow()

    fun reportarErro() {
        _comErro.value = true
    }

    /** Snapshot do servidor chegou ⇒ conectado de novo ⇒ limpa o aviso. */
    fun reportarSucesso() {
        _comErro.value = false
    }
}
