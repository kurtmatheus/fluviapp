package dev.matheus.fluviapp.ui.viewmodel.passagem

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.matheus.fluviapp.services.bilhete.GaleriaDeBilhetes
import dev.matheus.fluviapp.services.repository.operacoes.SessaoUsuario
import dev.matheus.fluviapp.services.repository.passagem.PassagemRepository
import dev.matheus.fluviapp.ui.states.passagem.BilheteDigital
import dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.ColetorDeReferencias
import dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.paraBilhete
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * **O bilhete digital** — uma tela só, alcançada de dois lugares.
 *
 * *"Mesmo bilhete"* (analista, 2026-08-13): a emissão navega para cá logo depois de emitir, e a consulta de
 * uma passagem já emitida — quando existir — navega para o mesmo destino. A alternativa seria o bilhete ser
 * um pedaço da tela de emissão, e a consulta ganhar outra tela que desenha o mesmo documento: **dois lugares
 * desenhando a mesma coisa é como eles passam a divergir**.
 *
 * ### O que ele faz, na ordem
 *
 * 1. **procura o arquivo** na galeria (decisão: *procurar antes*). Achou, mostra e acabou — reabrir bilhete é
 *    o caso comum do balcão, e regenerar o que já existe é trabalho à toa;
 * 2. **não achou, carrega e desenha**: a passagem vem do servidor e a junção resolve os ids (é aqui que o
 *    `ColetorDeReferencias.completas` ganha, enfim, um consumidor);
 * 3. **salva ao ver** (decisão): pré-visualizar e ter o arquivo são o mesmo ato — não há um segundo gesto de
 *    "salvar" a ser esquecido com a fila esperando.
 *
 * Regenerar continua legítimo em qualquer momento: o arquivo é **cache de conveniência** e o dado de origem
 * está no Firestore ([ADR-0017] D5).
 */
@HiltViewModel
class BilheteViewModel @Inject constructor(
    private val passagemRepository: PassagemRepository,
    private val coletorDeReferencias: ColetorDeReferencias,
    private val galeria: GaleriaDeBilhetes,
    private val sessaoUsuario: SessaoUsuario,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BilheteUiState())
    val uiState: StateFlow<BilheteUiState> = _uiState.asStateFlow()

    fun carregar(idPassagem: String?) {
        if (idPassagem.isNullOrBlank()) {
            _uiState.update { it.copy(carregando = false, naoEncontrado = true) }
            return
        }

        // Procurar antes: se o arquivo existe, o bilhete já está pronto e nada precisa ser desenhado.
        val jaSalvo = galeria.procurar(idPassagem)
        _uiState.update { it.copy(carregando = true, arquivo = jaSalvo) }

        viewModelScope.launch {
            val passagem = runCatching { passagemRepository.obterPorId(idPassagem) }.getOrNull()
            if (passagem == null) {
                _uiState.update { it.copy(carregando = false, naoEncontrado = true) }
                return@launch
            }

            val referencias = runCatching { coletorDeReferencias.completas(passagem) }.getOrNull()
            val agencia = sessaoUsuario.atual()?.empresaAtivaNome.orEmpty()

            _uiState.update {
                it.copy(
                    carregando = false,
                    bilhete = passagem.paraBilhete(referencias ?: ReferenciasVazias, agencia),
                )
            }
        }
    }

    /**
     * Chamado pela captura, quando o desenho ficou pronto.
     *
     * **Só grava se ainda não havia arquivo**: reabrir um bilhete já salvo não precisa reescrever o mesmo
     * PNG, e a regravação existe para o caso de regeneração — que é quando [carregar] não achou nada.
     */
    fun aoCapturar(imagem: ImageBitmap) {
        val bilhete = _uiState.value.bilhete ?: return
        if (_uiState.value.arquivo != null) return

        val salvo = galeria.salvar(bilhete.idPassagem, imagem.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false))
        _uiState.update { it.copy(arquivo = salvo) }
    }

    /** A URI para o `ACTION_SEND` — o gesto que **entrega** o bilhete ao passageiro, e que continua existindo. */
    fun uriParaCompartilhar(): Uri? =
        _uiState.value.arquivo ?: _uiState.value.bilhete?.let { galeria.paraCompartilhar(it.idPassagem) }

    private companion object {
        val ReferenciasVazias = dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.ReferenciasDaPassagem()
    }
}

/**
 * O estado do bilhete.
 *
 * [arquivo] presente significa **já está na galeria** — e é o que decide entre gravar e não gravar depois da
 * captura, e o que o compartilhar usa.
 */
data class BilheteUiState(
    val carregando: Boolean = true,
    val bilhete: BilheteDigital? = null,
    val arquivo: Uri? = null,
    val naoEncontrado: Boolean = false,
)