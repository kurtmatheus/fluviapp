package dev.matheus.fluviapp.ui.viewmodel.helpers.viagem

import dev.matheus.fluviapp.domain.viagem.Viagem
import dev.matheus.fluviapp.domain.viagem.rotulo
import dev.matheus.fluviapp.ui.states.EmbarcacaoOpcao
import dev.matheus.fluviapp.ui.states.ErroHoraViagem
import dev.matheus.fluviapp.ui.states.FormViagemUiState
import dev.matheus.fluviapp.ui.states.RotaOpcao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

/**
 * Validação pura do formulário de viagem. O que estes casos travam é a **ordem** das queixas da hora e o
 * fato de a duplicidade ser do conjunto, não do campo.
 */
class ValidacaoViagemTest {

    private val rotaOpcao = RotaOpcao("r1", "Porto A · Belém/PA → Porto B · Parintins/AM")
    private val embarcacaoOpcao = EmbarcacaoOpcao("e1", "F/B Modelo")
    private val terca = DayOfWeek.TUESDAY

    private fun estado(
        rota: String = rotaOpcao.rotulo,
        embarcacao: String = embarcacaoOpcao.rotulo,
        dia: String = terca.rotulo,
        hora: String = "18:00",
        existentes: List<Viagem> = emptyList(),
    ) = FormViagemUiState(
        rota = rota,
        embarcacao = embarcacao,
        diaSemana = dia,
        hora = hora,
        rotas = listOf(rotaOpcao),
        embarcacoes = listOf(embarcacaoOpcao),
        viagensExistentes = existentes,
    )

    private fun viagem(
        id: String = "v1",
        dia: DayOfWeek = terca,
        horaMin: Int = 18 * 60,
        ativo: Boolean = true,
    ) = Viagem(
        id = id,
        rotaId = rotaOpcao.id,
        embarcacaoId = embarcacaoOpcao.id,
        diaSemana = dia,
        horaMin = horaMin,
        ativo = ativo,
    )

    // --- Caminho feliz ---

    @Test
    fun `formulario completo e valido`() {
        assertTrue(validarViagem(estado()).valido)
    }

    @Test
    fun `a chave sai dos quatro campos, ja em id e minuto`() {
        assertEquals(
            Viagem.Chave(rotaOpcao.id, embarcacaoOpcao.id, terca, 18 * 60),
            chaveDaViagem(estado()),
        )
    }

    // --- Escolhas de dropdown ---

    @Test
    fun `rota nao escolhida e erro`() {
        assertTrue(validarViagem(estado(rota = "")).rota)
    }

    /**
     * Texto que **não está na lista** conta como não escolhido — e não como escolha estranha a validar
     * depois: as opções já chegam recortadas pela concessão, então fora da lista é fora do alcance.
     */
    @Test
    fun `rota fora da lista conta como nao escolhida`() {
        assertTrue(validarViagem(estado(rota = "Rota de outra empresa")).rota)
    }

    @Test
    fun `embarcacao nao escolhida e erro`() {
        assertTrue(validarViagem(estado(embarcacao = "")).embarcacao)
        assertTrue(validarViagem(estado(embarcacao = "F/B Alheio")).embarcacao)
    }

    @Test
    fun `dia nao escolhido e erro`() {
        assertTrue(validarViagem(estado(dia = "")).diaSemana)
        assertTrue(validarViagem(estado(dia = "Terça")).diaSemana)
    }

    // --- A hora, e a ordem das três queixas ---

    @Test
    fun `hora vazia e obrigatoria`() {
        assertEquals(ErroHoraViagem.OBRIGATORIA, validarViagem(estado(hora = "")).hora)
    }

    @Test
    fun `hora ilegivel e invalida`() {
        assertEquals(ErroHoraViagem.INVALIDA, validarViagem(estado(hora = "18h")).hora)
        assertEquals(ErroHoraViagem.INVALIDA, validarViagem(estado(hora = "25:00")).hora)
    }

    @Test
    fun `saida ja existente e duplicada`() {
        val erros = validarViagem(estado(existentes = listOf(viagem())))

        assertEquals(ErroHoraViagem.DUPLICADA, erros.hora)
        assertFalse(erros.valido)
    }

    /**
     * Acusar duplicidade antes de saber ler a hora seria comparar com uma chave incompleta — e a queixa
     * errada manda corrigir o que já está certo.
     */
    @Test
    fun `hora ilegivel vence a duplicidade`() {
        val erros = validarViagem(estado(hora = "18h", existentes = listOf(viagem())))

        assertEquals(ErroHoraViagem.INVALIDA, erros.hora)
    }

    /**
     * Com um dropdown por escolher ainda não se sabe **qual** saída é esta, e "já existe" seria dito
     * sobre uma partida que não está determinada.
     */
    @Test
    fun `sem rota escolhida, a hora nao acusa duplicidade`() {
        val erros = validarViagem(estado(rota = "", existentes = listOf(viagem())))

        assertEquals(ErroHoraViagem.NENHUM, erros.hora)
        assertTrue(erros.rota)
        assertNull(chaveDaViagem(estado(rota = "")))
    }

    // --- A duplicidade é do conjunto ---

    @Test
    fun `mesma saida em outro dia nao e duplicada`() {
        val existentes = listOf(viagem(dia = DayOfWeek.FRIDAY))

        assertTrue(validarViagem(estado(existentes = existentes)).valido)
    }

    @Test
    fun `mesma saida em outra hora nao e duplicada`() {
        val existentes = listOf(viagem(horaMin = 6 * 60))

        assertTrue(validarViagem(estado(existentes = existentes)).valido)
    }

    /** Recriar o que se acabou de inativar é o **único** jeito de corrigir — não há edição. */
    @Test
    fun `viagem inativada nao bloqueia a recriacao`() {
        val existentes = listOf(viagem(ativo = false))

        assertTrue(validarViagem(estado(existentes = existentes)).valido)
    }
}