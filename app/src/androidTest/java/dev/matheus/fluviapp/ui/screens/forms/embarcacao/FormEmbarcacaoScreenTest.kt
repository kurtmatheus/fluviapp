package dev.matheus.fluviapp.ui.screens.forms.embarcacao

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.viagem.Empresa
import dev.matheus.fluviapp.domain.viagem.TipoEmbarcacao
import dev.matheus.fluviapp.ui.states.FormEmbarcacaoUiState
import dev.matheus.fluviapp.ui.theme.FluviAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * O cadastro da Embarcação em tela, no mesmo molde mínimo da Empresa (`FormEmpresaScreenTest`):
 * `ComponentActivity` vazia, estado fabricado, sem Hilt, sem Firestore e sem navegação. O que se afirma é
 * o contrato da apresentação — *dado este estado, isto aparece; dado este toque, isto é avisado*.
 *
 * Aqui há uma afirmação que o formulário da Empresa não tinha o que fazer: **a tela muda de forma conforme
 * o tipo**. A regra "não se vende veículo para uma lancha" foi decidida no domínio, provada em JVM no
 * `TipoEmbarcacaoTest` e aplicada no `FormEmbarcacaoViewModel` — mas quem promete que o campo *desaparece*
 * é a tela, e essa promessa só se verifica renderizando. É por isso que estes casos existem em aparelho, e
 * não em mais um teste de ViewModel.
 */
class FormEmbarcacaoScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun texto(id: Int) = composeTestRule.activity.getString(id)

    private val empresas = listOf(
        Empresa("1", "NAVEGA MODELO", "Navega Modelo LTDA", "00000000000100", "Cais 1", "9999-0001", ""),
    )

    private fun montarTela(
        uiState: FormEmbarcacaoUiState = FormEmbarcacaoUiState(listaEmpresas = empresas),
        onTipoChange: (String) -> Unit = {},
        onClickSalvar: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            FluviAppTheme {
                FormEmbarcacaoScreen(
                    uiState = uiState,
                    onTipoChange = onTipoChange,
                    onClickSalvar = onClickSalvar,
                )
            }
        }
    }

    private fun estadoCom(tipo: TipoEmbarcacao) =
        FormEmbarcacaoUiState(listaEmpresas = empresas, tipo = tipo)

    // --- O que a tela mostra ---

    @Test
    fun formEmbarcacao_estadoInicial_exibeFormularioVazio() {
        montarTela()

        composeTestRule.onNodeWithText(texto(R.string.subtitle_cadastrar_nova_embarcacao)).assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(R.string.label_nome_embarcacao)).assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(R.string.label_tipo_embarcacao)).assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(R.string.label_empresa)).assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(R.string.btn_salvar)).performScrollTo().assertIsDisplayed()
    }

    /** Editar mostra o tipo já escolhido — o dropdown exibe o rótulo, não o `name` do enum. */
    @Test
    fun formEmbarcacao_naEdicao_exibeORotuloDoTipoGravado() {
        montarTela(estadoCom(TipoEmbarcacao.FERRY_BOAT).copy(titulo = R.string.subtitle_editar_embarcacao))

        composeTestRule.onNodeWithText(texto(R.string.subtitle_editar_embarcacao)).assertIsDisplayed()
        composeTestRule.onNodeWithText(TipoEmbarcacao.FERRY_BOAT.rotulo).assertIsDisplayed()
    }

    // --- A forma da tela depende do tipo ---

    /**
     * Sem tipo escolhido a pergunta não existe: perguntar quantos carros cabem antes de saber o que é a
     * embarcação é oferecer uma resposta que pode não ter sentido nenhum.
     */
    @Test
    fun formEmbarcacao_semTipoEscolhido_naoPerguntaCapacidadeDeVeiculo() {
        montarTela()

        composeTestRule.onNodeWithText(texto(R.string.label_capacidade_veiculo)).assertDoesNotExist()
        // As outras capacidades não dependem do tipo e seguem na tela.
        composeTestRule.onNodeWithText(texto(R.string.label_capacidade_camarote)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun formEmbarcacao_ferryBoat_perguntaCapacidadeDeVeiculo() {
        montarTela(estadoCom(TipoEmbarcacao.FERRY_BOAT))

        composeTestRule.onNodeWithText(texto(R.string.label_capacidade_veiculo)).performScrollTo().assertIsDisplayed()
    }

    /** A regra do domínio, visível: para a lancha o campo **some** — não fica cinza, não fica vazio. */
    @Test
    fun formEmbarcacao_lancha_naoPerguntaCapacidadeDeVeiculo() {
        montarTela(estadoCom(TipoEmbarcacao.LANCHA))

        composeTestRule.onNodeWithText(TipoEmbarcacao.LANCHA.rotulo).assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(R.string.label_capacidade_veiculo)).assertDoesNotExist()
        composeTestRule.onNodeWithText(texto(R.string.label_capacidade_suite2)).performScrollTo().assertIsDisplayed()
    }

    // --- O que a tela avisa ---

    /**
     * O dropdown devolve **rótulo**, não tipo: é o ViewModel que traduz (`TipoEmbarcacao.porRotulo`).
     * Este caso trava justamente a fronteira — se alguém trocar o `listaItens` para `entries.map { name }`,
     * a tela passaria a mostrar `FERRY_BOAT` e a tradução quebraria sem nenhum teste de JVM reclamar.
     */
    @Test
    fun formEmbarcacao_aoEscolherOTipo_avisaORotuloEscolhido() {
        val escolhidos = mutableListOf<String>()
        montarTela(onTipoChange = { escolhidos += it })

        composeTestRule.onNodeWithText(texto(R.string.label_tipo_embarcacao)).performClick()
        composeTestRule.onNodeWithText(TipoEmbarcacao.LANCHA.rotulo).performClick()

        assertEquals(listOf(TipoEmbarcacao.LANCHA.rotulo), escolhidos)
    }

    /** Os três tipos são oferecidos — a lista da tela é a do domínio, sem curadoria de UI. */
    @Test
    fun formEmbarcacao_aoAbrirOTipo_ofereceOsTresTipos() {
        montarTela()

        composeTestRule.onNodeWithText(texto(R.string.label_tipo_embarcacao)).performClick()

        TipoEmbarcacao.entries.forEach {
            composeTestRule.onNodeWithText(it.rotulo).assertIsDisplayed()
        }
    }

    /** O erro do tipo é estado, não exceção: entra pelo `uiState` e sai como texto. */
    @Test
    fun formEmbarcacao_comErroDeTipo_exibeMensagem() {
        montarTela(FormEmbarcacaoUiState(listaEmpresas = empresas, isTipoError = true))

        composeTestRule.onNodeWithText(texto(R.string.error_camp_obrig)).assertIsDisplayed()
    }

    @Test
    fun formEmbarcacao_aoTocarEmSalvar_avisa() {
        var salvou = 0
        montarTela(onClickSalvar = { salvou++ })

        composeTestRule.onNodeWithText(texto(R.string.btn_salvar)).performScrollTo().performClick()

        assertEquals(1, salvou)
    }
}