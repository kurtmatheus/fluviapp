package dev.matheus.fluviapp.domain.operacoes

import dev.matheus.fluviapp.domain.operacoes.Funcionario.Cargo
import dev.matheus.fluviapp.domain.operacoes.Usuario.Papel
import dev.matheus.fluviapp.domain.screendata.SecaoMenu
import dev.matheus.fluviapp.domain.screendata.secoesDa
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Matriz de autorização dos **dois eixos** (ADR-0010 + ADR-0015 §8.2). Política pura, JVM-testável.
 *
 * - SISTEMA: `papel` — `ADM`/`GESTOR` (plataforma) e `OPERADOR` (o coringa que corresponde a um
 *   funcionário).
 * - NEGÓCIO: `cargo` — `SUPERVISOR`/`AGENTE`.
 *
 * Ambos chegam como a String persistida (o `.name` do enum). **Cargo ausente é caso normal**: quem tem
 * papel de plataforma não tem registro de funcionário.
 */
class PermissoesUsuarioTest {

    private val adm = Papel.ADM.name
    private val gestor = Papel.GESTOR.name
    private val operador = Papel.OPERADOR.name

    private val supervisor = Cargo.SUPERVISOR.name
    private val agente = Cargo.AGENTE.name

    // --- Fronteira String -> enum, nos dois eixos ---

    @Test
    fun `Papel de converte os tres papeis canonicos`() {
        assertEquals(Papel.ADM, Papel.de("ADM"))
        assertEquals(Papel.GESTOR, Papel.de("GESTOR"))
        assertEquals(Papel.OPERADOR, Papel.de("OPERADOR"))
    }

    @Test
    fun `Cargo de converte os dois cargos canonicos`() {
        assertEquals(Cargo.SUPERVISOR, Cargo.de("SUPERVISOR"))
        assertEquals(Cargo.AGENTE, Cargo.de("AGENTE"))
    }

    @Test
    fun `de retorna null para desconhecido ou nulo nos dois eixos`() {
        assertEquals(null, Papel.de(null))
        assertEquals(null, Papel.de(""))
        assertEquals(null, Papel.de("GERENTE"))
        assertEquals(null, Cargo.de(null))
        assertEquals(null, Cargo.de(""))
        assertEquals(null, Cargo.de("GERENTE"))
    }

    /**
     * Os eixos **não se misturam no vocabulário**: cargo de negócio não resolve como papel de sistema e
     * vice-versa. Sem isto, um perfil gravado com `cargo: "AGENTE"` (vocabulário de antes da divisão)
     * passaria a ser lido como papel — e um `SUPERVISOR` viraria papel de sistema, que é justamente o
     * acoplamento que a revisão estrutural desfez.
     */
    @Test
    fun `papel nao aceita cargo de negocio, e cargo nao aceita papel de sistema`() {
        assertEquals(null, Papel.de("SUPERVISOR"))
        assertEquals(null, Papel.de("AGENTE"))
        assertEquals(null, Cargo.de("ADM"))
        assertEquals(null, Cargo.de("GESTOR"))
        assertEquals(null, Cargo.de("OPERADOR"))
    }

    /** Vocabulário anterior ao ADR-0015 §4.2 continua sem alias de compatibilidade. */
    @Test
    fun `vocabulario antigo nao resolve — fail-closed no rename`() {
        assertEquals(null, Papel.de("DIRETOR"))
        assertEquals(null, Papel.de("COLABORADOR_MASTER"))
        assertEquals(null, Cargo.de("DIRETOR"))
        assertEquals(null, Cargo.de("COLABORADOR_MASTER"))
    }

    // --- ehPapelPlataforma ---

    @Test
    fun `apenas ADM e GESTOR sao papeis de plataforma`() {
        assertTrue(PermissoesUsuario.ehPapelPlataforma(adm))
        assertTrue(PermissoesUsuario.ehPapelPlataforma(gestor))
        assertFalse(PermissoesUsuario.ehPapelPlataforma(operador))
        assertFalse(PermissoesUsuario.ehPapelPlataforma(null))
        assertFalse(PermissoesUsuario.ehPapelPlataforma("DESCONHECIDO"))
    }

