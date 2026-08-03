package dev.matheus.fluviapp.domain.documento

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tipo de documento como tipo de domínio (ADR-0020 D2): fronteira String→enum, formatação progressiva,
 * formatação de exibição, **ocultação parcial (LGPD)** e validação de dígito verificador.
 *
 * CPF `529.982.247-25` e CNPJ `11.222.333/0002-62` são válidos de verdade — os DVs conferem. O CNPJ foi
 * escolhido com filial `0002` de propósito: é o caso que o `formatarCNPJ()` antigo errava.
 */
class TipoDocumentoTest {

    private val cpfValido = "52998224725"
    private val cnpjValido = "11222333000262"

    // --- de(): fronteira String -> enum ---

    @Test
    fun `de converte o name canonico dos cinco tipos`() {
        assertEquals(TipoDocumento.CPF, TipoDocumento.de("CPF"))
        assertEquals(TipoDocumento.CNPJ, TipoDocumento.de("CNPJ"))
        assertEquals(TipoDocumento.RG, TipoDocumento.de("RG"))
        assertEquals(TipoDocumento.CNH, TipoDocumento.de("CNH"))
        assertEquals(TipoDocumento.PASSAPORTE, TipoDocumento.de("PASSAPORTE"))
    }

    @Test
    fun `de tolera espaco e caixa`() {
        assertEquals(TipoDocumento.CPF, TipoDocumento.de(" cpf "))
        assertEquals(TipoDocumento.PASSAPORTE, TipoDocumento.de("Passaporte"))
    }

    @Test
    fun `de retorna null para desconhecido ou nulo (fail-closed)`() {
        assertNull(TipoDocumento.de(null))
        assertNull(TipoDocumento.de(""))
        // O caso que motivou o ADR: item que um administrador cadastraria no catálogo.
        assertNull(TipoDocumento.de("RNE"))
    }

    // --- normalizar(): a forma canonica ---

    @Test
    fun `normalizar descarta separadores e mantem so os digitos`() {
        assertEquals(cpfValido, TipoDocumento.CPF.normalizar("529.982.247-25"))
        assertEquals(cnpjValido, TipoDocumento.CNPJ.normalizar("11.222.333/0002-62"))
    }

    @Test
    fun `normalizar do passaporte mantem letras e sobe a caixa`() {
        assertEquals("AB123456", TipoDocumento.PASSAPORTE.normalizar("ab-123456"))
    }

    @Test
    fun `normalizar aceita nulo e vazio sem lancar`() {
        assertEquals("", TipoDocumento.CPF.normalizar(null))
        assertEquals("", TipoDocumento.CPF.normalizar(""))
    }

    // --- formatarProgressivo(): o campo enquanto se digita ---

    @Test
    fun `formatarProgressivo do cpf acompanha a digitacao`() {
        assertEquals("", TipoDocumento.CPF.formatarProgressivo(""))
        assertEquals("529", TipoDocumento.CPF.formatarProgressivo("529"))
        assertEquals("529.9", TipoDocumento.CPF.formatarProgressivo("5299"))
        assertEquals("529.982.247", TipoDocumento.CPF.formatarProgressivo("529982247"))
        assertEquals("529.982.247-25", TipoDocumento.CPF.formatarProgressivo(cpfValido))
    }

    @Test
    fun `formatarProgressivo do passaporte separa apos duas letras`() {
        assertEquals("AB", TipoDocumento.PASSAPORTE.formatarProgressivo("AB"))
        assertEquals("AB-1", TipoDocumento.PASSAPORTE.formatarProgressivo("AB1"))
        assertEquals("AB-123456", TipoDocumento.PASSAPORTE.formatarProgressivo("AB123456"))
    }

    @Test
    fun `rg e cnh nao tem separador`() {
        assertEquals("123456789", TipoDocumento.RG.formatarProgressivo("123456789"))
        assertEquals("12345678901", TipoDocumento.CNH.formatarProgressivo("12345678901"))
    }

    // --- formatar(): exibicao ---

    @Test
    fun `formatar o cnpj respeita a filial — o codigo antigo fixava 0001`() {
        assertEquals("11.222.333/0002-62", TipoDocumento.CNPJ.formatar(cnpjValido))
    }

    @Test
    fun `formatar documento incompleto devolve o valor cru em vez de sumir com ele`() {
        // O `extrairDocumentoFormatado` antigo devolvia "" para tipo desconhecido: o documento
        // desaparecia do bilhete sem erro e sem log.
        assertEquals("529", TipoDocumento.CPF.formatar("529"))
        assertEquals("", TipoDocumento.CPF.formatar(null))
    }

