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
    fun `estao revitalizadas as quatro do painel, a Equipe e os Usuarios`() {
        assertEquals(
            setOf(
                SecaoMenu.EMPRESA,
                SecaoMenu.EMBARCACAO,
                SecaoMenu.LOCALIDADE,
                SecaoMenu.PORTO,
                SecaoMenu.EQUIPE,
                SecaoMenu.USUARIOS,
            ),
            SECOES_REVITALIZADAS,
        )
        assertTrue(estaRevitalizada(SecaoMenu.USUARIOS))
        assertTrue(estaRevitalizada(SecaoMenu.EMPRESA))
        assertTrue(estaRevitalizada(SecaoMenu.EMBARCACAO))
        assertTrue(estaRevitalizada(SecaoMenu.LOCALIDADE))
        assertTrue(estaRevitalizada(SecaoMenu.PORTO))
        // Entrou na F6.4, junto com a seleção de contexto — não antes (ADR-0022 D5).
        assertTrue(estaRevitalizada(SecaoMenu.EQUIPE))
        assertFalse(estaRevitalizada(SecaoMenu.VIAGEM))
        assertFalse(estaRevitalizada(SecaoMenu.PASSAGEM))
    }

    // --- O menu que o painel monta ---

    /**
     * A ordem é a de [SecaoMenu] — parte, ativos, capacidades, e o acesso por último.
     *
     * **`ADM` e `GESTOR` divergem pela primeira vez** (F6.6): administrar quem entra é do `ADM`
     * (ADR-0021 D1), e nenhum dos dois vê a Equipe, que é da empresa.
     */
    @Test
    fun `a plataforma ve as quatro do painel, e so o ADM ve Usuarios`() {
        val doPainel = listOf(
            SecaoMenu.EMPRESA,
            SecaoMenu.EMBARCACAO,
            SecaoMenu.LOCALIDADE,
            SecaoMenu.PORTO,
        )

        assertEquals(doPainel + SecaoMenu.USUARIOS, secoesDoMenu(adm))
        assertEquals(doPainel, secoesDoMenu(gestor))
    }

    /**
     * **O supervisor ganhou a primeira seção dele** (F6.4): a Equipe atravessa painel e atuação (§2.2), e
     * ele a gere na própria empresa. O agente continua sem menu — a seção dele é a Passagem, que ainda
     * não foi revitalizada —, e isso não é bug: é o app agindo como recém-implementado.
     */
    @Test
    fun `o supervisor ve a Equipe e o agente segue sem menu`() {
        assertEquals(listOf(SecaoMenu.EQUIPE), secoesDoMenu(operador, supervisor, Atuacao.AGENCIAMENTO))
        assertEquals(emptyList<SecaoMenu>(), secoesDoMenu(operador, agente, Atuacao.AGENCIAMENTO))
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