    // --- Eixo seção (menu): puramente de sistema ---

    /**
     * **A plataforma deixou de ver a Equipe** (F6.6): o quadro de pessoal é da empresa, e o que a
     * plataforma administra é *quem acessa o app* — que agora tem seção própria, `USUARIOS`. E ela é
     * `ADM`-only, então o `GESTOR` vê uma seção a menos que o `ADM`: é a primeira vez que os dois papéis
     * de plataforma divergem (ADR-0021 D1).
     */
    @Test
    fun `papel de plataforma ve tudo menos a Equipe — e so o ADM ve Usuarios`() {
        assertEquals(SecaoMenu.entries - SecaoMenu.EQUIPE, PermissoesUsuario.secoesVisiveis(adm))
        assertEquals(
            SecaoMenu.entries - SecaoMenu.EQUIPE - SecaoMenu.USUARIOS,
            PermissoesUsuario.secoesVisiveis(gestor),
        )
    }

    @Test
    fun `agente so ve Passagem`() {
        assertEquals(listOf(SecaoMenu.PASSAGEM), PermissoesUsuario.secoesVisiveis(operador, agente))
        assertFalse(PermissoesUsuario.podeAcessar(SecaoMenu.EQUIPE, operador, agente))
    }

    @Test
    fun `supervisor ve Passagem e EQUIPE — e nada de cadastro de plataforma`() {
        // A Equipe é a única seção que olha o cargo: ela existe para o supervisor gerir a própria
        // agência (§2.2). Viagem/Empresa/Embarcacao continuam sendo cadastro de plataforma.
        assertEquals(
            listOf(SecaoMenu.PASSAGEM, SecaoMenu.EQUIPE),
            PermissoesUsuario.secoesVisiveis(operador, supervisor),
        )
        assertFalse(PermissoesUsuario.podeAcessar(SecaoMenu.VIAGEM, operador, supervisor))
        assertFalse(PermissoesUsuario.podeAcessar(SecaoMenu.EMBARCACAO, operador, supervisor))
    }

    // --- Família da atuação × permissão (ADR-0016 §2, ADR-0020 F3) ---

    @Test
    fun `sem atuacao, o comportamento e o de antes — a familia nao filtra nada`() {
        // O caminho de compatibilidade: o vínculo só existe a partir da F4. Enquanto não existir,
        // esta fatia não pode mudar uma linha do que aparece em tela.
        assertEquals(SecaoMenu.entries - SecaoMenu.EQUIPE, PermissoesUsuario.secoesVisiveis(adm, atuacao = null))
        assertEquals(
            listOf(SecaoMenu.PASSAGEM),
            PermissoesUsuario.secoesVisiveis(operador, agente, atuacao = null),
        )
    }

    @Test
    fun `com atuacao, o papel de plataforma ve o painel — nao a operacao`() {
        val visiveis = PermissoesUsuario.secoesVisiveis(adm, atuacao = Atuacao.AGENCIAMENTO)

        // A ordem é a do enum, e desde o ADR-0020 D10 ela começa pela EMPRESA: é a parte, e dela
        // dependem as outras — embarcacao tem dono, funcionário tem vínculo.
        assertEquals(
            listOf(
                SecaoMenu.EMPRESA,
                SecaoMenu.EMBARCACAO,
                // A Localidade é capacidade da plataforma (ADR-0016 §5): sem dono e sem atuação, ela só
                // pode estar aqui — nenhuma agência a administraria.
                SecaoMenu.LOCALIDADE,
                // O Porto, idem — e apesar de a atuação portuária existir: o cais é infraestrutura, e o
                // que a empresa tem nele é a atuação, não o porto.
                SecaoMenu.PORTO,
                SecaoMenu.VIAGEM,
                // A `EQUIPE` saiu daqui na F6.6 e no lugar dela entrou `USUARIOS`: a plataforma
                // administra **quem acessa o app**, não o quadro de pessoal de uma empresa.
                SecaoMenu.USUARIOS,
            ),
            visiveis,
        )
        // ADM administra a plataforma; emitir passagem exige vínculo de funcionário (ADR-0016 §2).
        assertFalse(SecaoMenu.PASSAGEM in visiveis)
        assertFalse(SecaoMenu.EQUIPE in visiveis)
    }

