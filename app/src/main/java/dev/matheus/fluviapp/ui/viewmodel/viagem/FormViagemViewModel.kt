package dev.matheus.fluviapp.ui.viewmodel.viagem

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.passagem.ModoPassagem
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Categoria.MUNICIPIO
import dev.matheus.fluviapp.domain.passagem.ClasseVeiculo
import dev.matheus.fluviapp.domain.extrairPorDescricao
import dev.matheus.fluviapp.domain.mappers.ViagemDadosViagemMapper
import dev.matheus.fluviapp.domain.viagem.Navio
import dev.matheus.fluviapp.domain.viagem.TarifaViagem
import dev.matheus.fluviapp.domain.viagem.Viagem
import dev.matheus.fluviapp.navigation.navcomposables.viagem.ID_VIAGEM_ARGUMENT
import dev.matheus.fluviapp.services.repository.cadastro.ConstanteRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.NavioRepository
import dev.matheus.fluviapp.services.repository.firebase.ViagemRepository
import dev.matheus.fluviapp.ui.states.FormViagemUiState
import dev.matheus.fluviapp.ui.states.TarifaInputUiState
import dev.matheus.fluviapp.ui.viewmodel.helpers.viagem.validarViagem
import java.math.BigDecimal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Cadastro/edição de viagem no molde refatorado (cadastro-modulos §7.2): VM dona do estado (sem
 * FormHelper); eventos são métodos; validação pura; sucesso via evento one-shot; cargas suspensas
 * (sem runBlocking); arg de rota opcional; sem Context.
 */