    @Test
    fun `formatar nunca lanca em entrada curta`() {
        TipoDocumento.entries.forEach { tipo ->
            listOf(null, "", "1", "ab", "1234567890123456789").forEach { entrada ->
                tipo.formatar(entrada)
                tipo.formatarProgressivo(entrada)
                tipo.mascarar(entrada)
            }
        }
    }

    // --- mascarar(): a politica de exibicao (LGPD) ---

    @Test
    fun `mascarar o cpf esconde os 6 primeiros e mostra os 5 ultimos`() {
        assertEquals("###.###.247-25", TipoDocumento.CPF.mascarar(cpfValido))
    }

    @Test
    fun `a mascara do cpf expoe 5 digitos, nao 6 — e nenhum dos 6 primeiros`() {
        val mascarado = TipoDocumento.CPF.mascarar(cpfValido)
        assertEquals(5, mascarado.count { it.isDigit() })
        assertEquals(cpfValido.takeLast(5), mascarado.filter { it.isDigit() })
    }

    @Test
    fun `mascarar o rg e a cnh escondem o miolo`() {
        assertEquals("1###56789", TipoDocumento.RG.mascarar("123456789"))
        assertEquals("12######901", TipoDocumento.CNH.mascarar("12345678901"))
    }

    @Test
    fun `mascarar o passaporte esconde tres caracteres do numero`() {
        assertEquals("AB-1###56", TipoDocumento.PASSAPORTE.mascarar("AB123456"))
    }

    @Test
    fun `cnpj nao e ocultado — identifica pessoa juridica, nao e dado pessoal`() {
        assertEquals(
            TipoDocumento.CNPJ.formatar(cnpjValido),
            TipoDocumento.CNPJ.mascarar(cnpjValido),
        )
    }

    @Test
    fun `exibir escolhe entre formatar e mascarar`() {
        assertEquals("529.982.247-25", TipoDocumento.CPF.exibir(cpfValido, ocultar = false))
        assertEquals("###.###.247-25", TipoDocumento.CPF.exibir(cpfValido, ocultar = true))
    }

    // --- validar(): o lugar que nao existia ---

    @Test
    fun `cpf valido passa`() {
        assertTrue(TipoDocumento.CPF.validar(cpfValido))
        assertTrue(TipoDocumento.CPF.validar("529.982.247-25"))
    }

    @Test
    fun `cpf com digito verificador errado falha`() {
        assertFalse(TipoDocumento.CPF.validar("52998224726"))
        assertFalse(TipoDocumento.CPF.validar("12345678900"))
    }

    @Test
    fun `cpf de digitos repetidos falha — inclusive o do SampleData`() {
        assertFalse(TipoDocumento.CPF.validar("00000000000"))
        assertFalse(TipoDocumento.CPF.validar("11111111111"))
    }

    @Test
    fun `cpf incompleto falha`() {
        assertFalse(TipoDocumento.CPF.validar("5299822472"))
        assertFalse(TipoDocumento.CPF.validar(null))
    }

    @Test
    fun `cnpj valido passa e invalido falha`() {
        assertTrue(TipoDocumento.CNPJ.validar(cnpjValido))
        assertTrue(TipoDocumento.CNPJ.validar("11.222.333/0001-81"))
        assertFalse(TipoDocumento.CNPJ.validar("11222333000263"))
        assertFalse(TipoDocumento.CNPJ.validar("00000000000000"))
    }

    @Test
    fun `rg cnh e passaporte valem pelo comprimento — nao ha regra nacional unica`() {
        assertTrue(TipoDocumento.RG.validar("12345"))
        assertFalse(TipoDocumento.RG.validar("1234"))
        assertTrue(TipoDocumento.CNH.validar("12345678901"))
        assertFalse(TipoDocumento.CNH.validar("1234567890"))
        assertTrue(TipoDocumento.PASSAPORTE.validar("AB123456"))
        assertFalse(TipoDocumento.PASSAPORTE.validar("AB12345"))
    }

    // --- estaCompleto() ---

    @Test
    fun `estaCompleto responde pelo comprimento normalizado`() {
        assertTrue(TipoDocumento.CPF.estaCompleto("529.982.247-25"))
        assertFalse(TipoDocumento.CPF.estaCompleto("529.982.247-2"))
    }

    // --- teclado ---

    @Test
    fun `so o passaporte admite letras`() {
        assertFalse(TipoDocumento.PASSAPORTE.apenasDigitos)
        listOf(TipoDocumento.CPF, TipoDocumento.CNPJ, TipoDocumento.RG, TipoDocumento.CNH)
            .forEach { assertTrue(it.apenasDigitos) }
    }
}