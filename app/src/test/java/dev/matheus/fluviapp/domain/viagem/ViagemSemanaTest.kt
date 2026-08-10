package dev.matheus.fluviapp.domain.viagem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * A **ocorrência concreta** da viagem — `viagem_semana` (decisão do analista, 2026-08-10).
 *
 * A viagem é a regra ("terça às 18h"); a ocorrência é o acontecimento ("terça, 11/08, às 18h"). O que
 * estes casos travam é a janela deslizante e o significado de *disponível*: a saída de hoje de manhã não
 * está disponível esta tarde.
 */
class ViagemSemanaTest {

    /** Terça, 11 de agosto de 2026. */
    private val terca = LocalDate.of(2026, 8, 11)

    private fun viagem(
        id: String = "v1",
        dia: DayOfWeek = DayOfWeek.TUESDAY,
        horaMin: Int = 18 * 60,
        ativo: Boolean = true,
    ) = Viagem(id, "r1", "e1", dia, horaMin, ativo = ativo)

    private fun agoraEm(data: LocalDate, hora: Int = 0, minuto: Int = 0): LocalDateTime =
        data.atTime(hora, minuto)

    // --- A expansão ---

    @Test
    fun `a viagem semanal ocorre uma vez na janela de sete dias`() {
        val ocorrencias = listOf(viagem()).disponiveisAPartirDe(agoraEm(terca))

        assertEquals(1, ocorrencias.size)
        assertEquals(terca, ocorrencias.single().data)
        assertEquals(terca.atTime(18, 0), ocorrencias.single().partida)
    }

    /** Terça e sexta são **duas viagens**, e cada uma ocorre uma vez na semana. */
    @Test
    fun `duas viagens semanais dao duas ocorrencias`() {
        val viagens = listOf(viagem(id = "v1"), viagem(id = "v2", dia = DayOfWeek.FRIDAY, horaMin = 6 * 60))

        val ocorrencias = viagens.disponiveisAPartirDe(agoraEm(terca))

        assertEquals(listOf("v1", "v2"), ocorrencias.map { it.viagem.id })
    }

    @Test
    fun `ordena pela partida, nao pela ordem do cadastro`() {
        val viagens = listOf(
            viagem(id = "sexta", dia = DayOfWeek.FRIDAY, horaMin = 6 * 60),
            viagem(id = "terca", dia = DayOfWeek.TUESDAY, horaMin = 18 * 60),
        )

        assertEquals(
            listOf("terca", "sexta"),
            viagens.disponiveisAPartirDe(agoraEm(terca)).map { it.viagem.id },
        )
    }

    @Test
    fun `a ocorrencia se identifica por viagem e data`() {
        val ocorrencia = listOf(viagem()).disponiveisAPartirDe(agoraEm(terca)).single()

        assertEquals("v1@2026-08-11", ocorrencia.id)
    }

    // --- O que "disponível" quer dizer ---

    /**
     * O caso que justifica a conta ser sobre o **instante**: às 18h de terça, a saída das 06h de terça já
     * partiu. Tratá-la como disponível porque "é hoje" ofereceria um barco que não está mais no cais.
     */
    @Test
    fun `a saida de hoje que ja partiu nao esta disponivel`() {
        val manha = viagem(horaMin = 6 * 60)

        assertTrue(listOf(manha).disponiveisAPartirDe(agoraEm(terca, hora = 18)).isEmpty())
    }

    @Test
    fun `a saida de hoje que ainda vai partir esta disponivel`() {
        val noite = viagem(horaMin = 18 * 60)

        assertEquals(1, listOf(noite).disponiveisAPartirDe(agoraEm(terca, hora = 6)).size)
    }

    /** A partida no minuto exato ainda conta — quem chegou na hora embarca. */
    @Test
    fun `a saida no instante exato ainda esta disponivel`() {
        val ocorrencias = listOf(viagem(horaMin = 18 * 60))
            .disponiveisAPartirDe(agoraEm(terca, hora = 18, minuto = 0))

        assertEquals(1, ocorrencias.size)
    }

    /**
     * **A janela é deslizante**, e não a semana do calendário: no sábado, uma semana fixa de segunda a
     * domingo mostraria dois dias. A agência precisa ver sempre a mesma quantidade de futuro.
     */
    @Test
    fun `no fim da semana a janela continua com sete dias de futuro`() {
        val sabado = LocalDate.of(2026, 8, 15)
        val todosOsDias = DIAS_DA_SEMANA.mapIndexed { i, dia -> viagem(id = "v$i", dia = dia) }

        assertEquals(7, todosOsDias.disponiveisAPartirDe(agoraEm(sabado)).size)
    }

    /** A viagem de hoje **não** aparece duas vezes: a janela cobre sete dias, não oito. */
    @Test
    fun `a mesma viagem nao aparece duas vezes na janela`() {
        val ocorrencias = listOf(viagem()).disponiveisAPartirDe(agoraEm(terca))

        assertEquals(1, ocorrencias.size)
    }

    @Test
    fun `a janela menor mostra menos dias`() {
        val quinta = viagem(dia = DayOfWeek.THURSDAY)

        assertTrue(listOf(quinta).disponiveisAPartirDe(agoraEm(terca), dias = 2).isEmpty())
        assertEquals(1, listOf(quinta).disponiveisAPartirDe(agoraEm(terca), dias = 3).size)
    }

    @Test
    fun `janela zero ou negativa nao devolve nada`() {
        assertTrue(listOf(viagem()).disponiveisAPartirDe(agoraEm(terca), dias = 0).isEmpty())
        assertTrue(listOf(viagem()).disponiveisAPartirDe(agoraEm(terca), dias = -1).isEmpty())
    }

    /** Viagem inativada é registro do passado — ofertá-la seria vender o que foi encerrado. */
    @Test
    fun `viagem inativa nao gera ocorrencia`() {
        assertTrue(listOf(viagem(ativo = false)).disponiveisAPartirDe(agoraEm(terca)).isEmpty())
    }

    @Test
    fun `sem viagem nao ha ocorrencia`() {
        assertTrue(emptyList<Viagem>().disponiveisAPartirDe(agoraEm(terca)).isEmpty())
    }
}