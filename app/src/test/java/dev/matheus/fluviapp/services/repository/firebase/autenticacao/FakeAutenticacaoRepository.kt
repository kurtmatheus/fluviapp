package dev.matheus.fluviapp.services.repository.firebase.autenticacao

/** Fake da porta de autenticação para testes de VM (sem rede/Firebase). */
class FakeAutenticacaoRepository : AutenticacaoRepository {

    var resultado: ResultadoAutenticacao = ResultadoAutenticacao.Sucesso(emailVerificado = false)

    var perfilCriado: Triple<String, String, String>? = null
        private set
    var saiuVezes = 0
        private set

    /** Perfil devolvido por [perfilAutenticado]; ajuste nos testes do fluxo Google. */
    var perfil: PerfilAutenticado? = null

    override suspend fun autenticar(email: String, senha: String) = resultado

    override suspend fun cadastrar(email: String, senha: String) = resultado

    override suspend fun reenviarVerificacao(email: String, senha: String) = resultado

    override suspend fun recuperarSenha(email: String) = resultado

    override suspend fun autenticarComGoogle(idToken: String) = resultado

    override suspend fun perfilAutenticado() = perfil

    override suspend fun criarPerfil(email: String, nome: String, cargo: String) {
        perfilCriado = Triple(email, nome, cargo)
    }

    override fun sair() {
        saiuVezes++
    }
}