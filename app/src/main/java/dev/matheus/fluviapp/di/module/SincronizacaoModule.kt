package dev.matheus.fluviapp.di.module

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/** Qualifica o escopo de sessão dos listeners de sincronização (ADR-0008 / estudo sync D2). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SyncScope

@Module
@InstallIn(SingletonComponent::class)
object SincronizacaoModule {

    /**
     * Escopo de sessão dos listeners de sync (estudo sincronizacao-firestore-room.md, D2/D3).
     * `SupervisorJob`: falha de um sync não derruba os outros. `Dispatchers.IO`: escritas no Room.
     * Cancelar os filhos (via [SincronizacaoSessao]) remove todas as registrations pelo `awaitClose`,
     * mantendo o escopo vivo para um novo login re-iniciar o sync.
     */
    @Provides
    @Singleton
    @SyncScope
    fun provideSyncScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
