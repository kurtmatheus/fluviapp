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

    suspend fun criarPerfil(email: String, nome: String, cargo: String)

    fun sair()
}