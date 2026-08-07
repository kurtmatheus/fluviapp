package dev.matheus.fluviapp.ui.viewmodel.porto

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.localidade.Localidade
import dev.matheus.fluviapp.domain.porto.Porto
import dev.matheus.fluviapp.navigation.navcomposables.porto.ID_PORTO_ARGUMENT
import dev.matheus.fluviapp.services.repository.cadastro.localidade.LocalidadeRepository
import dev.matheus.fluviapp.services.repository.cadastro.porto.PortoRepository
import dev.matheus.fluviapp.ui.states.ErroNomePorto
import dev.matheus.fluviapp.ui.states.FormPortoUiState
import dev.matheus.fluviapp.ui.states.LocalidadeOpcao
import dev.matheus.fluviapp.ui.viewmodel.helpers.porto.validarPorto
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Cadastro/edição de porto no molde do ADR-0006: VM dona do estado, eventos como métodos, validação
 * pura, sucesso por evento one-shot, cargas suspensas e sem Context.
 *
 * O que esta tela tem de próprio é **depender de outra entidade**. Duas consequências, e as duas estão
 * em [carregarLocalidades] e [carregar]:
 *
 * - o dropdown oferece só localidades **ativas** — inativar um município é dizer "não escolham mais
 *   este", e a lista de escolha é onde essa frase se cumpre;
 * - a edição **resolve por id, sem filtrar** ([garantirOpcaoDaLocalidadeAtual]). É o outro lado da mesma
 *   regra, e o porto é o primeiro a precisar dele: um porto cadastrado antes de a localidade sair de uso
 *   continua mostrando onde fica, em vez de abrir o formulário com o campo misteriosamente vazio.
 *
 * ### Por que as duas cargas são independentes
 *
 * Elas nasceram numa função só, sequencial, e isso produziu um bug em campo: com as regras da coleção
 * `portos` ainda não publicadas, o servidor negava o listener, o primeiro snapshot **nunca chegava** —
 * `obterTodos` espera por ele de propósito, para não confundir *vazio* com *ainda não chegou* — e a
 * espera engolia junto a lista de **localidades**, que já tinha chegado. O sintoma foi um dropdown vazio
 * acusando a coleção errada.
 *
 * Agora cada fonte tem a sua corrotina e o seu `update`: as localidades servem para **escolher**, os
 * portos só para **não repetir**, e nada obriga a segunda a chegar para a primeira aparecer. O que se
 * perde quando os portos não chegam é só a checagem de homônimo — que é verificação de cadastro, não
 * garantia (a do servidor é a F8 do ADR-0016).
 */
