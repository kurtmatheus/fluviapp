package dev.matheus.fluviapp.services.repository.firebase.autenticacao

/** Fake da porta de autenticação para testes de VM (sem rede/Firebase). */
class FakeAutenticacaoRepository : AutenticacaoRepository {

    var resultado: ResultadoAutenticacao = ResultadoAutenticacao.Sucesso(emailVerificado = false)

    var perfilCriado: Triple<String, String, String>? = null
        private set
    var saiuVezes = 0
        private set

    override suspend fun autenticar(email: String, senha: String) = resultado

    override suspend fun cadastrar(email: String, senha: String) = resultado

    override suspend fun reenviarVerificacao(email: String, senha: String) = resultado

    override suspend fun criarPerfil(email: String, nome: String, cargo: String) {
        perfilCriado = Triple(email, nome, cargo)
    }

    override fun sair() {
        saiuVezes++
    }
}