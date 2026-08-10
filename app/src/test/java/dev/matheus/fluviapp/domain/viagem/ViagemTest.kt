package dev.matheus.fluviapp.domain.viagem

import dev.matheus.fluviapp.domain.rota.Rota
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

/**
 * A Viagem (ADR-0016 §7.1, F8) — a **partida física**, atômica no par `(dia, hora)` sobre uma rota.
 */
class ViagemTest {

    private fun viagem(
        id: String = "v1",
        rotaId: String = "rota-1",
        embarcacaoId: String = "emb-1",
        dia: DayOfWeek = DayOfWeek.TUESDAY,
        horaMin: Int = 18 * 60,
        ativo: Boolean = true,
    ) = Viagem(
        id = id,
        rotaId = rotaId,
        embarcacaoId = embarcacaoId,
        diaSemana = dia,
        horaMin = horaMin,
        ativo = ativo,
    )

    private fun rota(tempoMedioH: Double) = Rota(
        id = "rota-1",
        portoOrigemId = "porto-a",
        portoDestinoId = "porto-b",
        tempoMedioH = tempoMedioH,
    )

    // --- Sentido ---

    @Test
    fun `rota, embarcacao e hora do relogio fazem uma partida`() {
        assertTrue(viagem().temSentido())
    }

    @Test
    fun `partida sem rota ou sem embarcacao nao tem sentido`() {
        assertFalse(viagem(rotaId = "").temSentido())
        assertFalse(viagem(embarcacaoId = "").temSentido())
    }

    @Test
    fun `hora fora do relogio nao tem sentido`() {
        assertFalse(viagem(horaMin = -1).temSentido())
        assertFalse(viagem(horaMin = MINUTOS_POR_DIA).temSentido())
    }

    // --- Chave e duplicidade ---

    /**
     * O que faz duas viagens serem a mesma saída são os **quatro** campos. Trocar qualquer um produz
     * outra partida — é o que impede "terça e sexta" de virar um documento só.
     */
    @Test
    fun `a chave sao os quatro campos da partida`() {
        val base = viagem()
        assertEquals(base.chave, viagem(id = "outro-id").chave)

        assertNotEquals(base.chave, viagem(rotaId = "rota-2").chave)
        assertNotEquals(base.chave, viagem(embarcacaoId = "emb-2").chave)
        assertNotEquals(base.chave, viagem(dia = DayOfWeek.FRIDAY).chave)
        assertNotEquals(base.chave, viagem(horaMin = 19 * 60).chave)
    }

    @Test
    fun `encontra a viagem ativa com a mesma chave`() {
        val existente = viagem(id = "v1")
        val lista = listOf(existente, viagem(id = "v2", dia = DayOfWeek.FRIDAY))

        assertEquals(existente, lista.ativaComChave(viagem(id = "nova").chave))
    }

    /**
     * Duplicidade só entre **ativas**: recusar por causa de uma inativada impediria de recriar
     * exatamente o que se acabou de corrigir — que é o único jeito de corrigir, já que não há edição.
     */
    @Test
    fun `viagem inativada nao bloqueia a recriacao da mesma partida`() {
        val lista = listOf(viagem(id = "v1", ativo = false))

        assertNull(lista.ativaComChave(viagem(id = "nova").chave))
    }

    @Test
    fun `pool vazio nao tem duplicidade`() {
        assertNull(emptyList<Viagem>().ativaComChave(viagem().chave))
    }

    // --- Chegada estimada ---

    @Test
    fun `chegada no mesmo dia soma as horas`() {
        val chegada = viagem(dia = DayOfWeek.TUESDAY, horaMin = 8 * 60).chegadaEstimada(rota(6.0))

        assertEquals(14 * 60, chegada.horaMin)
        assertEquals(0, chegada.diasDepois)
        assertEquals(DayOfWeek.TUESDAY, chegada.diaSemana)
    }

    /** Meia hora é `0.5` — e é por isso que o tempo é decimal e a hora é número. */
    @Test
    fun `tempo medio fracionado vira minutos`() {
        val chegada = viagem(horaMin = 8 * 60).chegadaEstimada(rota(2.5))

        assertEquals(10 * 60 + 30, chegada.horaMin)
    }

    /**
     * A travessia longa do Amazonas é o caso comum, não a exceção: 30 horas saindo às 18h de terça chega
     * às 00h de **quinta**. Mostrar só "00:00" seria esconder o dado que decide a viagem de quem compra.
     *
     * E é a borda da meia-noite: `18:00 + 30h` dá 2880 minutos, dois dias **exatos** — a chegada cai no
     * primeiro instante de quinta, e `diasDepois` conta 2, não 1. Contar pelo relógio (0h "ainda parece
     * de madrugada") erraria o dia impresso no bilhete por um.
     */
    @Test
    fun `travessia longa chega em outro dia da semana`() {
        val chegada = viagem(dia = DayOfWeek.TUESDAY, horaMin = 18 * 60).chegadaEstimada(rota(30.0))

        assertEquals(0, chegada.horaMin)
        assertEquals(2, chegada.diasDepois)
        assertEquals(DayOfWeek.THURSDAY, chegada.diaSemana)
    }

    /** Uma hora a menos e a chegada é de véspera — o par que prova que a borda acima não é acaso. */
    @Test
    fun `uma hora antes da borda, a chegada ainda e de quarta`() {
        val chegada = viagem(dia = DayOfWeek.TUESDAY, horaMin = 18 * 60).chegadaEstimada(rota(29.0))

        assertEquals(23 * 60, chegada.horaMin)
        assertEquals(1, chegada.diasDepois)
        assertEquals(DayOfWeek.WEDNESDAY, chegada.diaSemana)
    }

    @Test
    fun `chegada atravessa o fim da semana sem quebrar`() {
        val chegada = viagem(dia = DayOfWeek.SUNDAY, horaMin = 20 * 60).chegadaEstimada(rota(10.0))

        assertEquals(6 * 60, chegada.horaMin)
        assertEquals(1, chegada.diasDepois)
        assertEquals(DayOfWeek.MONDAY, chegada.diaSemana)
    }

    @Test
    fun `travessia de varios dias conta os dias`() {
        val chegada = viagem(dia = DayOfWeek.MONDAY, horaMin = 12 * 60).chegadaEstimada(rota(72.0))

        assertEquals(12 * 60, chegada.horaMin)
        assertEquals(3, chegada.diasDepois)
        assertEquals(DayOfWeek.THURSDAY, chegada.diaSemana)
    }

    /** Rota sem tempo medido: a chegada é a saída, e não uma hora inventada. */
    @Test
    fun `rota sem tempo medio nao move a chegada`() {
        val chegada = viagem(horaMin = 9 * 60).chegadaEstimada(rota(0.0))

        assertEquals(9 * 60, chegada.horaMin)
        assertEquals(0, chegada.diasDepois)
    }
}