    @Test
    fun `com atuacao, o agente do agenciamento ve so a passagem`() {
        assertEquals(
            listOf(SecaoMenu.PASSAGEM),
            PermissoesUsuario.secoesVisiveis(operador, agente, Atuacao.AGENCIAMENTO),
        )
    }

    @Test
    fun `a familia nao concede o que a permissao nega`() {
        // Equipe está na família do agenciamento, mas o AGENTE não pode cadastrar membro (§2.1):
        // pertencer à família não basta.
        assertTrue(SecaoMenu.EQUIPE in secoesDa(Atuacao.AGENCIAMENTO))
        assertFalse(
            SecaoMenu.EQUIPE in PermissoesUsuario.secoesVisiveis(operador, agente, Atuacao.AGENCIAMENTO),
        )
        assertTrue(
            SecaoMenu.EQUIPE in
                PermissoesUsuario.secoesVisiveis(operador, supervisor, Atuacao.AGENCIAMENTO),
        )
    }

    @Test
    fun `atuacao dormente nao mostra secao nenhuma`() {
        assertEquals(
            emptyList<SecaoMenu>(),
            PermissoesUsuario.secoesVisiveis(operador, supervisor, Atuacao.PORTUARIA_OPERACAO),
        )
    }

    // --- Eixo ação sobre a Equipe (§2.1/§2.2/§8.5) ---

    @Test
    fun `cadastrar membro e da plataforma ou do supervisor`() {
        assertTrue(PermissoesUsuario.podeCadastrarFuncionario(adm, null))
        assertTrue(PermissoesUsuario.podeCadastrarFuncionario(operador, supervisor))
        assertFalse(PermissoesUsuario.podeCadastrarFuncionario(operador, agente))
        assertFalse(PermissoesUsuario.podeCadastrarFuncionario(null, null))
    }

    @Test
    fun `escolher agencia, definir cargo, deletar e ver todas as agencias sao SO da plataforma`() {
        listOf(adm, gestor).forEach { papel ->
            assertTrue(PermissoesUsuario.podeEscolherAgencia(papel))
            assertTrue(PermissoesUsuario.podeDefinirCargo(papel))
            assertTrue(PermissoesUsuario.podeDeletarFuncionario(papel))
            assertTrue(PermissoesUsuario.podeVerTodasAgencias(papel))
        }
        // O supervisor cadastra, mas na agência dele, sem promover ninguém e sem apagar.
        assertFalse(PermissoesUsuario.podeEscolherAgencia(operador))
        assertFalse(PermissoesUsuario.podeDefinirCargo(operador))
        assertFalse(PermissoesUsuario.podeDeletarFuncionario(operador))
        assertFalse(PermissoesUsuario.podeVerTodasAgencias(operador))
    }

    // --- O eixo do vínculo (ADR-0016 §6, ADR-0022 D4 — F6) ---

    private val naEmpresaA = Vinculo("empresa-a", Cargo.SUPERVISOR)
    private val agenteNaB = Vinculo("empresa-b", Cargo.AGENTE)

    /** A mesma regra do §2.1, com a coordenada certa: o "dele" deixa de ser String e vira id de empresa. */
    @Test
    fun `cadastrar membro pelo vinculo — plataforma em qualquer empresa, supervisor na dele`() {
        assertTrue(PermissoesUsuario.podeCadastrarMembro(adm, vinculo = null))
        assertTrue(PermissoesUsuario.podeCadastrarMembro(operador, naEmpresaA))
        assertFalse(PermissoesUsuario.podeCadastrarMembro(operador, agenteNaB))
        assertFalse(PermissoesUsuario.podeCadastrarMembro(operador, vinculo = null))
    }

    @Test
    fun `o escopo de empresa recorta pelo vinculo ativo`() {
        assertEquals(
            PermissoesUsuario.EscopoEmpresa.Todas,
            PermissoesUsuario.escopoDeEmpresa(adm, agenteNaB),
        )
        assertEquals(
            PermissoesUsuario.EscopoEmpresa.Apenas("empresa-b"),
            PermissoesUsuario.escopoDeEmpresa(operador, agenteNaB),
        )
    }

