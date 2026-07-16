package dev.matheus.fluviapp.fakes

import dev.matheus.fluviapp.services.repository.firebase.FonteSnapshots
import dev.matheus.fluviapp.services.repository.firebase.ResultadoColecao
import dev.matheus.fluviapp.services.repository.firebase.ResultadoDocumento
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Fake da porta [FonteSnapshots]: emite snapshots controlados, sem Firebase (§10 Nível 2). Permite
 * testar o ciclo de vida do sync (lote, erro, parada) alimentando `emitirColecao`/`emitirDocumento`.
 */
class FakeFonteSnapshots : FonteSnapshots {

    private val colecao = MutableSharedFlow<ResultadoColecao>(replay = 1, extraBufferCapacity = 16)
    private val documento = MutableSharedFlow<ResultadoDocumento>(replay = 1, extraBufferCapacity = 16)

    override fun observar(colecao: String): Flow<ResultadoColecao> = this.colecao
    override fun observarDocumento(colecao: String, documento: String): Flow<ResultadoDocumento> = this.documento

    suspend fun emitirColecao(resultado: ResultadoColecao) = colecao.emit(resultado)
    suspend fun emitirDocumento(resultado: ResultadoDocumento) = documento.emit(resultado)
}
