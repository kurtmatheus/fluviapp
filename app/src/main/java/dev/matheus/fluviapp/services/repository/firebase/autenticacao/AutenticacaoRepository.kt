package dev.matheus.fluviapp.services.repository.firebase.autenticacao

/**
 * Porta de autenticação (DIP): o ViewModel depende desta interface, não do Firebase `Task`.
 * As operações de rede são `suspend` e devolvem [ResultadoAutenticacao] (domínio). Testes usam
 * um fake; produção usa a impl Firebase.
 */
interface AutenticacaoRepository {

    suspend fun autenticar(email: String, senha: String): ResultadoAutenticacao

    suspend fun cadastrar(email: String, senha: String): ResultadoAutenticacao

    suspend fun reenviarVerificacao(email: String, senha: String): ResultadoAutenticacao

    /** Envia o e-mail de redefinição (link built-in do Firebase). `Sucesso` = enviado. */
    suspend fun recuperarSenha(email: String): ResultadoAutenticacao

    /**
     * Autentica com o ID token do Google (obtido via Credential Manager na borda de UI) e
     * auto-provisiona o perfil em `users` no 1º login (só se ausente). Contas Google já vêm com
     * e-mail verificado.
     */
    suspend fun autenticarComGoogle(idToken: String): ResultadoAutenticacao

    /** Perfil autoritativo do usuário autenticado (`users/{uid}`); null se ausente. */
    suspend fun perfilAutenticado(): PerfilAutenticado?

    suspend fun criarPerfil(email: String, nome: String, cargo: String)

    fun sair()
}