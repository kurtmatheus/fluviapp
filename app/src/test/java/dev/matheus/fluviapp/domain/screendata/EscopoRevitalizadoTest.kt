package dev.matheus.fluviapp.domain.screendata

import dev.matheus.fluviapp.domain.operacoes.Atuacao
import dev.matheus.fluviapp.domain.operacoes.Funcionario.Cargo
import dev.matheus.fluviapp.domain.operacoes.PermissoesUsuario
import dev.matheus.fluviapp.domain.operacoes.Usuario.Papel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * O recorte da revitalização, como domínio puro: **só a Empresa está viva**, e o menu de qualquer usuário
 * reflete isso — sem Firestore, sem sessão, sem tela.
 *
 * O que este teste protege não é a lista em si (ela muda a cada seção revitalizada), e sim as duas
 * propriedades que fazem o andaime ser seguro: o menu **nunca amplia** a permissão, e nenhuma seção não
 * revitalizada escapa por nenhum caminho de papel ou cargo.
 */
class EscopoRevitalizadoTest {

    private val adm = Papel.ADM.name
    private val gestor = Papel.GESTOR.name
    private val operador = Papel.OPERADOR.name

    private val supervisor = Cargo.SUPERVISOR.name
    private val agente = Cargo.AGENTE.name

    /** Todo par (papel, cargo) que o app admite hoje — a matriz que os casos abaixo varrem. */
    private val todosOsUsuarios: List<Pair<String?, String?>> = listOf(
        adm to null,
        gestor to null,
        operador to supervisor,
        operador to agente,
        null to null, // deslogado / papel desconhecido: fail-closed
    )

    // --- O escopo em si ---

    @Test
    fun `estao revitalizadas a Empresa e a Flotilha`() {
        assertEquals(setOf(SecaoMenu.EMPRESA, SecaoMenu.EMBARCACAO), SECOES_REVITALIZADAS)
        assertTrue(estaRevitalizada(SecaoMenu.EMPRESA))
        assertTrue(estaRevitalizada(SecaoMenu.EMBARCACAO))
        assertFalse(estaRevitalizada(SecaoMenu.VIAGEM))
        assertFalse(estaRevitalizada(SecaoMenu.PASSAGEM))
        assertFalse(estaRevitalizada(SecaoMenu.EQUIPE))
    }

    // --- O menu que o painel monta ---

    /** A ordem é a de [SecaoMenu] — Empresa antes da Flotilha, que é a ordem de dependência do cadastro. */
    @Test
    fun `plataforma ve a Empresa e a Flotilha`() {
        assertEquals(listOf(SecaoMenu.EMPRESA, SecaoMenu.EMBARCACAO), secoesDoMenu(adm))
        assertEquals(listOf(SecaoMenu.EMPRESA, SecaoMenu.EMBARCACAO), secoesDoMenu(gestor))
    }

    /**
     * O operador tinha PASSAGEM garantida pela política (`podeAcessar` devolve `true` para todos) e a
     * Empresa negada — logo, menu vazio. Não é bug: é o app agindo como recém-implementado para quem
     * ainda não tem seção viva.
     */
    @Test
    fun `operador fica sem menu enquanto a secao dele nao for revitalizada`() {
        assertEquals(emptyList<SecaoMenu>(), secoesDoMenu(operador, agente, Atuacao.AGENCIAMENTO))
        assertEquals(emptyList<SecaoMenu>(), secoesDoMenu(operador, supervisor, Atuacao.AGENCIAMENTO))
    }

    @Test
    fun `nenhum usuario alcanca secao nao revitalizada`() {
        todosOsUsuarios.forEach { (papel, cargo) ->
            val menu = secoesDoMenu(papel, cargo, Cargo.de(cargo)?.atuacao)
            assertTrue(
                "menu de ($papel, $cargo) vazou seção não revitalizada: $menu",
                menu.all(::estaRevitalizada),
            )
        }
    }

    /**
     * A propriedade que sustenta a separação entre política e andaime: o escopo só **reduz**. Se algum dia
     * o menu conceder o que a permissão nega, é aqui que quebra.
     */
    @Test
    fun `o menu nunca amplia o que a politica concede`() {
        todosOsUsuarios.forEach { (papel, cargo) ->
            val atuacao = Cargo.de(cargo)?.atuacao
            val permitidas = PermissoesUsuario.secoesVisiveis(papel, cargo, atuacao)
            assertTrue(
                "menu de ($papel, $cargo) ampliou a permissão",
                permitidas.containsAll(secoesDoMenu(papel, cargo, atuacao)),
            )
        }
    }

    /** A ordem do menu continua sendo a de [SecaoMenu] — o filtro preserva, não reordena. */
    @Test
    fun `o menu preserva a ordem do enum`() {
        val menu = secoesDoMenu(adm)
        assertEquals(menu.sortedBy { it.ordinal }, menu)
    }
}