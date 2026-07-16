package dev.matheus.fluviapp.services.repository.firebase

import dev.matheus.fluviapp.fakes.FakeFonteSnapshots
import dev.matheus.fluviapp.telemetry.EstadoSincronizacao
import dev.matheus.fluviapp.telemetry.FakeTelemetry
import dev.matheus.fluviapp.telemetry.RegistroSincronizacao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nível 2 de testabilidade (§10): testa o CICLO DE VIDA de [sincronizarColecao] sem Firebase, via
 * [FakeFonteSnapshots]. Cobre lote (1 snapshot → 1 salvarTodos), erro (registra sem encerrar) e
 * parada (cancelar → evento). O `paraModelo` aqui é trivial (DocumentoBruto → String).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SincronizarColecaoTest {

    private val telemetry = FakeTelemetry()
    private val registro = RegistroSincronizacao(telemetry, EstadoSincronizacao())

    @Test
    fun `grava em lote os itens mapeados de um snapshot`() = runTest(UnconfinedTestDispatcher()) {
        val fonte = FakeFonteSnapshots()
        val salvos = mutableListOf<List<String>>()

        val job = sincronizarColecao(
            fonte = fonte,
            colecao = "viagens",
            scope = backgroundScope,
            registro = registro,
            paraModelo = { it.texto("nome") },
            salvarTodos = { salvos.add(it) },
        )
        fonte.emitirColecao(
            ResultadoColecao.Dados(
                listOf(
                    DocumentoBruto("1", mapOf("nome" to "A")),
                    DocumentoBruto("2", mapOf("nome" to "B")),
                ),
                doCache = false,
            ),
        )
        advanceUntilIdle()

        // uma única escrita, com os dois itens (lote) — não uma por doc.
        assertEquals(listOf(listOf("A", "B")), salvos)
        job.cancel()
    }

    @Test
    fun `falha registra erro e nao encerra o sync`() = runTest(UnconfinedTestDispatcher()) {
        val fonte = FakeFonteSnapshots()

        val job = sincronizarColecao(
            fonte = fonte,
            colecao = "viagens",
            scope = backgroundScope,
            registro = registro,
            paraModelo = { it.texto("nome") },
            salvarTodos = {},
        )
        fonte.emitirColecao(ResultadoColecao.Falha(RuntimeException("offline")))
        advanceUntilIdle()

        assertTrue(telemetry.nomesDeEventos().contains(RegistroSincronizacao.EVENTO_ERRO))
        assertTrue(job.isActive) // não fechou o Flow — deixa reconectar
        job.cancel()
    }

    @Test
    fun `cancelar o escopo dispara o evento de parada`() = runTest(UnconfinedTestDispatcher()) {
        val fonte = FakeFonteSnapshots()

        val job = sincronizarColecao(
            fonte = fonte,
            colecao = "viagens",
            scope = backgroundScope,
            registro = registro,
            paraModelo = { it.texto("nome") },
            salvarTodos = {},
        )
        advanceUntilIdle()
        job.cancel()
        advanceUntilIdle()

        assertTrue(telemetry.nomesDeEventos().contains(RegistroSincronizacao.EVENTO_PARADO))
    }
}
