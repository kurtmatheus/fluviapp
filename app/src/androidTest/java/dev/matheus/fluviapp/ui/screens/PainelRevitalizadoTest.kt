package dev.matheus.fluviapp.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.operacoes.Atuacao
import dev.matheus.fluviapp.domain.operacoes.Funcionario.Cargo
import dev.matheus.fluviapp.domain.operacoes.Usuario.Papel
import dev.matheus.fluviapp.domain.screendata.AcaoMenu
import dev.matheus.fluviapp.domain.screendata.SecaoMenu
import dev.matheus.fluviapp.domain.screendata.acoesPorSecao
import dev.matheus.fluviapp.domain.screendata.secoesDoMenu
import dev.matheus.fluviapp.ui.components.drawer.FluviMenuDrawer
import dev.matheus.fluviapp.ui.states.InicioDaTela
import dev.matheus.fluviapp.ui.states.MainScreenState
import dev.matheus.fluviapp.ui.states.MainScreenUiState
import dev.matheus.fluviapp.ui.states.ViagemDisponivelCard
import dev.matheus.fluviapp.ui.theme.FluviAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * O painel da revitalização, em tela: **existem a Empresa, a Flotilha e as Localidades**, e mais nada.
 *
 * O menu não é fabricado à mão aqui — vem de `secoesDoMenu(ADM)` e `acoesPorSecao`, as mesmas funções de
 * domínio que a Main Screen usa em produção. É o que faz este teste valer como emenda entre as camadas:
 * se alguém revitalizar uma seção no domínio, é aqui que a tela é cobrada; e se o recorte vazar, é aqui
 * que aparece — sem Firestore, sem sessão e sem login.
 *
 * A emenda cobrou: quando a Flotilha entrou em `SECOES_REVITALIZADAS`, este teste ficou **vermelho**
 * afirmando que ela não deveria aparecer. Foi o combinado funcionando — a lista viva do domínio e a tela
 * discordaram em voz alta, em vez de a seção nova entrar sem ninguém perceber.
 */
class PainelRevitalizadoTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun texto(id: Int) = composeTestRule.activity.getString(id)

    /** O menu de quem mais enxerga no app: se nem para o ADM vazou, não vazou para ninguém. */
    private val secoesDoAdm = secoesDoMenu(Papel.ADM.name)

    private fun montarPainel(inicio: InicioDaTela = InicioDaTela.DaPlataforma) {
        composeTestRule.setContent {
            FluviAppTheme {
                MainScreen(
                    state = MainScreenUiState(
                        userName = "Odair",
                        secoesVisiveis = secoesDoAdm,
                        inicio = inicio,
                        mainScreenState = MainScreenState.HOME,
                    ),
                    acoesPorSecao = acoesPorSecao(secoesDoAdm),
                )
            }
        }
    }

    /**
     * O menu de quem **opera**: desde a F9.6 ele não é mais um subconjunto do outro.
     *
     * O `ADM` deixou de enxergar o app inteiro quando o painel da plataforma passou a valer também sem
     * vínculo, e a Passagem — que nunca foi do painel dela — só aparece aqui. Cobrar a seção acesa no menu
     * do `ADM` seria cobrá-la onde ela não deve estar.
     */
    private val secoesDoAgente =
        secoesDoMenu(Papel.OPERADOR.name, Cargo.AGENTE.name, Atuacao.AGENCIAMENTO)

    private fun montarMenu(
        secoes: List<SecaoMenu> = secoesDoAdm,
        onNavegar: (AcaoMenu) -> Unit = {},
    ) {
        composeTestRule.setContent {
            FluviAppTheme {
                FluviMenuDrawer(
                    userName = "Odair",
                    secoes = secoes,
                    acoesPorSecao = acoesPorSecao(secoes),
                    isDarkTheme = false,
                    onInicio = {},
                    onNavegar = onNavegar,
                    onToggleTheme = {},
                    onDeslogar = {},
                )
            }
        }
    }

    // --- O painel ---

    /**
     * **O Início da plataforma não é uma lista de saídas** (F8.4): ela monta o universo e não vende. O
     * sumário do painel dela é a F10; até lá, o convite a abrir o menu.
     */
    @Test
    fun painel_daPlataforma_convidaAAbrirOMenu() {
        montarPainel(InicioDaTela.DaPlataforma)

        composeTestRule.onNodeWithText(texto(R.string.msg_painel_plataforma)).assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(R.string.subtitle_viagens_disponiveis)).assertDoesNotExist()
    }

    /** O Início da empresa é "Viagens Disponíveis" — a ocorrência datada, não o cadastro. */
    @Test
    fun painel_daEmpresa_mostraViagensDisponiveis() {
        montarPainel(InicioDaTela.DaEmpresa(listOf(SAIDA_DE_EXEMPLO)))

        composeTestRule.onNodeWithText(texto(R.string.subtitle_viagens_disponiveis)).assertIsDisplayed()
        composeTestRule.onNodeWithText(SAIDA_DE_EXEMPLO.partida).assertIsDisplayed()
        composeTestRule.onNodeWithText(SAIDA_DE_EXEMPLO.embarcacao).assertIsDisplayed()
    }

    /**
     * **Os dois vazios não podem virar a mesma tela**: um manda esperar a próxima semana, o outro manda
     * procurar a plataforma. É a distinção que o `InicioDaTela` existe para preservar.
     */
    @Test
    fun painel_daEmpresa_distingueSemSaidaDeSemConcessao() {
        montarPainel(InicioDaTela.DaEmpresa(emptyList()))
        composeTestRule.onNodeWithText(texto(R.string.msg_nenhuma_viagem)).assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(R.string.msg_viagem_sem_concessao)).assertDoesNotExist()
    }

    @Test
    fun painel_semConcessao_mandaProcurarAPlataforma() {
        montarPainel(InicioDaTela.SemConcessao)

        composeTestRule.onNodeWithText(texto(R.string.msg_viagem_sem_concessao)).assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(R.string.msg_nenhuma_viagem)).assertDoesNotExist()
    }

    /**
     * O embarque saiu do painel com o domínio que o alimenta (lê o QR de uma passagem) e **não voltou com
     * a F9**: a passagem existe, o QR está no bilhete, e mesmo assim a tela do scanner continua sem porta
     * de entrada. É dívida declarada — conferir bilhete é um fluxo próprio, e ainda não tem lugar decidido
     * no painel.
     */
    @Test
    fun painel_naoOfereceOQueNaoFoiRevitalizado() {
        montarPainel()

        composeTestRule.onNodeWithContentDescription(texto(R.string.btn_embarque)).assertDoesNotExist()
    }

    // --- O menu ---

    @Test
    fun menu_mostraApenasAsSecoesRevitalizadas() {
        montarMenu()

        composeTestRule.onNodeWithText(texto(SecaoMenu.EMPRESA.titulo)).assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(SecaoMenu.EMBARCACAO.titulo)).assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(SecaoMenu.LOCALIDADE.titulo)).assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(SecaoMenu.PORTO.titulo)).assertIsDisplayed()
        // A seção da plataforma que faltava (F6.6): quem acessa o app e com que papel.
        composeTestRule.onNodeWithText(texto(SecaoMenu.USUARIOS.titulo)).assertIsDisplayed()
        // O **pool compartilhado** (F7/F8): as duas aparecem no painel da plataforma porque não têm dono,
        // e é ela quem os cura. É a única parte do menu que o outro painel também mostra.
        composeTestRule.onNodeWithText(texto(SecaoMenu.ROTA.titulo)).assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(SecaoMenu.VIAGEM.titulo)).assertIsDisplayed()

        // A Passagem **existe** desde a F9.6 e mesmo assim não está aqui: quem emite e consulta bilhete é
        // quem tem vínculo de funcionário (ADR-0016 §2). A ausência mudou de motivo — era "ainda não foi
        // refeita", virou "não é o trabalho da plataforma" —, e o menu do agente abaixo é a outra metade.
        composeTestRule.onNodeWithText(texto(SecaoMenu.PASSAGEM.titulo)).assertDoesNotExist()
        // **A Equipe é da empresa** (F6.6): o `ADM` não abre o quadro de pessoal de ninguém.
        composeTestRule.onNodeWithText(texto(SecaoMenu.EQUIPE.titulo)).assertDoesNotExist()
    }

    /**
     * **A seção acesa, em tela** (F9.6) — e o primeiro caso deste arquivo que monta o menu de quem opera.
     *
     * O agente é o menu mais estreito do app, e o que ele tem a menos importa tanto quanto o que tem: nada
     * de cadastro de plataforma, nada de Equipe. O que ele tem a mais é a razão de o cargo existir.
     */
    @Test
    fun menu_doAgente_mostraOPoolEAPassagem() {
        montarMenu(secoes = secoesDoAgente)

        composeTestRule.onNodeWithText(texto(SecaoMenu.PASSAGEM.titulo)).assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(SecaoMenu.ROTA.titulo)).assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(SecaoMenu.VIAGEM.titulo)).assertIsDisplayed()

        composeTestRule.onNodeWithText(texto(SecaoMenu.EMPRESA.titulo)).assertDoesNotExist()
        composeTestRule.onNodeWithText(texto(SecaoMenu.USUARIOS.titulo)).assertDoesNotExist()
        composeTestRule.onNodeWithText(texto(SecaoMenu.EQUIPE.titulo)).assertDoesNotExist()
    }

    /**
     * A seção abre e navega — para **cada** seção viva.
     *
     * As ações esperadas não são digitadas aqui: vêm de `AcaoMenu.de(secao)`, a mesma função que o menu
     * usa. É o que faz a cobrança acompanhar o domínio sozinha — acrescentar uma ação a uma seção passa a
     * ser exigida em tela sem que este arquivo mude, que é o oposto de uma lista repetida por seção.
     */
    private fun expandeENavega(
        secao: SecaoMenu,
        acaoTocada: AcaoMenu,
        secoes: List<SecaoMenu> = secoesDoAdm,
    ) {
        val navegadas = mutableListOf<AcaoMenu>()
        montarMenu(secoes = secoes, onNavegar = { navegadas += it })

        composeTestRule.onNodeWithText(texto(secao.titulo)).performClick()

        AcaoMenu.de(secao).forEach {
            composeTestRule.onNodeWithText(texto(it.titulo)).assertIsDisplayed()
        }

        composeTestRule.onNodeWithText(texto(acaoTocada.titulo)).performClick()

        assertEquals(listOf(acaoTocada), navegadas)
    }

    /** O corte não deixou a Empresa inacessível junto com o resto. */
    @Test
    fun menu_expandeEmpresa_eNavegaPelaAcao() =
        expandeENavega(SecaoMenu.EMPRESA, AcaoMenu.EMPRESA_NOVA)

    @Test
    fun menu_expandeFlotilha_eNavegaPelaAcao() =
        expandeENavega(SecaoMenu.EMBARCACAO, AcaoMenu.EMBARCACAO_PESQUISAR)

    @Test
    fun menu_expandeLocalidades_eNavegaPelaAcao() =
        expandeENavega(SecaoMenu.LOCALIDADE, AcaoMenu.LOCALIDADE_NOVA)

    @Test
    fun menu_expandePortos_eNavegaPelaAcao() =
        expandeENavega(SecaoMenu.PORTO, AcaoMenu.PORTO_PESQUISAR)

    @Test
    fun menu_expandeUsuarios_eNavegaPelaAcao() =
        expandeENavega(SecaoMenu.USUARIOS, AcaoMenu.USUARIO_NOVO)

    /** O pool compartilhado, que a F7 pôs no menu e este arquivo ainda não cobria. */
    @Test
    fun menu_expandeRotas_eNavegaPelaAcao() =
        expandeENavega(SecaoMenu.ROTA, AcaoMenu.ROTA_PESQUISAR)

    @Test
    fun menu_expandeViagens_eNavegaPelaAcao() =
        expandeENavega(SecaoMenu.VIAGEM, AcaoMenu.VIAGEM_NOVA)

    /**
     * A Passagem se expande no menu **do agente**, e `AcaoMenu.de` cobra o que ela oferece: hoje, só a
     * busca. Se a emissão ou a contagem voltarem ao menu, este caso fica vermelho sem ser editado.
     */
    @Test
    fun menu_expandePassagens_eNavegaPelaAcao() =
        expandeENavega(SecaoMenu.PASSAGEM, AcaoMenu.PASSAGEM_PESQUISAR, secoesDoAgente)
}

private val SAIDA_DE_EXEMPLO = ViagemDisponivelCard(
    id = "v1@2026-08-11",
    viagemId = "v1",
    partida = "Terça-feira, 11/08 · 18:00",
    rota = "Porto de Val-de-Cães · Belém/PA → Porto de Parintins · Parintins/AM",
    embarcacao = "F/B Modelo",
    chegada = "Qui 00:00",
)