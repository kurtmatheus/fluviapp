package dev.matheus.fluviapp.services.repository.operacoes

import dev.matheus.fluviapp.domain.operacoes.ContextoUsuario
import dev.matheus.fluviapp.preferences.EscolhaDeVinculo
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Porta fina (DIP) que responde **quem está operando** — os dois contextos resolvidos de uma vez
 * (ADR-0015 §8.1). Os ViewModels dependem desta interface, não do par de repositórios: assim o recorte
 * por papel/cargo/empresa é testável com um fake, e o caminho `usuário → funcionarioId → funcionário`
 * existe em **um** lugar em vez de repetido em cada tela.
 *
 * Desde a F6.4 ela responde uma terceira coisa: **em nome de quem** a pessoa opera, quando há mais de um
 * vínculo (ADR-0016 §6).
 */
interface SessaoUsuario {
    /** `null` se não há usuário logado. O `funcionario` dentro dele é que pode faltar (papel puro). */
    suspend fun atual(): ContextoUsuario?

    /**
     * Guarda a empresa escolhida — a resposta à seleção de contexto.
     *
     * A porta **não valida** a escolha: quem decide se ela vale é o domínio, a cada leitura
     * (`ContextoUsuario.vinculoAtivo`). É o que faz um vínculo perdido invalidar a escolha sozinho, sem
     * depender de alguém lembrar de limpá-la.
     */
    suspend fun escolherEmpresa(empresaId: String)

    /** Esquece a escolha — logout, ou troca deliberada de contexto. */
    suspend fun limparEscolha()
}

/**
 * Impl sobre os repositórios locais + a escolha persistida ([EscolhaDeVinculo]).
 *
 * O nome `…Room` ficou do tempo em que as duas leituras vinham do espelho; hoje o funcionário vem do
 * Firestore (F6.2) e o usuário ainda é local. O que a classe faz continua sendo o mesmo: juntar as três
 * peças num contexto só.
 */
@Singleton
class SessaoUsuarioRoom @Inject constructor(
    private val usuarioRepository: UsuarioRepository,
    private val funcionarioRepository: FuncionarioRepository,
    private val empresaRepository: EmpresaRepository,
    private val escolhaDeVinculo: EscolhaDeVinculo,
) : SessaoUsuario {

    override suspend fun atual(): ContextoUsuario? {
        val usuario = usuarioRepository.obterUltimoUsuarioLogado() ?: return null
        val funcionario = usuario.funcionarioId
            .takeIf { it.isNotBlank() }
            ?.let { funcionarioRepository.obterPorId(it) }

        val contexto = ContextoUsuario(
            usuario = usuario,
            funcionario = funcionario,
            empresaAtivaId = escolhaDeVinculo.empresaEscolhida(),
        )

        // O nome da empresa é resolvido **aqui**, e só quando há vínculo em vigor: é o bilhete que
        // precisa dele (gente lê nome, não id), e uma leitura a mais por sessão é mais barata do que
        // cada tela que imprime algo repetir a consulta.
        val empresaAtiva = contexto.vinculoAtivo?.let { empresaRepository.obterPorId(it.empresaId) }
        return contexto.copy(empresaAtivaNome = empresaAtiva?.nome.orEmpty())
    }

    override suspend fun escolherEmpresa(empresaId: String) = escolhaDeVinculo.guardar(empresaId)

    override suspend fun limparEscolha() = escolhaDeVinculo.limpar()
}