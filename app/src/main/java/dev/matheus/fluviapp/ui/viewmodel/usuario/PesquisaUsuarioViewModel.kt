package dev.matheus.fluviapp.ui.viewmodel.usuario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.matheus.fluviapp.domain.operacoes.Convite
import dev.matheus.fluviapp.domain.operacoes.PermissoesUsuario
import dev.matheus.fluviapp.domain.operacoes.SituacaoAcesso
import dev.matheus.fluviapp.domain.operacoes.Usuario
import dev.matheus.fluviapp.domain.operacoes.situacaoDoAcesso
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaRepository
import dev.matheus.fluviapp.services.repository.operacoes.ConviteRepository
import dev.matheus.fluviapp.services.repository.operacoes.SessaoUsuario
import dev.matheus.fluviapp.services.repository.operacoes.UsuarioRepository
import dev.matheus.fluviapp.telemetry.RegistroCadastro
import dev.matheus.fluviapp.ui.states.PesquisaUsuarioUiState
import dev.matheus.fluviapp.ui.states.UsuarioResultado
import dev.matheus.fluviapp.ui.viewmodel.helpers.usuario.prazoEmMillis
import dev.matheus.fluviapp.ui.viewmodel.helpers.usuario.prazoFormatado
import dev.matheus.fluviapp.util.Relogio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A seção Usuários — e desde a [ADR-0032] D6 ela **escreve**, o que supera o ADR-0021 D2.
 *
 * ### Duas coleções, uma lista
 *
 * Ela listava **convites**, e só. Fazia sentido enquanto o acesso não tinha estado: o convite dizia quem
 * podia entrar e com que papel, e *usado* era tudo o que havia a dizer sobre o depois. Com `ativo` e
 * `expiraEm` em `users/{uid}` (Q1), aquele *Ativo* virou mentira — um convite usado por alguém depois
 * desativado continuaria dizendo que está ativo.
 *
 * Então a lista **junta as duas**, por e-mail, e nenhuma das duas manda sozinha:
 *
 *  - quem tem convite e não tem perfil é **Convidado** — o primeiro acesso não aconteceu;
 *  - quem tem perfil traz a **situação** dele (ativo · desativado · expirado);
 *  - quem tem perfil e **não** tem convite aparece igual, e este caso não é exótico: é o `ADM` de
 *     bootstrap (ADR-0021 D0), que entrou pelo console. Deixá-lo fora da lista seria esconder da gestão de
 *     acesso justamente a conta mais poderosa.
 *
 * O nome e o vínculo continuam vindo do convite: `users/{uid}` não tem nome (§8.1), e o vínculo em vigor é
 * do funcionário — mostrá-lo aqui é conveniência de leitura, não fonte.
 *
 * ### O que ela **não** faz, e continua sendo decisão
 *
 * Não muda papel de ninguém: o `papel` fica fora da lista fechada de chaves que a regra abre ao `ADM`
 * (Q1), inclusive para ele. Não há convite de `ADM` (D6), logo também não há promoção a `ADM` — trocar
 * papel segue sendo ato de console.
 */
