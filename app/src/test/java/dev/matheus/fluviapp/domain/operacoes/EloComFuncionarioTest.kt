package dev.matheus.fluviapp.domain.operacoes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * O elo 1-1 entre perfil e funcionário (ADR-0015 §8.3, [ADR-0032] D5/Q1).
 *
 * O que estes casos guardam é a **unicidade que o servidor não guarda**: regra não consulta coleção, então
 * quem impede dois perfis apontando para o mesmo funcionário é o app. Sem isso, duas pessoas seriam donas
 * das mesmas passagens (§8.4) e nenhuma das duas saberia.
 */
class EloComFuncionarioTest {

    private fun funcionario(id: String, nome: String) = Funcionario(id = id, descricaoNome = nome)

    private fun perfil(id: String, papel: Usuario.Papel, elo: String = "") = Usuario(
        id = id,
        email = "$id@x.com",
        username = id,
        papel = papel.name,
        funcionarioId = elo,
    )

    private val ana = funcionario("f-ana", "Ana Ribeiro")
    private val bruno = funcionario("f-bruno", "Bruno Costa")
    private val carla = funcionario("f-carla", "Carla Dias")

    // --- Quais funcionários podem receber o elo ---

    @Test
    fun `sem perfil nenhum, todos podem receber o elo`() {
        val elegiveis = funcionariosElegiveis(listOf(ana, bruno), usuarios = emptyList())

        assertEquals(listOf("f-ana", "f-bruno"), elegiveis.map { it.id })
    }

    /** **O 1-1 em uma linha**: funcionário que já responde por um perfil sai da lista. */
    @Test
    fun `funcionario que ja tem perfil sai da lista`() {
        val elegiveis = funcionariosElegiveis(
            funcionarios = listOf(ana, bruno, carla),
            usuarios = listOf(perfil("u1", Usuario.Papel.OPERADOR, elo = "f-bruno")),
        )

        assertEquals(listOf("f-ana", "f-carla"), elegiveis.map { it.id })
    }

    /**
     * **O elo do próprio perfil continua elegível**, e é o que faz a lista servir para exibir e não só para
     * escolher: sem isso, abrir um perfil já ligado mostraria o campo vazio, como se o elo não existisse.
     */
    @Test
    fun `o elo atual permanece na lista`() {
        val elegiveis = funcionariosElegiveis(
            funcionarios = listOf(ana, bruno),
            usuarios = listOf(perfil("u1", Usuario.Papel.ADM, elo = "f-ana")),
            eloAtual = "f-ana",
        )

        assertEquals(listOf("f-ana", "f-bruno"), elegiveis.map { it.id })
    }

    /** Perfil sem elo não reserva ninguém — `funcionarioId` vazio não é um id. */
    @Test
    fun `perfil sem elo nao tira ninguem da lista`() {
        val elegiveis = funcionariosElegiveis(
            funcionarios = listOf(ana),
            usuarios = listOf(perfil("u1", Usuario.Papel.ADM), perfil("u2", Usuario.Papel.GESTOR)),
        )

        assertEquals(listOf("f-ana"), elegiveis.map { it.id })
    }

    /** Elo apontando para funcionário que não existe não estoura nem inventa opção. */
    @Test
    fun `elo quebrado nao afeta a lista`() {
        val elegiveis = funcionariosElegiveis(
            funcionarios = listOf(ana),
            usuarios = listOf(perfil("u1", Usuario.Papel.OPERADOR, elo = "f-que-nao-existe")),
        )

        assertEquals(listOf("f-ana"), elegiveis.map { it.id })
    }

    /** Ordena por nome porque a lista é lida por gente — e sem depender da caixa. */
    @Test
    fun `a lista vem ordenada por nome`() {
        val elegiveis = funcionariosElegiveis(
            funcionarios = listOf(funcionario("f-3", "carla"), funcionario("f-1", "Ana"), bruno),
            usuarios = emptyList(),
        )

        assertEquals(listOf("Ana", "Bruno Costa", "carla"), elegiveis.map { it.descricaoNome })
    }

    // --- Em que perfis o elo se liga à mão ---

    /**
     * **`OPERADOR` fica fora**, e não por hierarquia: o elo dele já tem caminho, e o caminho **verifica** o
     * e-mail no primeiro acesso. Ligar à mão passaria por fora dessa verificação e moveria a posse das
     * passagens seguintes (§8.4).
     */
    @Test
    fun `o elo manual e dos papeis de plataforma`() {
        assertTrue(PermissoesUsuario.aceitaEloManual(Usuario.Papel.ADM.name))
        assertTrue(PermissoesUsuario.aceitaEloManual(Usuario.Papel.GESTOR.name))
        assertFalse(PermissoesUsuario.aceitaEloManual(Usuario.Papel.OPERADOR.name))
    }

    @Test
    fun `papel desconhecido nao aceita elo manual`() {
        assertFalse(PermissoesUsuario.aceitaEloManual(null))
        assertFalse(PermissoesUsuario.aceitaEloManual(""))
        assertFalse(PermissoesUsuario.aceitaEloManual("GERENTE"))
    }
}
