package dev.matheus.fluviapp.ui.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.matheus.fluviapp.domain.operacoes.Funcionario
import dev.matheus.fluviapp.domain.screendata.secoesDoMenu
import dev.matheus.fluviapp.preferences.PreferencesKey
import com.google.firebase.auth.FirebaseAuth
import dev.matheus.fluviapp.services.repository.firebase.SincronizacaoSessao
import dev.matheus.fluviapp.telemetry.EstadoSincronizacao
// REVITALIZAÇÃO: voltam com as seções Passagem / Equipe.
// import dev.matheus.fluviapp.services.repository.operacoes.FuncionarioRepository
// import dev.matheus.fluviapp.services.repository.firebase.PassagemFirestoreRepository
import dev.matheus.fluviapp.ui.viewmodel.helpers.inicio.fluxoDoInicio
import dev.matheus.fluviapp.services.repository.cadastro.localidade.LocalidadeRepository
import dev.matheus.fluviapp.services.repository.cadastro.porto.PortoRepository
import dev.matheus.fluviapp.services.repository.cadastro.rota.RotaRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmbarcacaoRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.ViagemRepository
import dev.matheus.fluviapp.services.repository.operacoes.EscopoDaSessao
import dev.matheus.fluviapp.ui.states.MainScreenState
import dev.matheus.fluviapp.ui.states.MainScreenUiState
import dev.matheus.fluviapp.util.Relogio
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * **Revitalização (ADR-0020):** só a Empresa está viva ponta a ponta, e este ViewModel é onde o painel
 * para de fingir o contrário. Saíram daqui as dependências dos domínios ainda não refeitos — viagem,
 * passagem e funcionário —, e com elas a lista de próximas viagens, o pull-to-refresh e a sincronização
 * de três coleções que a home nem exibe mais. O que resta é o mínimo do painel: **quem está logado, o que
 * ele vê no menu, e sair**.
 *
 * As linhas comentadas ficam no lugar de propósito, com o repositório real ao lado: cada uma volta quando
 * a seção correspondente entrar em [dev.matheus.fluviapp.domain.screendata.SECOES_REVITALIZADAS], e assim
 * a volta é uma leitura, não uma arqueologia no histórico.
 *
 * **A lista de viagens voltou na F8.4 — outra** (decisão do analista, 2026-08-10). O `viagemRepository` e
 * o `viagemMapper` que estavam aqui eram da Viagem-trecho, e restaurá-los teria sido ressuscitar o que a
 * F8.0 demoliu. O que voltou é o **Início da empresa**: a lista de `ViagemSemana` — ocorrências datadas —
 * recortada pela concessão, sob o subtítulo *Viagens Disponíveis*.
 *
 * E ela não é mais uma lista só para todo mundo: **quem decide o que a tela mostra é o domínio**
 * (`inicioDoPainel`), pelo mesmo `EscopoDoPool` que recorta busca e cadastro. A plataforma não vê saídas
 * porque não vende; o sumário do painel dela continua sendo a **F10**.
 */
