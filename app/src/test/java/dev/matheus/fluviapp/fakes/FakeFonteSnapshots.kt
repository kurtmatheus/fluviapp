package dev.matheus.fluviapp.fakes

import dev.matheus.fluviapp.services.repository.firebase.FonteSnapshots
import dev.matheus.fluviapp.services.repository.firebase.ResultadoColecao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Fake da porta [FonteSnapshots]: emite snapshots controlados, sem Firebase (§10 Nível 2). Permite
 * testar o ciclo de vida do sync (lote, erro, parada) alimentando `emitirColecao`.
 *
 * Ele tinha um segundo canal, `emitirDocumento`, porque a porta tinha um segundo método — e nenhum teste
 * chamava nem um nem outro. É o custo que um método sem chamador cobra mesmo assim: o fake precisa
 * cumpri-lo.
 */
class FakeFonteSnapshots : FonteSnapshots {

    private val colecao = MutableSharedFlow<ResultadoColecao>(replay = 1, extraBufferCapacity = 16)

    override fun observar(colecao: String): Flow<ResultadoColecao> = this.colecao

    suspend fun emitirColecao(resultado: ResultadoColecao) = colecao.emit(resultado)
}
