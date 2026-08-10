package dev.matheus.fluviapp.domain.viagem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A hora como minutos-do-dia (decisão do analista, 2026-08-10): número por dentro, `HH:mm` na fronteira.
 */
class HoraDoDiaTest {

    // --- Exibição ---

    @Test
    fun `formata com dois digitos dos dois lados`() {
        assertEquals("00:00", formatarHora(0))
        assertEquals("08:05", formatarHora(8 * 60 + 5))
        assertEquals("18:30", formatarHora(18 * 60 + 30))
        assertEquals("23:59", formatarHora(MINUTOS_POR_DIA - 1))
    }

    /**
     * Acima de um dia o relógio **dá a volta**, porque é o que a chegada de uma travessia longa produz.
     * Quantos dias adiante é a `Chegada` quem diz — o relógio não tem esse dado.
     */
    @Test
    fun `hora acima de um dia da a volta no relogio`() {
        assertEquals("00:00", formatarHora(MINUTOS_POR_DIA))
        assertEquals("03:00", formatarHora(MINUTOS_POR_DIA + 3 * 60))
        assertEquals("21:00", formatarHora(2 * MINUTOS_POR_DIA + 21 * 60))
    }

    /** `floorMod`, e não `%`: um negativo com resto negativo formataria hora com sinal. */
    @Test
    fun `hora negativa tambem da a volta, sem sinal`() {
        assertEquals("23:00", formatarHora(-60))
    }

    // --- Leitura ---

    @Test
    fun `le a hora que a tela devolve`() {
        assertEquals(0, minutosDaHora("00:00"))
        assertEquals(18 * 60 + 30, minutosDaHora("18:30"))
        assertEquals(23 * 60 + 59, minutosDaHora("23:59"))
    }

    @Test
    fun `ignora espaco em volta`() {
        assertEquals(6 * 60, minutosDaHora("  06:00 "))
    }

    /**
     * **Hora pela metade é hora incompleta, não hora abreviada.** A versão anterior aceitava `"8:05"`
     * "porque é o que se digita" — com a máscara não é mais: quem digita `805` vê `"80:5"`. Manter a
     * tolerância abriria a armadilha oposta: parar em três dígitos e gravar `"18:3"` como **18:03**
     * quando se queria 18:30, sem avisar.
     */
    @Test
    fun `hora pela metade nao vale`() {
        assertNull(minutosDaHora("8:05"))
        assertNull(minutosDaHora("18:3"))
        assertNull(minutosDaHora("1:2"))
    }

    /**
     * `null` e não zero: `"00:00"` é meia-noite, horário legítimo de saída de balsa. Confundir os dois
     * faria o formulário aceitar campo vazio como madrugada.
     */
    @Test
    fun `texto que nao e hora devolve nulo, nao zero`() {
        assertNull(minutosDaHora(""))
        assertNull(minutosDaHora("   "))
        assertNull(minutosDaHora("1830"))
        assertNull(minutosDaHora("18:30:00"))
        assertNull(minutosDaHora("dezoito e meia"))
        assertNull(minutosDaHora("18:xx"))
    }

    /** Fora do relógio não é hora mal escrita — é outra coisa. */
    @Test
    fun `recusa hora fora do relogio`() {
        assertNull(minutosDaHora("24:00"))
        assertNull(minutosDaHora("18:60"))
        assertNull(minutosDaHora("-1:00"))
    }

    @Test
    fun `ida e volta preserva o valor`() {
        (0 until MINUTOS_POR_DIA step 7).forEach { minutos ->
            assertEquals(minutos, minutosDaHora(formatarHora(minutos)))
        }
    }

    // --- Os dígitos que a tela guarda ---

    @Test
    fun `o campo guarda so digito, e no maximo quatro`() {
        assertEquals("1830", digitosDaHora("18h30m"))
        assertEquals("1830", digitosDaHora("18:30"))
        assertEquals("", digitosDaHora("abc"))
    }

    /** Digitar rápido demais não deve mudar o que já está certo: o excedente é descartado. */
    @Test
    fun `o quinto digito e descartado, nao empurra os outros`() {
        assertEquals("1830", digitosDaHora("18305"))
    }

    @Test
    fun `os digitos viram minuto sem passar pela tela`() {
        assertEquals(18 * 60 + 30, minutosDosDigitos("1830"))
        assertEquals(0, minutosDosDigitos("0000"))
        assertNull(minutosDosDigitos("183"))
        assertNull(minutosDosDigitos(""))
    }

    // --- A máscara ---

    /**
     * Ela existe porque **o teclado numérico do Android não tem `:`** (achado em homologação): o campo
     * pedia `HH:mm` e oferecia um teclado onde o separador não existe. Hoje ela é de **exibição** — o
     * valor guardado são os dígitos, e o `:` é desenhado pela `HoraVisualTransformation`.
     */
    @Test
    fun `a mascara escreve o separador enquanto se digita`() {
        assertEquals("", mascararHora(""))
        assertEquals("1", mascararHora("1"))
        assertEquals("18", mascararHora("18"))
        assertEquals("18:3", mascararHora("183"))
        assertEquals("18:30", mascararHora("1830"))
    }

    @Test
    fun `a mascara para em quatro digitos`() {
        assertEquals("18:30", mascararHora("18305"))
        assertEquals("18:30", mascararHora("1830999"))
    }

    /**
     * Apagar não tem regra à parte: some um dígito do valor, a máscara reescreve o que sobrou, e o `:`
     * desaparece sozinho quando restam dois.
     */
    @Test
    fun `apagar desfaz pelo mesmo caminho`() {
        assertEquals("18:3", mascararHora("183"))
        assertEquals("18", mascararHora("18"))
        assertEquals("1", mascararHora("1"))
    }

    /**
     * **O comprimento é o que o `OffsetMapping` da tela assume**: um caractere a mais a partir do
     * terceiro dígito, e nenhum antes disso. Se esta relação mudar, o cursor volta a errar — e o mapa,
     * que é o que traduz as duas réguas, passa a mentir.
     */
    @Test
    fun `a mascara acrescenta exatamente um caractere, e so a partir do terceiro digito`() {
        listOf("", "1", "18").forEach { assertEquals(it.length, mascararHora(it).length) }
        listOf("183", "1830").forEach { assertEquals(it.length + 1, mascararHora(it).length) }
        assertEquals(2, mascararHora("1830").indexOf(':'))
    }

    /** O que a máscara completa, o leitor aceita: é o contrato entre os dois. */
    @Test
    fun `o que a mascara completa, minutosDaHora le`() {
        (0 until MINUTOS_POR_DIA step 13).forEach { minutos ->
            val digitado = formatarHora(minutos).filter { it.isDigit() }
            assertEquals(minutos, minutosDaHora(mascararHora(digitado)))
        }
    }

    // --- Faixa ---

    @Test
    fun `hora valida e a que cabe num dia`() {
        assertTrue(horaValida(0))
        assertTrue(horaValida(MINUTOS_POR_DIA - 1))
        assertFalse(horaValida(-1))
        assertFalse(horaValida(MINUTOS_POR_DIA))
    }
}