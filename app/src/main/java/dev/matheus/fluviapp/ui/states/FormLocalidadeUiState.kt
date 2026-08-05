package dev.matheus.fluviapp.ui.states

import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.localidade.Uf

/**
 * Estado do formulário de localidade — puro (só dados + flags), no molde do ADR-0006.
 *
 * `codigoIbge` guarda **só dígitos** (teclado numérico + filtro), como o CNPJ da Empresa. `uf` é o tipo do
 * domínio e não texto: a lista é fechada, então não há grafia a validar.
 *
 * [buscaIbge] é o estado da **ajuda de digitação** — nunca do salvamento. O formulário salva em qualquer
 * um dos seus valores; ela só governa o que a tela avisa enquanto a consulta acontece.
 */
data class FormLocalidadeUiState(
    val titulo: Int = R.string.subtitle_cadastrar_nova_localidade,

    val codigoIbge: String = "",
    val isCodigoIbgeError: Boolean = false,

    val municipio: String = "",
    val isMunicipioError: Boolean = false,

    val uf: Uf? = null,
    val isUfError: Boolean = false,

    val buscaIbge: BuscaIbge = BuscaIbge.Ociosa,

    val isProcessing: Boolean = false,
) {

    /** O código está completo o bastante para valer uma consulta? Sete dígitos, nem um a menos. */
    val podeConsultarIbge: Boolean get() = codigoIbge.length == TAMANHO_CODIGO_IBGE

    private companion object {
        const val TAMANHO_CODIGO_IBGE = 7
    }
}

/**
 * O que a tela mostra sobre a consulta ao IBGE. São quatro estados porque *não perguntei ainda*,
 * *perguntando*, *não é município* e *não deu para perguntar* pedem respostas diferentes de quem está
 * cadastrando — e dois deles não são culpa de quem digitou.
 */
sealed interface BuscaIbge {
    data object Ociosa : BuscaIbge
    data object Consultando : BuscaIbge

    /** O serviço respondeu e o código não existe: aí sim é o código. */
    data object NaoEncontrado : BuscaIbge

    /** Sem rede ou serviço fora: siga digitando à mão, o cadastro não depende disto. */
    data object Indisponivel : BuscaIbge
}