    /**
     * O caso que o tipo existe para impedir: sem plataforma e sem vínculo, a listagem não abre inteira —
     * ela não abre. "Não filtra nada" e "não tem empresa nenhuma" seriam a mesma String vazia.
     */
    @Test
    fun `sem papel de plataforma e sem vinculo, o escopo e Nenhuma`() {
        assertEquals(
            PermissoesUsuario.EscopoEmpresa.Nenhuma,
            PermissoesUsuario.escopoDeEmpresa(operador, vinculo = null),
        )
        assertEquals(
            PermissoesUsuario.EscopoEmpresa.Nenhuma,
            PermissoesUsuario.escopoDeEmpresa(null, vinculo = null),
        )
    }

    /**
     * A atuação passa a sair da **escolha**, não do cargo — e é isso que faz sentido quando a pessoa tem
     * dois vínculos, porque aí o cargo deixa de ser um só.
     */
    @Test
    fun `a atuacao em vigor e a do vinculo ativo`() {
        assertEquals(Atuacao.AGENCIAMENTO, PermissoesUsuario.atuacaoEmVigor(naEmpresaA))
        // Quem administra a plataforma não atua em segmento nenhum, e isso é a informação, não a falta dela.
        assertEquals(null, PermissoesUsuario.atuacaoEmVigor(null))
    }

    @Test
    fun `papel desconhecido nao ve nenhuma secao operacional`() {
        assertEquals(listOf(SecaoMenu.PASSAGEM), PermissoesUsuario.secoesVisiveis(null))
        assertFalse(PermissoesUsuario.podeAcessar(SecaoMenu.VIAGEM, null))
        assertFalse(PermissoesUsuario.podeAcessar(SecaoMenu.EMPRESA, "X"))
        assertFalse(PermissoesUsuario.podeAcessar(SecaoMenu.EMBARCACAO, operador))
    }

    @Test
    fun `Passagem e visivel para todos`() {
        listOf(adm, gestor, operador, null, "X").forEach {
            assertTrue(PermissoesUsuario.podeAcessar(SecaoMenu.PASSAGEM, it))
        }
    }

    // --- Eixo ação: criar ---

    @Test
    fun `os tres papeis conhecidos podem criar passagem`() {
        listOf(adm, gestor, operador).forEach {
            assertTrue("papel $it deveria poder criar", PermissoesUsuario.podeCriarPassagem(it))
        }
    }

    @Test
    fun `papel desconhecido nao pode criar passagem`() {
        assertFalse(PermissoesUsuario.podeCriarPassagem(null))
        assertFalse(PermissoesUsuario.podeCriarPassagem("GERENTE"))
    }

    // --- Escopo de agência na listagem (§4.1/§6) ---

    @Test
    fun `plataforma atravessa agencias`() {
        assertEquals(
            PermissoesUsuario.EscopoAgencia.Todas,
            PermissoesUsuario.escopoDeAgencia(adm, "MATRIZ"),
        )
        // Mesmo sem vínculo nenhum: o papel basta.
        assertEquals(
            PermissoesUsuario.EscopoAgencia.Todas,
            PermissoesUsuario.escopoDeAgencia(gestor, null),
        )
    }

    @Test
    fun `quem nao e plataforma ve so a propria agencia`() {
        assertEquals(
            PermissoesUsuario.EscopoAgencia.Apenas("AGENCIA LITORAL"),
            PermissoesUsuario.escopoDeAgencia(operador, "AGENCIA LITORAL"),
        )
    }

    @Test
    fun `sem plataforma e sem agencia nao ve nada — nao vira sem-filtro`() {
        // O caso perigoso: se "sem agência" virasse String vazia tratada como "sem filtro", um perfil
        // sem vínculo abriria a listagem inteira. O tipo separa os dois.
        assertEquals(
            PermissoesUsuario.EscopoAgencia.Nenhuma,
            PermissoesUsuario.escopoDeAgencia(operador, null),
        )
        assertEquals(
            PermissoesUsuario.EscopoAgencia.Nenhuma,
            PermissoesUsuario.escopoDeAgencia(operador, "   "),
        )
        assertEquals(
            PermissoesUsuario.EscopoAgencia.Nenhuma,
            PermissoesUsuario.escopoDeAgencia(null, null),
        )
    }

