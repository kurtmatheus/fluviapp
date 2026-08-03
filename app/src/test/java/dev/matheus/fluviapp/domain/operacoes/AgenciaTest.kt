package dev.matheus.fluviapp.domain.operacoes

import dev.matheus.fluviapp.revitalizacao.ForaDoEscopo
import org.junit.experimental.categories.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Agência como capacidade organizacional do **funcionário** (ADR-0015 §8.1) e o contrato da fronteira:
 * `de` é estrito (String→enum, `null` se desconhecido).
 *
 * O `deOuPadrao` saiu junto com a coluna no `Usuario`: enquanto `Funcionario.agencia` for String livre,
 * não existe leitura que possa cair em `AUTONOMO` sem apagar agência real (o seed tem nomes fora do
 * enum). Ele volta quando a agência virar seletor (P2.2b) — e este teste é o registro de que a ausência
 * é decisão, não esquecimento.
 */
@Category(ForaDoEscopo::class)
class AgenciaTest {

    @Test
    fun `de converte o valor canonico e recusa desconhecido`() {
        assertEquals(Agencia.MATRIZ, Agencia.de("MATRIZ"))
        assertEquals(Agencia.AUTONOMO, Agencia.de("AUTONOMO"))
        assertNull(Agencia.de("AGENCIA HORIZONTE"))
        assertNull(Agencia.de(""))
        assertNull(Agencia.de(null))
    }

    @Test
    fun `AUTONOMO e a coringa — existe no enum como padrao de quem nao foi alocado`() {
        assertEquals(Agencia.AUTONOMO, Agencia.entries.first())
    }
}
