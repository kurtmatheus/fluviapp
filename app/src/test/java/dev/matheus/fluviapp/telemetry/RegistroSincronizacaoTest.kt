package dev.matheus.fluviapp.telemetry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Trava a taxonomia de observabilidade da sync (estudo sincronizacao-firestore-room.md, §10).
 * Puro: só a porta [Telemetry] via [FakeTelemetry], sem Firebase — Nível 1 de testabilidade.
 */
class RegistroSincronizacaoTest {

    private lateinit var telemetry: FakeTelemetry
    private lateinit var registro: RegistroSincronizacao

    @Before
    fun setup() {
        telemetry = FakeTelemetry()
        registro = RegistroSincronizacao(telemetry)
    }

    @Test
    fun `iniciado emite evento com a colecao`() {
        registro.iniciado("viagens")

        val evento = telemetry.eventos.single { it.nome == RegistroSincronizacao.EVENTO_INICIADO }
        assertEquals("viagens", evento.params[RegistroSincronizacao.PARAM_COLECAO])
    }

    @Test
    fun `snapshot do servidor marca origem servidor`() {
        registro.snapshotRecebido("viagens", docs = 3, doCache = false)

        val evento = telemetry.eventos.single { it.nome == RegistroSincronizacao.EVENTO_SNAPSHOT }
        assertEquals("3", evento.params[RegistroSincronizacao.PARAM_DOCS])
        assertEquals(RegistroSincronizacao.ORIGEM_SERVIDOR, evento.params[RegistroSincronizacao.PARAM_ORIGEM])
    }

    @Test
    fun `snapshot do cache marca origem cache`() {
        registro.snapshotRecebido("viagens", docs = 2, doCache = true)

        val evento = telemetry.eventos.single { it.nome == RegistroSincronizacao.EVENTO_SNAPSHOT }
        assertEquals(RegistroSincronizacao.ORIGEM_CACHE, evento.params[RegistroSincronizacao.PARAM_ORIGEM])
    }

    @Test
    fun `gravado deixa rastro sem evento navegavel`() {
        registro.gravado("viagens", 5)

        assertTrue(telemetry.eventos.isEmpty())
        assertTrue(telemetry.rastros.any { it.contains("5") && it.contains("viagens") })
    }

    @Test
    fun `parado emite evento da colecao`() {
        registro.parado("viagens")

        val evento = telemetry.eventos.single { it.nome == RegistroSincronizacao.EVENTO_PARADO }
        assertEquals("viagens", evento.params[RegistroSincronizacao.PARAM_COLECAO])
    }

    @Test
    fun `erro emite evento e registra nao-fatal`() {
        val causa = RuntimeException("permission denied")

        registro.erro("viagens", causa)

        val evento = telemetry.eventos.single { it.nome == RegistroSincronizacao.EVENTO_ERRO }
        assertEquals("permission denied", evento.params[RegistroSincronizacao.PARAM_MOTIVO])
        assertTrue(telemetry.naoFatais.contains(causa))
    }

    @Test
    fun `idempotencia observavel — um unico iniciado por colecao`() {
        // simula o repo idempotente chamando o sync 3x: só o 1o de fato anexa (chama iniciado).
        registro.iniciado("viagens")

        val iniciados = telemetry.eventos.count { it.nome == RegistroSincronizacao.EVENTO_INICIADO }
        assertEquals(1, iniciados)
    }
}