@HiltViewModel
class FormPortoViewModel @Inject constructor(
    private val portoRepository: PortoRepository,
    private val localidadeRepository: LocalidadeRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // "" = criação; id preenchido = edição (arg de rota opcional, sem sentinela "null").
    private val idPorto: String = savedStateHandle.get<String>(ID_PORTO_ARGUMENT).orEmpty()

    private val _uiState = MutableStateFlow(FormPortoUiState())
    val uiState: StateFlow<FormPortoUiState> = _uiState.asStateFlow()

    private val _sucesso = Channel<Unit>(Channel.BUFFERED)
    val sucesso = _sucesso.receiveAsFlow()

    init {
        // Duas corrotinas, e não uma sequência: uma fonte que demore (ou que o servidor negue) não pode
        // segurar a outra. Ver o bloco "Por que as duas cargas são independentes", acima.
        viewModelScope.launch { carregarLocalidades() }
        viewModelScope.launch { carregarOutrosPortos() }
    }

    /**
     * As localidades **para escolher** — e, na edição, o porto que está sendo editado.
     *
     * O `update` das opções vem **antes** de carregar o porto: assim o dropdown já está utilizável
     * mesmo que a leitura do porto demore, e a ordem também protege a opção inativa que
     * [garantirOpcaoDaLocalidadeAtual] acrescenta — se as opções chegassem depois, elas a apagariam.
     */
    private suspend fun carregarLocalidades() {
        val opcoes = localidadeRepository.obterTodas()
            .filter { it.ativo }
            .map { it.paraOpcao() }
            .sortedBy { it.rotulo }

        _uiState.update { it.copy(localidades = opcoes) }

        if (idPorto.isNotBlank()) carregar()
    }

    /**
     * Os portos que já existem, **para não repetir**. Exclui o que está sendo editado, e isso não é
     * detalhe: sem a exclusão, salvar um porto sem renomeá-lo o acusaria de ser duplicata de si mesmo.
     */
    private suspend fun carregarOutrosPortos() {
        val outros = portoRepository.obterTodos().filterNot { it.id == idPorto }

        _uiState.update { it.copy(outrosPortos = outros) }
    }

    fun onNomeChange(v: String) = _uiState.update { it.copy(nome = v, erroNome = ErroNomePorto.NENHUM) }

    /**
     * A tela devolve o **rótulo** escolhido no dropdown; quem o traduz para id é o VM. Rótulo que não
     * casa com nenhuma opção vira id vazio — é o que a validação chama de "sem localidade", e é melhor
     * do que guardar um texto que não aponta para lugar nenhum.
     *
     * Trocar de localidade **apaga a queixa de duplicidade**, e só ela: o erro é do par
     * `(nome, localidade)`, então mexer em qualquer um dos lados pode tê-lo resolvido. O "campo
     * obrigatório" do nome sobrevive, porque esse é do nome sozinho e continua verdadeiro.
     */
    fun onLocalidadeChange(rotulo: String) = _uiState.update {
        it.copy(
            localidadeId = it.localidades.firstOrNull { opcao -> opcao.rotulo == rotulo }?.id.orEmpty(),
            isLocalidadeError = false,
            erroNome = if (it.erroNome == ErroNomePorto.DUPLICADO) ErroNomePorto.NENHUM else it.erroNome,
        )
    }

    private suspend fun carregar() {
        portoRepository.obterPorId(idPorto)?.let { porto ->
            garantirOpcaoDaLocalidadeAtual(porto.localidadeId)
            _uiState.update {
                it.copy(
                    titulo = R.string.subtitle_editar_porto,
                    nome = porto.nome,
                    localidadeId = porto.localidadeId,
                )
            }
        }
    }

    /**
     * A localidade deste porto pode não estar entre as opções — porque foi inativada depois. Resolvê-la
     * por id e acrescentá-la é o que faz o delete lógico significar "não escolham mais" em vez de
     * "nunca existiu".
     *
     * Ela entra **só para este documento**: quem abrir o formulário de outro porto, ou um cadastro novo,
     * não a verá na lista.
     */
    private suspend fun garantirOpcaoDaLocalidadeAtual(localidadeId: String) {
        if (localidadeId.isBlank()) return
        if (_uiState.value.localidades.any { it.id == localidadeId }) return

        val inativa = localidadeRepository.obterPorId(localidadeId) ?: return
        _uiState.update {
            it.copy(localidades = (it.localidades + inativa.paraOpcao()).sortedBy { o -> o.rotulo })
        }
    }

    fun salvar() {
        val estado = _uiState.value
        val erros = validarPorto(estado)
        if (!erros.valido) {
            _uiState.update { it.copy(erroNome = erros.nome, isLocalidadeError = erros.localidade) }
            return
        }

        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            try {
                portoRepository.salvar(
                    Porto(
                        id = idPorto, // "" na criação → auto-id no repo
                        nome = estado.nome.trim(),
                        localidadeId = estado.localidadeId,
                        // Editar não ressuscita nem enterra: o `ativo` é do gesto de excluir, e toda
                        // gravação daqui é de porto em uso.
                        ativo = true,
                    )
                )
                _sucesso.send(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "salvar: ${e.message}", e)
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    private companion object {
        const val TAG = "formPortoViewModel"
    }
}

/** O que o formulário precisa de uma localidade: o id que grava e o rótulo que mostra. */
private fun Localidade.paraOpcao() = LocalidadeOpcao(id = id, rotulo = rotulo)