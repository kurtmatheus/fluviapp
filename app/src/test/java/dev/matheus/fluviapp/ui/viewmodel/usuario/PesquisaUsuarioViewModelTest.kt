package dev.matheus.fluviapp.ui.viewmodel.usuario

import dev.matheus.fluviapp.domain.operacoes.Convite
import dev.matheus.fluviapp.domain.operacoes.Funcionario.Cargo
import dev.matheus.fluviapp.domain.operacoes.Usuario
import dev.matheus.fluviapp.domain.operacoes.Usuario.Papel
import dev.matheus.fluviapp.domain.viagem.Empresa
import dev.matheus.fluviapp.fakes.FakeConviteRepository
import dev.matheus.fluviapp.fakes.FakeEmpresaRepository
import dev.matheus.fluviapp.fakes.FakeRelogio
import dev.matheus.fluviapp.fakes.FakeSessaoUsuario
import dev.matheus.fluviapp.fakes.FakeUsuarioRepository
import dev.matheus.fluviapp.services.repository.operacoes.SessaoUsuario
import dev.matheus.fluviapp.telemetry.registroCadastroDeTeste
import dev.matheus.fluviapp.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * A seção Usuários deixando de ser somente-leitura ([ADR-0032] D6).
 *
 * O que estes casos travam é a **junção**: a lista deixou de ser a dos convites e passou a ser a das
 * pessoas, com a situação vinda de onde ela mora (`users/{uid}`). O convite responde pelo passado, e é
 * por isso que um convite usado por alguém depois desativado não pode aparecer como *Ativo*.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PesquisaUsuarioViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val hoje = LocalDateTime.of(2026, 9, 8, 10, 0)
    private val agora = hoje.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private val ontem = agora - 86_400_000L
    private val amanha = agora + 86_400_000L

    private fun empresa(id: String, nome: String) =
        Empresa(id = id, nome = nome, razaoSocial = nome, cnpj = "", endereco = "", telefone1 = "", telefone2 = "")

    private fun vm(
        usuarios: FakeUsuarioRepository,
        convites: FakeConviteRepository = FakeConviteRepository(),
        sessao: SessaoUsuario = FakeSessaoUsuario.plataforma(),
    ) = PesquisaUsuarioViewModel(
        convites,
        usuarios,
        FakeEmpresaRepository().apply { empresas = listOf(empresa("empresa-1", "Navegação Norte")) },
        sessao,
        registroCadastroDeTeste(),
        FakeRelogio(hoje),
    )

    private fun usuario(
        id: String,
        email: String,
        papel: Papel = Papel.OPERADOR,
        ativo: Boolean = true,
        expiraEm: Long? = null,
    ) = Usuario(
        id = id,
        email = email,
        username = email.substringBefore('@'),
        papel = papel.name,
        ativo = ativo,
        expiraEm = expiraEm,
    )

    private fun convite(email: String, nome: String, usado: Boolean = true) = Convite(
        email = email,
        nome = nome,
        papel = Papel.OPERADOR,
        empresaId = "empresa-1",
        cargo = Cargo.AGENTE,
        usado = usado,
    )

    private fun repo(vararg perfis: Usuario) = FakeUsuarioRepository().apply { usuarios = perfis.toList() }

    // --- A junção das duas coleções ---

    /**
     * **O caso que motivou a fatia**: o convite usado dizia *Ativo* para sempre, porque era o único dado
     * que a lista tinha. Com o acesso tendo estado, a situação vem do perfil — e desativado aparece
     * desativado.
     */
    @Test
    fun `a situacao vem do perfil, e nao do convite usado`() = runTest(mainRule.dispatcher) {
        val convites = FakeConviteRepository().apply {
            convites = listOf(convite("ana@x.com", "Ana Ribeiro"))
        }
        val vm = vm(repo(usuario("uid-ana", "ana@x.com", ativo = false)), convites)
        advanceUntilIdle()

        val ana = vm.uiState.value.resultados.single()
        assertEquals("Desativado", ana.situacao)
        // O nome continua vindo do convite: `users/{uid}` não tem nome (§8.1).
        assertEquals("Ana Ribeiro", ana.nome)
        assertEquals("Navegação Norte · AGENTE", ana.vinculo)
    }

    @Test
    fun `quem tem convite e nao tem perfil aparece como convidado`() = runTest(mainRule.dispatcher) {
        val convites = FakeConviteRepository().apply {
            convites = listOf(convite("carla@x.com", "Carla", usado = false))
        }
        val vm = vm(repo(), convites)
        advanceUntilIdle()

        val carla = vm.uiState.value.resultados.single()
        assertEquals("Convidado", carla.situacao)
        // Sem uid não há acesso a gerir: não se desativa quem ainda não entrou.
        assertEquals("", carla.id)
        assertFalse(carla.temAcesso)
    }

    /**
     * **O `ADM` de bootstrap não tem convite** (ADR-0021 D0), e é a conta mais poderosa do sistema.
     * Deixá-lo fora da lista esconderia da gestão de acesso justamente quem mais precisa aparecer.
     */
    @Test
    fun `quem tem perfil e nao tem convite aparece igual`() = runTest(mainRule.dispatcher) {
        val vm = vm(repo(usuario("uid-adm", "adm@x.com", papel = Papel.ADM)))
        advanceUntilIdle()

        val adm = vm.uiState.value.resultados.single()
        assertEquals("Ativo", adm.situacao)
        assertEquals("ADM", adm.papel)
        // Sem convite, o nome cai no `username` — é o que `users/{uid}` tem.
        assertEquals("adm", adm.nome)
        assertTrue(adm.temAcesso)
    }

    /** Prazo vencido é **Expirado**, e não *Desativado*: a correção de um é prazo novo, do outro é reativar. */
    @Test
    fun `prazo vencido aparece como expirado, e o futuro nao`() = runTest(mainRule.dispatcher) {
        val vm = vm(
            repo(
                usuario("uid-1", "vencido@x.com", expiraEm = ontem),
                usuario("uid-2", "no-prazo@x.com", expiraEm = amanha),
            )
        )
        advanceUntilIdle()

        val porId = vm.uiState.value.resultados.associateBy { it.id }
        assertEquals("Expirado", porId.getValue("uid-1").situacao)
        assertEquals("Ativo", porId.getValue("uid-2").situacao)
        // Quem expirou continua **ligado**: o gesto que falta é prazo, não reativação.
        assertTrue(porId.getValue("uid-1").ativo)
    }

    @Test
    fun `filtra por inicio do email`() = runTest(mainRule.dispatcher) {
        val vm = vm(repo(usuario("uid-1", "ana@x.com"), usuario("uid-2", "bruno@x.com")))
        advanceUntilIdle()

        vm.onEmailChange("an")

        assertEquals(listOf("ana@x.com"), vm.uiState.value.resultados.map { it.email })
    }

    // --- Os gestos ---

    @Test
    fun `desativar grava e a lista passa a dizer desativado`() = runTest(mainRule.dispatcher) {
        val repo = repo(usuario("uid-ana", "ana@x.com"))
        val vm = vm(repo)
        advanceUntilIdle()

        vm.onAlternarAcesso("uid-ana", ativo = false)
        advanceUntilIdle()

        assertEquals(Triple("uid-ana", false, null), repo.acessosDefinidos.single())
        assertEquals("Desativado", vm.uiState.value.resultados.single().situacao)
    }

    /** O par importa: desativar sem reativar transforma um engano em ida ao console. */
    @Test
    fun `reativar volta o acesso`() = runTest(mainRule.dispatcher) {
        val repo = repo(usuario("uid-ana", "ana@x.com", ativo = false))
        val vm = vm(repo)
        advanceUntilIdle()

        vm.onAlternarAcesso("uid-ana", ativo = true)
        advanceUntilIdle()

        assertEquals("Ativo", vm.uiState.value.resultados.single().situacao)
    }

    /**
     * **Reativar não apaga o prazo.** São duas decisões, e juntá-las faria a reativação conceder mais do
     * que o gesto diz — quem foi religado com prazo de ontem continua expirado, e é isso que a tela tem
     * de mostrar.
     */
    @Test
    fun `reativar preserva o prazo que existia`() = runTest(mainRule.dispatcher) {
        val repo = repo(usuario("uid-ana", "ana@x.com", ativo = false, expiraEm = ontem))
        val vm = vm(repo)
        advanceUntilIdle()

        vm.onAlternarAcesso("uid-ana", ativo = true)
        advanceUntilIdle()

        assertEquals(Triple("uid-ana", true, ontem), repo.acessosDefinidos.single())
        assertEquals("Expirado", vm.uiState.value.resultados.single().situacao)
    }

    @Test
    fun `definir prazo grava o fim do dia escolhido`() = runTest(mainRule.dispatcher) {
        val repo = repo(usuario("uid-ana", "ana@x.com"))
        val vm = vm(repo)
        advanceUntilIdle()

        vm.onDefinirPrazo("uid-ana", "31/12/2026")
        advanceUntilIdle()

        val (_, ativo, prazo) = repo.acessosDefinidos.single()
        assertTrue(ativo)
        assertEquals("31/12/2026", vm.uiState.value.resultados.single().prazo)
        // Fim do dia, e não início: o dia 31 ainda vale (ver `PrazoDeAcesso`).
        assertTrue(requireNotNull(prazo) > agora)
    }

    /** Texto em branco é *sem prazo* — é como se tira um prazo definido por engano. */
    @Test
    fun `prazo em branco tira o prazo`() = runTest(mainRule.dispatcher) {
        val repo = repo(usuario("uid-ana", "ana@x.com", expiraEm = amanha))
        val vm = vm(repo)
        advanceUntilIdle()

        vm.onDefinirPrazo("uid-ana", "")
        advanceUntilIdle()

        assertEquals(Triple("uid-ana", true, null), repo.acessosDefinidos.single())
        assertEquals("", vm.uiState.value.resultados.single().prazo)
    }

    /** Definir prazo **não religa** quem está desativado: quem foi desligado por gesto volta por gesto. */
    @Test
    fun `definir prazo nao reativa quem esta desativado`() = runTest(mainRule.dispatcher) {
        val repo = repo(usuario("uid-ana", "ana@x.com", ativo = false))
        val vm = vm(repo)
        advanceUntilIdle()

        vm.onDefinirPrazo("uid-ana", "31/12/2026")
        advanceUntilIdle()

        assertFalse(repo.acessosDefinidos.single().second)
        assertEquals("Desativado", vm.uiState.value.resultados.single().situacao)
    }

    // --- A política antes do gesto (D1) ---

    /**
     * Gerir acesso é **`ADM`-only** (ADR-0021 D1), e a barreira é dupla: a tela não desenha o botão, e o
     * ViewModel recusa. A seção já é `ADM`-only no menu — a pergunta se repete aqui porque a ausência do
     * botão não é fronteira.
     */
    @Test
    fun `gestor ve a lista e nao desativa ninguem`() = runTest(mainRule.dispatcher) {
        val repo = repo(usuario("uid-ana", "ana@x.com"))
        val vm = vm(repo, sessao = FakeSessaoUsuario.plataforma(Papel.GESTOR.name))
        advanceUntilIdle()

        assertFalse(vm.uiState.value.podeGerir)
        assertEquals(1, vm.uiState.value.resultados.size)

        vm.onAlternarAcesso("uid-ana", ativo = false)
        advanceUntilIdle()

        assertTrue(repo.acessosDefinidos.isEmpty())
    }

    @Test
    fun `sem sessao a lista nasce sem gestao — fail-closed`() = runTest(mainRule.dispatcher) {
        val repo = repo(usuario("uid-ana", "ana@x.com"))
        val vm = vm(repo, sessao = FakeSessaoUsuario(contexto = null))
        advanceUntilIdle()

        assertFalse(vm.uiState.value.podeGerir)

        vm.onAlternarAcesso("uid-ana", ativo = false)
        advanceUntilIdle()

        assertTrue(repo.acessosDefinidos.isEmpty())
    }

    /**
     * **A falha não passa em silêncio** ([ADR-0032] D4): quem desativou precisa saber que não desativou.
     * O caso trava as duas metades — o estado sai do "processando" e a lista continua dizendo a verdade.
     */
    @Test
    fun `falha ao gravar nao mente sobre a situacao`() = runTest(mainRule.dispatcher) {
        val repo = repo(usuario("uid-ana", "ana@x.com")).apply { falharAoDefinir = true }
        val vm = vm(repo)
        advanceUntilIdle()

        vm.onAlternarAcesso("uid-ana", ativo = false)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isProcessing)
        assertEquals("Ativo", vm.uiState.value.resultados.single().situacao)
    }
}
