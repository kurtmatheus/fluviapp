package dev.matheus.fluviapp.services.repository.operacoes

import dev.matheus.fluviapp.model.operacoes.ContextoUsuario
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Porta fina (DIP) que responde **quem está operando** — os dois contextos resolvidos de uma vez
 * (ADR-0015 §8.1). Os ViewModels dependem desta interface, não do par de repositórios: assim o recorte
 * por papel/cargo/agência é testável com um fake, e o caminho `usuário → funcionarioId → funcionário`
 * existe em **um** lugar em vez de repetido em cada tela.
 */
interface SessaoUsuario {
    /** `null` se não há usuário logado. O `funcionario` dentro dele é que pode faltar (papel puro). */
    suspend fun atual(): ContextoUsuario?
}

/** Impl sobre o espelho Room (ADR-0003): as duas leituras são locais, não vão à rede. */
@Singleton
class SessaoUsuarioRoom @Inject constructor(
    private val usuarioRepository: UsuarioRepository,
    private val funcionarioRepository: FuncionarioRepository,
) : SessaoUsuario {

    override suspend fun atual(): ContextoUsuario? {
        val usuario = usuarioRepository.obterUltimoUsuarioLogado() ?: return null
        val funcionario = usuario.funcionarioId
            .takeIf { it.isNotBlank() }
            ?.let { funcionarioRepository.obterPorId(it) }
        return ContextoUsuario(usuario, funcionario)
    }
}