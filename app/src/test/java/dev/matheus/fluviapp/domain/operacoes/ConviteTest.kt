package dev.matheus.fluviapp.domain.operacoes

import dev.matheus.fluviapp.domain.operacoes.Funcionario.Cargo
import dev.matheus.fluviapp.domain.operacoes.Usuario.Papel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * O convite (F6.6) — o único lugar onde os dois contextos se encontram.
 *
 * O que estes casos fixam são as duas metades: **papel** existe sempre (sistema); **empresa e cargo** só
 * para o operador (negócio). Um convite de plataforma com vínculo prometeria algo que ninguém vai criar.
 */
class ConviteTest {

    @Test
    fun `convite de operador produz o vinculo que a equipe vai receber`() {
        val convite = Convite(
            email = "ana@x.com",
            nome = "Ana",
            papel = Papel.OPERADOR,
            empresaId = "empresa-1",
            cargo = Cargo.SUPERVISOR,
        )

        assertFalse(convite.ehDePlataforma)
        assertEquals(Vinculo("empresa-1", Cargo.SUPERVISOR), convite.vinculo)
    }

    /** `ADM`/`GESTOR` não têm registro na operação (§8.1) — e é por isso que não emitem passagem. */
    @Test
    fun `convite de plataforma nao produz vinculo, mesmo se alguem preencher empresa`() {
        listOf(Papel.ADM, Papel.GESTOR).forEach { papel ->
            val convite = Convite(
                email = "adm@x.com",
                nome = "Adm",
                papel = papel,
                empresaId = "empresa-1",
                cargo = Cargo.SUPERVISOR,
            )

            assertTrue(convite.ehDePlataforma)
            assertNull("papel $papel não deveria ter vínculo", convite.vinculo)
        }
    }

    @Test
    fun `convite de operador sem empresa ou sem cargo nao produz vinculo`() {
        val semEmpresa = Convite("ana@x.com", "Ana", Papel.OPERADOR, empresaId = "", cargo = Cargo.AGENTE)
        val semCargo = Convite("ana@x.com", "Ana", Papel.OPERADOR, empresaId = "empresa-1", cargo = null)

        assertNull(semEmpresa.vinculo)
        assertNull(semCargo.vinculo)
    }

    // --- Fronteira ---

    @Test
    fun `de normaliza o e-mail — ele e o id, e id nao pode depender de caixa`() {
        val convite = Convite.de("  Ana@X.com ", "Ana", "OPERADOR", "empresa-1", "AGENTE")

        assertEquals("ana@x.com", convite?.email)
    }

    /** Um convite é o que concede papel: papel ilegível concederia acesso que ninguém sabe qual é. */
    @Test
    fun `papel desconhecido nao vira convite`() {
        assertNull(Convite.de("ana@x.com", "Ana", "CHEFAO", "", null))
        assertNull(Convite.de("ana@x.com", "Ana", null, "", null))
    }

    @Test
    fun `sem e-mail nao ha convite — e o e-mail e o id`() {
        assertNull(Convite.de("", "Ana", "ADM", "", null))
        assertNull(Convite.de(null, "Ana", "ADM", "", null))
    }

    /** Cargo ilegível não derruba o convite: o papel continua valendo, e o vínculo é que não nasce. */
    @Test
    fun `cargo desconhecido apaga o vinculo, nao o convite`() {
        val convite = Convite.de("ana@x.com", "Ana", "OPERADOR", "empresa-1", "CHEFAO")

        assertEquals(Papel.OPERADOR, convite?.papel)
        assertNull(convite?.vinculo)
    }
}