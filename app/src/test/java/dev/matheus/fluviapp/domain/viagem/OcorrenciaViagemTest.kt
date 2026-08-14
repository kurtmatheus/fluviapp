package dev.matheus.fluviapp.domain.viagem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * A ocorrência `(viagemId, data)` — a travessia concreta para onde a passagem aponta (ADR-0023 D2).
 *
 * O que se cobra aqui é a **data como texto ISO** (ADR-0024 D2): ela ordena cronologicamente ao ordenar
 * lexicograficamente, e é isso que faz faixa por período funcionar sem truque. E a fronteira é **fail-closed**:
 * uma ocorrência sem data não é uma ocorrência "de hoje" — não é nada.
 *
 * **De volta ao escopo na F9.6**: o portador acendeu (ver `AcomodacaoTest`).
 */
class OcorrenciaViagemTest {

    @Test
    fun `dataIso e a data em ISO-8601`() {
        val ocorrencia = OcorrenciaViagem("v1", LocalDate.of(2026, 8, 18))

        assertEquals("2026-08-18", ocorrencia.dataIso)
    }

    /** Zeros à esquerda são o que faz o texto ordenar como data. */
    @Test
    fun `mes e dia de um digito levam zero`() {
        val ocorrencia = OcorrenciaViagem("v1", LocalDate.of(2026, 1, 5))

        assertEquals("2026-01-05", ocorrencia.dataIso)
    }

    /**
     * A propriedade que justifica o formato: comparar texto ISO dá a mesma ordem que comparar data. Com
     * `dd/MM/yyyy` — o formato que o carimbo de embarque usa hoje — `18/08/2026` viria antes de `05/09/2026`.
     */
    @Test
    fun `ordem lexicografica do texto e a ordem cronologica`() {
        val datas = listOf(
            LocalDate.of(2026, 9, 5),
            LocalDate.of(2026, 8, 18),
            LocalDate.of(2027, 1, 1),
        )

        val ordenadasPorTexto = datas.map { OcorrenciaViagem("v1", it).dataIso }.sorted()

        assertEquals(listOf("2026-08-18", "2026-09-05", "2027-01-01"), ordenadasPorTexto)
    }

    @Test
    fun `chave junta viagem e data`() {
        val ocorrencia = OcorrenciaViagem("v_abc", LocalDate.of(2026, 8, 18))

        assertEquals("v_abc@2026-08-18", ocorrencia.chave)
    }

    @Test
    fun `de le viagem e data ISO`() {
        val ocorrencia = OcorrenciaViagem.de("v1", "2026-08-18")

        assertEquals(OcorrenciaViagem("v1", LocalDate.of(2026, 8, 18)), ocorrencia)
    }

    @Test
    fun `de recusa sem viagem`() {
        assertNull(OcorrenciaViagem.de(null, "2026-08-18"))
        assertNull(OcorrenciaViagem.de("", "2026-08-18"))
        assertNull(OcorrenciaViagem.de("   ", "2026-08-18"))
    }

    /** Inclui o formato brasileiro: quem gravar `18/08/2026` aqui é recusado, não adivinhado. */
    @Test
    fun `de recusa data ilegivel`() {
        assertNull(OcorrenciaViagem.de("v1", null))
        assertNull(OcorrenciaViagem.de("v1", ""))
        assertNull(OcorrenciaViagem.de("v1", "18/08/2026"))
        assertNull(OcorrenciaViagem.de("v1", "2026-13-01"))
    }
}
