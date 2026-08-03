package dev.matheus.fluviapp.telemetry

import dev.matheus.fluviapp.revitalizacao.ForaDoEscopo
import org.junit.experimental.categories.Category
import dev.matheus.fluviapp.exceptions.EmissaoException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@Category(ForaDoEscopo::class)
class RegistroEmissaoTest {

    private lateinit var telemetry: FakeTelemetry
    private lateinit var registro: RegistroEmissao

    @Before
    fun setUp() {
        telemetry = FakeTelemetry()
        registro = RegistroEmissao(telemetry)
    }

    @Test
    fun `salvaLocal emite sucesso local com numero e fase, e deixa breadcrumb`() {
        registro.salvaLocal("2444")

        val evento = telemetry.eventos.single()
        assertEquals(RegistroEmissao.EVENTO_SALVA, evento.nome)
        assertEquals("2444", evento.params[RegistroEmissao.PARAM_NUMERO])
        assertEquals(RegistroEmissao.FASE_LOCAL, evento.params[RegistroEmissao.PARAM_FASE])
        assertEquals(1, telemetry.rastros.size)
        assertTrue(telemetry.naoFatais.isEmpty())
    }

    @Test
    fun `sincronizou emite evento de sincronizacao sem nao-fatal`() {
        registro.sincronizou("2444")

        assertEquals(listOf(RegistroEmissao.EVENTO_SINCRONIZADA), telemetry.nomesDeEventos())
        assertTrue(telemetry.naoFatais.isEmpty())
    }

    @Test
    fun `pendenteDeSync e warning - evento com motivo mais nao-fatal de transmissao`() {
        registro.pendenteDeSync("2444", RuntimeException("sem rede"))

        val evento = telemetry.eventos.single()
        assertEquals(RegistroEmissao.EVENTO_PENDENTE_SYNC, evento.nome)
        assertEquals("sem rede", evento.params[RegistroEmissao.PARAM_MOTIVO])
        assertEquals(1, telemetry.naoFatais.size)
        assertTrue(telemetry.naoFatais.single() is EmissaoException.FalhaNaTransmissao)
    }

    @Test
    fun `falhou emite evento _falha do motivo mais nao-fatal`() {
        registro.falhou(EmissaoException.FalhaAoPersistir(IllegalStateException("db")), "2444")

        val evento = telemetry.eventos.single()
        assertEquals("passagem_persistencia" + RegistroEmissao.SUFIXO_FALHA, evento.nome)
        assertEquals(1, telemetry.naoFatais.size)
        assertTrue(telemetry.naoFatais.single() is EmissaoException.FalhaAoPersistir)
    }
}