@HiltViewModel
class PesquisaUsuarioViewModel @Inject constructor(
    private val conviteRepository: ConviteRepository,
    private val usuarioRepository: UsuarioRepository,
    private val empresaRepository: EmpresaRepository,
    private val sessaoUsuario: SessaoUsuario,
    private val registroCadastro: RegistroCadastro,
    private val relogio: Relogio,
) : ViewModel() {

    private var convites: List<Convite> = emptyList()
    private var usuarios: List<Usuario> = emptyList()
    private var empresasPorId: Map<String, String> = emptyMap()

    private val _uiState = MutableStateFlow(PesquisaUsuarioUiState())
    val uiState: StateFlow<PesquisaUsuarioUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // **A política antes do gesto** ([ADR-0032] D1): quem não pode gerir vê a lista e nada mais.
            // A seção já é `ADM`-only no menu, e é justamente por isso que a pergunta se repete aqui — a
            // ausência do botão não é fronteira.
            _uiState.update {
                it.copy(podeGerir = PermissoesUsuario.podeGerirAcesso(sessaoUsuario.atual()?.papel))
            }
            empresasPorId = empresaRepository.obterTodas().associate { it.id to it.nome }
            recarregar()
        }
    }

    fun onEmailChange(email: String) = _uiState.update {
        it.copy(email = email, resultados = filtrar(email))
    }

    /**
     * **Desativar e reativar** — o par, e o par importa: desativar sem reativar transforma um engano em
     * ida ao console, que é o que a D6 existe para tirar do caminho.
     *
     * O prazo **não é tocado** aqui. Ligar de volta quem tinha prazo não é apagar o prazo: são duas
     * decisões, e juntá-las faria a reativação conceder mais do que o gesto diz.
     */
    fun onAlternarAcesso(id: String, ativo: Boolean) = gerir(id) { usuario ->
        usuarioRepository.definirAcesso(id, ativo = ativo, expiraEm = usuario.expiraEm)
    }

    /**
     * O prazo, definido ou tirado — texto em branco é *sem prazo* (ver `prazoEmMillis`).
     *
     * Definir prazo **não religa** quem está desativado, pela mesma razão inversa: quem foi desligado por
     * gesto de alguém volta por gesto de alguém, não porque uma data foi escolhida.
     */
    fun onDefinirPrazo(id: String, texto: String) = gerir(id) { usuario ->
        usuarioRepository.definirAcesso(id, ativo = usuario.ativo, expiraEm = prazoEmMillis(texto))
    }

    /**
     * O caminho comum dos dois gestos: guarda, escreve, recarrega — e a falha deixa rastro.
     *
     * Lê o perfil da lista em memória para completar a metade que o gesto não informa (o `ativo` de quem
     * teve prazo definido, e vice-versa): a escrita é de dois campos juntos, porque são as duas metades da
     * mesma pergunta, e mandar só uma apagaria a outra.
     */
    private fun gerir(id: String, escrita: suspend (Usuario) -> Unit) {
        if (!_uiState.value.podeGerir) return
        val usuario = usuarios.firstOrNull { it.id == id } ?: return

        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            try {
                escrita(usuario)
                recarregar()
            } catch (e: Exception) {
                // A falha não é silenciosa ([ADR-0032] D4): quem desativou precisa saber que não desativou.
                registroCadastro.falhou("usuario", e)
            }
            _uiState.update { it.copy(isProcessing = false) }
        }
    }

    /** Recarrega as duas coleções e reaplica o filtro corrente (entrada e pós-gesto). */
    private suspend fun recarregar() {
        usuarioRepository.sincronizar()
        convites = conviteRepository.obterTodos()
        usuarios = usuarioRepository.obterTodos()
        _uiState.update { it.copy(resultados = filtrar(it.email)) }
    }

    /**
     * A junção, por e-mail em minúsculas — que é o id do convite e o campo do perfil.
     *
     * A ordem é a dos perfis primeiro e os convidados depois: quem tem acesso é o assunto da seção, e quem
     * está no meio do caminho aparece no fim, onde se procura o que ainda falta acontecer.
     */
    private fun filtrar(email: String): List<UsuarioResultado> {
        val procurado = email.trim()
        val convitesPorEmail = convites.associateBy { it.email.lowercase() }
        val emailsComPerfil = usuarios.map { it.email.lowercase() }.toSet()

        val comAcesso = usuarios.map { usuario ->
            resultadoDe(usuario, convitesPorEmail[usuario.email.lowercase()])
        }
        val convidados = convites
            .filterNot { it.email.lowercase() in emailsComPerfil }
            .map(::resultadoDe)

        return (comAcesso + convidados)
            .filter { procurado.isBlank() || it.email.startsWith(procurado, ignoreCase = true) }
    }

    /** Quem entrou: a situação vem do perfil; nome e vínculo, do convite, quando houver. */
    private fun resultadoDe(usuario: Usuario, convite: Convite?) = UsuarioResultado(
        id = usuario.id,
        email = usuario.email.ifBlank { convite?.email.orEmpty() },
        nome = convite?.nome.orEmpty().ifBlank { usuario.username },
        papel = usuario.papel,
        vinculo = vinculoDe(convite),
        situacao = rotuloDa(situacaoDoAcesso(usuario.ativo, usuario.expiraEm, relogio.millis())),
        prazo = prazoFormatado(usuario.expiraEm),
        ativo = usuario.ativo,
        temAcesso = true,
    )

    /** Quem só foi convidado: sem uid, sem situação de acesso — o primeiro acesso ainda não aconteceu. */
    private fun resultadoDe(convite: Convite) = UsuarioResultado(
        id = "",
        email = convite.email,
        nome = convite.nome,
        papel = convite.papel.name,
        vinculo = vinculoDe(convite),
        situacao = SITUACAO_CONVIDADO,
        temAcesso = false,
    )

    private fun vinculoDe(convite: Convite?): String = convite?.vinculo?.let { vinculo ->
        // Empresa que não resolve mostra o id: a linha existe, e esconder o vínculo esconderia justamente
        // o dado a corrigir.
        "${empresasPorId[vinculo.empresaId] ?: vinculo.empresaId} · ${vinculo.cargo.name}"
    }.orEmpty()

    private fun rotuloDa(situacao: SituacaoAcesso): String = when (situacao) {
        SituacaoAcesso.ATIVO -> SITUACAO_ATIVO
        SituacaoAcesso.DESATIVADO -> SITUACAO_DESATIVADO
        SituacaoAcesso.EXPIRADO -> SITUACAO_EXPIRADO
    }

    private companion object {
        const val SITUACAO_ATIVO = "Ativo"
        const val SITUACAO_DESATIVADO = "Desativado"
        const val SITUACAO_EXPIRADO = "Expirado"
        const val SITUACAO_CONVIDADO = "Convidado"
    }
}
