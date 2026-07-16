package dev.matheus.fluviapp.services.repository.firebase

import dev.matheus.fluviapp.telemetry.RegistroSincronizacao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach

/**
 * Espelha uma coleção do Firestore no Room como um Flow gerenciado e OBSERVÁVEL, dependendo da porta
 * [FonteSnapshots] — não mais do `FirebaseFirestore` concreto (estudo sincronizacao-firestore-room.md,
 * §10 Nível 2). Assim o ciclo de vida é testável sem Firebase (fake de [FonteSnapshots]):
 * - grava em LOTE (`salvarTodos`) no [scope], sem `runBlocking` (D3);
 * - erro (`Falha`) só registra, não encerra o Flow — o Firestore reconecta;
 * - `iniciado`/`parado` marcam anexar/remover (o `onCompletion` cobre o cancelamento do escopo).
 *
 * Devolve o [Job] para o repositório controlar idempotência (não re-anexar) e cancelamento.
 */
fun <T> sincronizarColecao(
    fonte: FonteSnapshots,
    colecao: String,
    scope: CoroutineScope,
    registro: RegistroSincronizacao,
    paraModelo: (DocumentoBruto) -> T?,
    salvarTodos: suspend (List<T>) -> Unit,
): Job {
    registro.iniciado(colecao)
    return fonte.observar(colecao)
        .onEach { resultado ->
            when (resultado) {
                is ResultadoColecao.Dados -> {
                    registro.snapshotRecebido(colecao, resultado.documentos.size, resultado.doCache)
                    val itens = resultado.documentos.mapNotNull(paraModelo)
                    salvarTodos(itens)
                    registro.gravado(colecao, itens.size)
                }

                is ResultadoColecao.Falha -> registro.erro(colecao, resultado.causa)
            }
        }
        .onCompletion { registro.parado(colecao) }
        .launchIn(scope)
}
