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
 * O recorte da revitalização, como domínio puro: o menu de qualquer usuário reflete o que já foi refeito —
 * sem Firestore, sem sessão, sem tela.
 *
 * Desde a F9.6 o recorte **não recorta mais nada**: todas as seções entraram. As duas propriedades que
 * fazem o andaime ser seguro continuam sendo o que este teste protege — o menu **nunca amplia** a permissão,
 * e nenhuma seção de fora escapa por nenhum caminho de papel ou cargo —, e são elas que sobrevivem à
 * eventual remoção do andaime, mudando de arquivo.
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

    /**
     * **O andaime alcançou a paridade** (F9.6): a Passagem foi a última a entrar, e `SECOES_REVITALIZADAS`
     * passou a ser `SecaoMenu.entries`.
     *
     * A asserção mudou de forma junto: não é mais uma lista escrita à mão — que precisava ser editada a cada
     * seção — e sim a igualdade com o enum. É o critério que o próprio [SECOES_REVITALIZADAS] declara para
     * o andaime poder ser removido, e enquanto ele existir este caso o mantém honesto: seção nova nasce
     * fora, e este teste cobra a entrada dela.
     */
    @Test
    fun `todas as secoes estao revitalizadas`() {
        assertEquals(SecaoMenu.entries.toSet(), SECOES_REVITALIZADAS)
        SecaoMenu.entries.forEach { assertTrue("$it fora do andaime", estaRevitalizada(it)) }
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

        // Rota e Viagem entram no meio, na ordem do enum: a Rota vem do Porto, a Viagem vem da Rota, e
        // as duas são compartilhadas (F7/F8).
        val pool = listOf(SecaoMenu.ROTA, SecaoMenu.VIAGEM)

        assertEquals(doPainel + pool + SecaoMenu.USUARIOS, secoesDoMenu(adm))
        assertEquals(doPainel + pool, secoesDoMenu(gestor))
    }

    /**
     * **O agente ganhou a seção dele** (F9.6): a Passagem é a razão de o cargo existir, e até aqui ele
     * tinha menu só de leitura — via o pool e não emitia nada. A diferença entre os dois cargos deixa de
     * ser "quem tem menu" e passa a ser o que o menu concede.
     */
    @Test
    fun `o supervisor ve o pool, a Passagem e a Equipe, e o agente tudo menos a Equipe`() {
        assertEquals(
            listOf(SecaoMenu.ROTA, SecaoMenu.VIAGEM, SecaoMenu.PASSAGEM, SecaoMenu.EQUIPE),
            secoesDoMenu(operador, supervisor, Atuacao.AGENCIAMENTO),
        )
        assertEquals(
            listOf(SecaoMenu.ROTA, SecaoMenu.VIAGEM, SecaoMenu.PASSAGEM),
            secoesDoMenu(operador, agente, Atuacao.AGENCIAMENTO),
        )
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