@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    // REVITALIZAÇÃO: voltam com as seções Passagem / Equipe.
    // private val passagemRepository: PassagemFirestoreRepository,
    // private val funcionarioRepository: FuncionarioRepository,
    private val viagemRepository: ViagemRepository,
    private val rotaRepository: RotaRepository,
    private val embarcacaoRepository: EmbarcacaoRepository,
    private val portoRepository: PortoRepository,
    private val localidadeRepository: LocalidadeRepository,
    private val escopoDaSessao: EscopoDaSessao,
    private val relogio: Relogio,
    private val firebaseAuth: FirebaseAuth,
    private val sincronizacaoSessao: SincronizacaoSessao,
    private val estadoSincronizacao: EstadoSincronizacao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainScreenUiState())
    val uiState: StateFlow<MainScreenUiState>
        get() = _uiState.asStateFlow()

    init {
        obterUsuario()
        observarSincronizacao()
        carregarInicio()
    }

    /**
     * O Início, decidido pelo domínio e **assinado**, não lido uma vez.
     *
     * Até 2026-08-17 este método fazia cinco leituras e copiava o resultado para o estado. A tela ficava com
     * o snapshot do instante em que nasceu: uma viagem inativada pelo painel continuava no card até o app ser
     * reaberto, porque este ViewModel vive enquanto a home está na pilha de navegação. O que mudou é só o
     * regime — a montagem foi para [fluxoDoInicio], que assina os `StateFlow` das cinco coleções.
     *
     * As leituras seguem sendo de coleções pequenas com junção em memória — mesma escolha do "Porto X —
     * Belém/PA", e a única possível num pool sem `empresaId`. A alternativa (uma consulta por linha) não
     * existe no Firestore, e um índice denormalizado seria uma segunda verdade sobre a concessão.
     */
    private fun carregarInicio() {
        viewModelScope.launch {
            fluxoDoInicio(
                escopo = escopoDaSessao.atual(),
                viagemRepository = viagemRepository,
                rotaRepository = rotaRepository,
                portoRepository = portoRepository,
                localidadeRepository = localidadeRepository,
                embarcacaoRepository = embarcacaoRepository,
                relogio = relogio,
            ).collect { inicio ->
                _uiState.update { it.copy(inicio = inicio) }
            }
        }
    }

    // D4: reflete a saúde do sync (EstadoSincronizacao) num flag de UI — o banner offline-first é
    // não-bloqueante (os cards do cache continuam). Limpa quando um snapshot do servidor chega.
    private fun observarSincronizacao() {
        viewModelScope.launch {
            estadoSincronizacao.comErro.collect { comErro ->
                _uiState.update { it.copy(sincronizacaoComErro = comErro) }
            }
        }
    }

    private fun obterUsuario() {
        viewModelScope.launch {
            dataStore.data.collect { prefs ->
                val username = prefs[PreferencesKey.USUARIO_ATUAL]
                // O menu é quase todo eixo de SISTEMA, mas a seção Equipe também olha o cargo: ela
                // existe para o supervisor gerir a própria agência (ADR-0015 §2.2/§8.2).
                val papel = prefs[PreferencesKey.PAPEL_ATUAL]
                val cargo = prefs[PreferencesKey.CARGO_ATUAL]
                // A **família** do menu deriva da atuação (ADR-0016 §2, ADR-0020 F3/F4), e a atuação vem
                // do cargo, que já a declara. Papel puro de plataforma não tem cargo — e aí `null` é a
                // informação, não a falta dela: quem administra a plataforma não atua num segmento.
                val atuacao = Funcionario.Cargo.de(cargo)?.atuacao
                _uiState.update { state ->
                    state.copy(
                        userName = username ?: state.userName,
                        // `secoesDoMenu` = a política (quem pode) ∩ o escopo revitalizado (o que existe).
                        // A política em si continua intacta — o recorte é do andaime, não da autorização.
                        secoesVisiveis = secoesDoMenu(papel, cargo, atuacao),
                    )
                }
            }
        }
    }

    fun irParaHome() {
        _uiState.update { it.copy(mainScreenState = MainScreenState.HOME) }
    }

    suspend fun deslogar() {
        // para os listeners de sync da sessão (D2 — awaitClose remove as registrations), encerra a
        // sessão do Firebase (autoridade) + limpa o cache de perfil no DataStore.
        sincronizacaoSessao.parar()
        firebaseAuth.signOut()
        dataStore.edit {
            it[PreferencesKey.LOGADO] = false
            it[PreferencesKey.USUARIO_ATUAL] = ""
            it[PreferencesKey.CARGO_ATUAL] = ""
            // A escolha de contexto (F6.4) é de quem estava operando, não do aparelho: quem entrar
            // depois responde de novo. Deixá-la para trás faria a próxima pessoa herdar a empresa da
            // anterior — e o app não perguntaria, porque já teria uma resposta guardada.
            it.remove(PreferencesKey.EMPRESA_ATIVA)
        }
    }

    // REVITALIZAÇÃO: o pull-to-refresh saiu da tela com a lista antiga (ADR-0020) e não voltou com a nova.
    // Ele forçava `Source.SERVER` para reencher o Room, e nem o Room nem aquele repositório existem — o
    // que reabastece a lista de hoje é o listener do ADR-0017. Reintroduzi-lo é decisão da F10.

    // REVITALIZAÇÃO: sincronizar coleção que nenhuma tela viva consome é pagar listener por nada. A
    // Empresa tem a própria sincronização, disparada por quem a usa (EmpresaFirestoreRepository).
    //
    // private fun sincronizarFirestore() {
    //     passagemRepository.sincronizarNumeroBilheteEmTempoReal()
    //     funcionarioRepository.sincronizar()
    // }
}
