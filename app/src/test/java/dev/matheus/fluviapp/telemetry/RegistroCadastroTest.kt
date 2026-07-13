package dev.matheus.fluviapp.telemetry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RegistroCadastroTest {

    private lateinit var telemetry: FakeTelemetry
    private lateinit var registro: RegistroCadastro

    @Before
    fun setUp() {
        telemetry = FakeTelemetry()
        registro = RegistroCadastro(telemetry)
    }

    @Test
    fun `salvou emite sucesso com entidade e id, deixa rastro e nenhum nao-fatal`() {
        registro.salvou("empresa", "abc123")

        val evento = telemetry.eventos.single()
        assertEquals(RegistroCadastro.EVENTO_SALVO, evento.nome)
        assertEquals("empresa", evento.params[RegistroCadastro.PARAM_ENTIDADE])
        assertEquals("abc123", evento.params[RegistroCadastro.PARAM_ID])
        assertEquals(1, telemetry.rastros.size)
        assertTrue(telemetry.naoFatais.isEmpty())
    }

    @Test
    fun `pendenteDeSync emite warning com motivo e registra nao-fatal, sem falha`() {
        val causa = RuntimeException("offline")

        registro.pendenteDeSync("empresa", "abc123", causa)

        val evento = telemetry.eventos.single()
        assertEquals(RegistroCadastro.EVENTO_PENDENTE_SYNC, evento.nome)
        assertEquals("empresa", evento.params[RegistroCadastro.PARAM_ENTIDADE])
        assertEquals("abc123", evento.params[RegistroCadastro.PARAM_ID])
        assertEquals("offline", evento.params[RegistroCadastro.PARAM_MOTIVO])
        assertEquals(listOf<Throwable>(causa), telemetry.naoFatais)
    }

    @Test
    fun `pendenteDeSync sem mensagem usa motivo desconhecido`() {
        registro.pendenteDeSync("empresa", "abc123", RuntimeException())

        val evento = telemetry.eventos.single()
        assertEquals(RegistroCadastro.DESCONHECIDO, evento.params[RegistroCadastro.PARAM_MOTIVO])
    }

    @Test
    fun `falhou emite falha com motivo e registra nao-fatal`() {
        val erro = IllegalStateException("Room caiu")

        registro.falhou("empresa", erro)

        val evento = telemetry.eventos.single()
        assertEquals(RegistroCadastro.EVENTO_FALHA, evento.nome)
        assertEquals("empresa", evento.params[RegistroCadastro.PARAM_ENTIDADE])
        assertEquals("Room caiu", evento.params[RegistroCadastro.PARAM_MOTIVO])
        assertEquals(listOf<Throwable>(erro), telemetry.naoFatais)
    }
}