@HiltViewModel
class FormViagemViewModel @Inject constructor(
    private val empresaRepository: EmpresaRepository,
    private val navioRepository: NavioRepository,
    private val constanteRepository: ConstanteRepository,
    private val viagemRepository: ViagemRepository,
    private val viagemDadosViagemMapper: ViagemDadosViagemMapper,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val idViagem: String = savedStateHandle.get<String>(ID_VIAGEM_ARGUMENT).orEmpty()

    private val _uiState = MutableStateFlow(FormViagemUiState())
    val uiState: StateFlow<FormViagemUiState> = _uiState.asStateFlow()

    private val _sucesso = Channel<Unit>(Channel.BUFFERED)
    val sucesso = _sucesso.receiveAsFlow()

    init {
        // Sequenciado: a edição precisa da `listaEmpresas` pronta para resolver o empresaId (ADR-0008).
        viewModelScope.launch {
            carregarFontes()
            if (idViagem.isNotBlank()) carregarParaEdicao()
        }
    }

    private suspend fun carregarFontes() {
        // Um input de tarifa por célula do catálogo (ADR-0013): acomodação (passageiro) + classe de veículo.
        // Moto sai da lista — sua tarifa é a regra por cilindrada (Fase 3), não uma célula cadastrada.
        val acomodacoes = ModoPassagem.acomodacoes()
            .map { TarifaInputUiState(chave = it.name, grupoTitulo = R.string.label_grupo_tarifa_acomodacao) }
        val veiculos = ClasseVeiculo.entries
            .filterNot { it == ClasseVeiculo.MOTO }
            .map { TarifaInputUiState(chave = it.name, grupoTitulo = R.string.label_grupo_tarifa_veiculo) }
        _uiState.update {
            it.copy(
                listaEmpresas = empresaRepository.obterTodas(),
                listaMunicipios = constanteRepository.obterTodosPorCategoria(MUNICIPIO.name),
                tarifas = acomodacoes + veiculos,
            )
        }
    }

    private suspend fun carregarParaEdicao() {
        val card = viagemDadosViagemMapper.map(viagemRepository.obterPorId(idViagem))
        // Prefill das tarifas já cadastradas, casadas por chave (acomodação sem tarifa fica em branco).
        val valores = viagemRepository.obterTarifas(idViagem).associate { it.chave to it.valor }
        _uiState.update { st ->
            st.copy(
                titulo = R.string.subtitle_editar_viagem,
                empresa = card.empresa,
                navio = card.navio,
                trechoOrigem = card.origem,
                trechoDestino = card.destino,
                navioDesabilitado = card.empresa.isBlank(),
                trechoDestinoDesabilitado = card.origem.isBlank(), // corrige o prefill (lia estado velho)
                listaNavios = naviosDaEmpresa(card.empresa),
                tarifas = st.tarifas.map { input ->
                    valores[input.chave]?.let { input.copy(valor = it.paraCampoMoeda()) } ?: input
                },
            )
        }
    }

    fun onEmpresaChange(empresa: String) {
        viewModelScope.launch {
            val navios = naviosDaEmpresa(empresa)
            _uiState.update {
                it.copy(
                    empresa = empresa,
                    isEmpresaError = false,
                    navioDesabilitado = false,
                    navio = "",
                    listaNavios = navios,
                )
            }
        }
    }

    /**
     * Navios da empresa selecionada — filtra pelo **empresaId** estável (ADR-0008), resolvido do nome
     * via `listaEmpresas` em cache. Rename-safe: o vínculo não depende do rótulo. Sem id resolvido →
     * lista vazia (nunca casa `empresaId == ""`, que pegaria navios sem vínculo).
     */
    private suspend fun naviosDaEmpresa(nomeEmpresa: String): List<Navio> {
        val empresaId = _uiState.value.listaEmpresas.firstOrNull { it.nome == nomeEmpresa }?.id
            ?: return emptyList()
        return navioRepository.obterTodos().filter { it.empresaId == empresaId }
    }

    fun onNavioChange(navio: String) = _uiState.update { it.copy(navio = navio, isNavioError = false) }

    fun onTrechoOrigemChange(trecho: String) = _uiState.update {
        it.copy(trechoOrigem = trecho, isTrechoOrigemError = false, trechoDestinoDesabilitado = false)
    }

    fun onLimparTrechoOrigem() = _uiState.update {
        it.copy(trechoOrigem = "", isTrechoOrigemError = false, trechoDestinoDesabilitado = true)
    }

    fun onTrechoDestinoChange(trecho: String) = _uiState.update {
        it.copy(trechoDestino = trecho, isTrechoDestinoError = false)
    }

    fun onLimparTrechoDestino() = _uiState.update {
        it.copy(trechoDestino = "", isTrechoDestinoError = false)
    }

    fun onTarifaChange(chave: String, valor: String) = _uiState.update { st ->
        st.copy(
            tarifas = st.tarifas.map {
                if (it.chave == chave) it.copy(valor = valor, isError = false) else it
            },
        )
    }

    fun salvar() {
        val erros = validarViagem(_uiState.value)
        if (!erros.valido) {
            _uiState.update {
                it.copy(
                    isEmpresaError = erros.empresa,
                    isNavioError = erros.navio,
                    isTrechoOrigemError = erros.trechoOrigem,
                    isTrechoDestinoError = erros.trechoDestino,
                    tarifas = it.tarifas.map { t -> t.copy(isError = t.chave in erros.tarifasInvalidas) },
                )
            }
            return
        }

        _uiState.update { it.copy(isProcessando = true) }
        viewModelScope.launch {
            val s = _uiState.value
            try {
                val navio = s.listaNavios.extrairPorDescricao(s.navio)
                val empresa = s.listaEmpresas.first { it.nome == s.empresa }
                val origem = s.listaMunicipios.extrairPorDescricao(s.trechoOrigem)
                val destino = s.listaMunicipios.extrairPorDescricao(s.trechoDestino)

                // Só acomodações com valor preenchido viram célula (branco = não ofertada, ADR-0013). O
                // viagemId é (re)carimbado no repo — na criação o id só existe lá.
                val tarifas = s.tarifas
                    .filter { it.valor.isNotBlank() }
                    .map { TarifaViagem(idViagem, it.chave, it.valor.trim().replace(",", ".").toDouble()) }

                viagemRepository.salvar(
                    Viagem(
                        id = idViagem, // "" na criação → auto-id no repo
                        codigo = "", // derivado na persistência (resolve o nome do navio pelo id)
                        origem = origem.descricaoNome,
                        destino = destino.descricaoNome,
                        // Vínculo vivo só por id (ADR-0008 Fase 3); nomes resolvidos na fronteira.
                        empresaId = empresa.id,
                        navioId = navio.id,
                    ),
                    tarifas,
                )
                _sucesso.send(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "salvar: ${e.message}", e)
                _uiState.update { it.copy(isProcessando = false) }
            }
        }
    }

    /** Double armazenado → texto do campo, sem zeros/`.0` supérfluos (300.0 → "300", 150.5 → "150.5"). */
    private fun Double.paraCampoMoeda(): String =
        BigDecimal.valueOf(this).stripTrailingZeros().toPlainString()

    private companion object {
        const val TAG = "formViagemViewModel"
    }
}