    // --- Eixo ação: editar qualquer / ver todas (é aqui que os dois eixos se encontram) ---

    @Test
    fun `plataforma edita qualquer passagem mesmo sem cargo de negocio`() {
        assertTrue(PermissoesUsuario.podeEditarQualquerPassagem(adm, cargo = null))
        assertTrue(PermissoesUsuario.podeEditarQualquerPassagem(gestor, cargo = null))
    }

    @Test
    fun `supervisor edita qualquer passagem pelo CARGO, com papel de operador`() {
        assertTrue(PermissoesUsuario.podeEditarQualquerPassagem(operador, supervisor))
    }

    @Test
    fun `agente nao edita qualquer passagem`() {
        assertFalse(PermissoesUsuario.podeEditarQualquerPassagem(operador, agente))
        assertFalse(PermissoesUsuario.podeEditarQualquerPassagem(operador, cargo = null))
        assertFalse(PermissoesUsuario.podeEditarQualquerPassagem(null, null))
    }

    @Test
    fun `ver todas na pesquisa acompanha o editar-qualquer`() {
        assertTrue(PermissoesUsuario.podeVerTodasPassagens(adm, null))
        assertTrue(PermissoesUsuario.podeVerTodasPassagens(operador, supervisor))
        assertFalse(PermissoesUsuario.podeVerTodasPassagens(operador, agente))
    }

    // --- Eixo ação: editar passagem específica (com posse) ---

    @Test
    fun `papel de plataforma edita passagem independentemente da posse`() {
        assertTrue(PermissoesUsuario.podeEditarPassagem(adm, null, ehDono = false))
        assertTrue(PermissoesUsuario.podeEditarPassagem(gestor, null, ehDono = false))
    }

    @Test
    fun `supervisor edita passagem de outros`() {
        assertTrue(PermissoesUsuario.podeEditarPassagem(operador, supervisor, ehDono = false))
        assertTrue(PermissoesUsuario.podeEditarPassagem(operador, supervisor, ehDono = true))
    }

    @Test
    fun `agente edita apenas a propria passagem`() {
        assertTrue(PermissoesUsuario.podeEditarPassagem(operador, agente, ehDono = true))
        // regressão do gate do detalhe: agente NÃO edita passagem de outro
        assertFalse(PermissoesUsuario.podeEditarPassagem(operador, agente, ehDono = false))
    }

    @Test
    fun `desconhecido nos dois eixos so edita se for dono`() {
        assertTrue(PermissoesUsuario.podeEditarPassagem(null, null, ehDono = true))
        assertFalse(PermissoesUsuario.podeEditarPassagem(null, null, ehDono = false))
        assertFalse(PermissoesUsuario.podeEditarPassagem("X", "Y", ehDono = false))
    }

    // --- Eixo ação: confirmar embarque (ADR-0012) ---

    @Test
    fun `qualquer papel conhecido pode confirmar embarque`() {
        listOf(adm, gestor, operador).forEach {
            assertTrue("papel $it deveria poder confirmar embarque", PermissoesUsuario.podeConfirmarEmbarque(it))
        }
    }

    @Test
    fun `papel desconhecido nao confirma embarque`() {
        assertFalse(PermissoesUsuario.podeConfirmarEmbarque(null))
        assertFalse(PermissoesUsuario.podeConfirmarEmbarque("GERENTE"))
    }

    // --- Deletar segue as mesmas regras de editar ---

    @Test
    fun `deletar segue exatamente o editar em toda a matriz`() {
        listOf(adm, gestor, operador, null, "X").forEach { papel ->
            listOf(supervisor, agente, null, "Y").forEach { cargo ->
                listOf(true, false).forEach { ehDono ->
                    assertEquals(
                        "deletar deve espelhar editar para papel=$papel cargo=$cargo dono=$ehDono",
                        PermissoesUsuario.podeEditarPassagem(papel, cargo, ehDono),
                        PermissoesUsuario.podeDeletarPassagem(papel, cargo, ehDono)
                    )
                }
            }
        }
    }
}