package dev.matheus.fluviapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.matheus.fluviapp.domain.operacoes.Perfil
import dev.matheus.fluviapp.domain.operacoes.PermissoesUsuario
import dev.matheus.fluviapp.domain.screendata.secoesDoMenu
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
import dev.matheus.fluviapp.services.repository.firebase.autenticacao.AutenticacaoRepository
import dev.matheus.fluviapp.services.repository.operacoes.EscopoDaSessao
import dev.matheus.fluviapp.services.repository.operacoes.SessaoUsuario
import dev.matheus.fluviapp.ui.states.MainScreenState
import dev.matheus.fluviapp.ui.states.MainScreenUiState
import dev.matheus.fluviapp.util.Relogio
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    private val autenticacaoRepository: AutenticacaoRepository,
    private val sessaoUsuario: SessaoUsuario,
    private val sincronizacaoSessao: SincronizacaoSessao,
    private val estadoSincronizacao: EstadoSincronizacao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainScreenUiState())
    val uiState: StateFlow<MainScreenUiState>
        get() = _uiState.asStateFlow()

    /**
     * O Início é assinado a partir de um **escopo lido uma vez**, então trocar de perfil pede reiniciar a
     * assinatura: o guardado aqui é o que se cancela ([trocarPerfil]).
     *
     * O menu não precisa disso — ele coleta o contexto, e o contexto reemite sozinho. A diferença é de
     * regime, não de cuidado: o escopo é uma foto (`escopoDaSessao.atual()`), e foto não reage.
     *
     * **Declarado antes do `init`** de propósito: inicializador de propriedade e bloco `init` rodam na
     * ordem em que aparecem, e daqui de baixo o `= null` apagaria o job que o `init` acabou de criar.
     */
    private var assinaturaDoInicio: Job? = null

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
        assinaturaDoInicio?.cancel()
        assinaturaDoInicio = viewModelScope.launch {
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

    /**
     * O menu, assinado ao **contexto** e não às chaves ([ADR-0032] D2).
     *
     * Até 2026-09-07 este método lia `USUARIO_ATUAL`/`PAPEL_ATUAL`/`CARGO_ATUAL` direto do DataStore — e
     * **não era desleixo**: ele `collect`ava, então reagia, enquanto a porta só sabia responder uma vez.
     * Com `SessaoUsuario.observar()`, a razão acabou, e três coisas somem juntas: a leitura crua, as duas
     * chaves que existiam só para alimentá-la, e a derivação da atuação repetida aqui.
     *
     * O nome exibido passa a vir de `ContextoUsuario.nomeExibicao`, que **resolve melhor** do que a chave
     * resolvia: a chave congelava o nome do login, e o contexto lê o funcionário de agora.
     *
     * Contexto nulo (ninguém logado) zera o menu em vez de manter o anterior — fail-closed, e é o estado
     * de quem acabou de sair.
     */
    private fun obterUsuario() {
        viewModelScope.launch {
            sessaoUsuario.observar().collect { contexto ->
                // O menu é quase todo eixo de SISTEMA, mas a seção Equipe também olha o cargo: ela
                // existe para o supervisor gerir a própria agência (ADR-0015 §2.2/§8.2).
                val papel = contexto?.papel
                val cargo = contexto?.cargo
                // A **família** do menu deriva da atuação (ADR-0016 §2, ADR-0020 F3/F4), e quem a declara
                // é o contexto — a derivação `Cargo.de(cargo)?.atuacao` morava aqui **e** no
                // `ContextoUsuario`, duas cópias do mesmo fato. Papel puro de plataforma não tem cargo, e
                // aí `null` é a informação, não a falta dela.
                val atuacao = contexto?.atuacao
                _uiState.update { state ->
                    state.copy(
                        userName = contexto?.nomeExibicao.orEmpty(),
                        // `secoesDoMenu` = a política (quem pode) ∩ o escopo revitalizado (o que existe).
                        // A política em si continua intacta — o recorte é do andaime, não da autorização.
                        secoesVisiveis = secoesDoMenu(papel, cargo, atuacao),
                        // O embarque é gesto, não seção: não passa pelo andaime porque não tem seção
                        // para revitalizar — a tela e a escrita já existem e estão testadas.
                        podeEmbarcar = PermissoesUsuario.temEntradaDeEmbarque(papel, cargo, atuacao),
                        // A troca de perfil ([ADR-0032] D5). O painel inteiro acima já saiu daqui trocado,
                        // porque `cargo` e `atuacao` vêm do vínculo **em vigor** — a lente mora no
                        // contexto, e este ViewModel não a conhece.
                        podeTrocarPerfil = contexto?.podeTrocarPerfil == true,
                        perfilAtivo = contexto?.perfilAtivo ?: Perfil.PLATAFORMA,
                        empresaDoVinculo = contexto?.empresaDoVinculo.orEmpty(),
                        // Volta do "carregando" da troca: quem sabe que o contexto novo chegou é quem o
                        // recebe.
                        mainScreenState = MainScreenState.HOME,
                    )
                }
            }
        }
    }

    fun irParaHome() {
        _uiState.update { it.copy(mainScreenState = MainScreenState.HOME) }
    }

    /**
     * **Trocar de perfil** ([ADR-0032] D5) — a opção do menu de quem tem os dois.
     *
     * Três coisas, nesta ordem, e cada uma por um motivo:
     *
     *  1. **o guarda**, porque a política se pergunta antes do gesto (D1) e não só antes do menu. Sem ele,
     *     o gesto existiria para quem a opção nem oferece;
     *  2. **o `LOADING`**, que é a resposta da Q3: *"Carregando sessão e informações do perfil."* com
     *     indicador circular, e nada além. Celebrar a troca sugeriria uma separação que o servidor não faz.
     *     O estado já existia na tela e **não tinha quem o produzisse** — a troca é o produtor que faltava;
     *  3. **a assinatura do Início reiniciada**, porque o escopo do pool é lido uma vez. O menu se corrige
     *     sozinho (o contexto é fluxo); o Início não, e um painel de empresa com as saídas da lente
     *     anterior seria pior do que um painel vazio.
     */
    fun trocarPerfil() {
        val estado = _uiState.value
        if (!estado.podeTrocarPerfil) return

        val destino = if (estado.perfilAtivo == Perfil.EMPRESA) Perfil.PLATAFORMA else Perfil.EMPRESA
        _uiState.update { it.copy(mainScreenState = MainScreenState.LOADING) }
        viewModelScope.launch {
            sessaoUsuario.trocarPerfil(destino)
            carregarInicio()
        }
    }

    /**
     * Sair — **três gestos, três portas** ([ADR-0032] D2).
     *
     * Antes eram cinco linhas e duas delas conheciam o DataStore por dentro: o ViewModel sabia quais
     * chaves existiam e apagava uma a uma. Um ViewModel assim precisa ser corrigido toda vez que uma
     * chave nasce — e foi exatamente o que aconteceu quando a projeção do usuário mudou de casa.
     *
     * O `firebaseAuth.signOut()` também sai: `autenticacaoRepository.sair()` faz o mesmo, é o caminho que
     * o login e o primeiro acesso já usam, e era **este** o único ponto do app que injetava `FirebaseAuth`
     * num ViewModel sem precisar.
     *
     * A escolha de contexto (F6.4) sai junto, dentro do `encerrar()`: ela é de quem estava operando, não
     * do aparelho — deixá-la para trás faria a próxima pessoa herdar a empresa da anterior, e o app não
     * perguntaria, porque já teria uma resposta guardada.
     */
    suspend fun deslogar() {
        // Para os listeners da sessão (D2 — `awaitClose` remove as registrations).
        sincronizacaoSessao.parar()
        // A autoridade da credencial.
        autenticacaoRepository.sair()
        // A projeção local: quem entrou e o que escolheu. O e-mail fica, porque é conveniência do
        // próximo login e não sessão.
        sessaoUsuario.encerrar()
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
