package dev.matheus.fluviapp.services.repository.firebase.autenticacao

/** Fake da porta de autenticação para testes de VM (sem rede/Firebase). */
class FakeAutenticacaoRepository : AutenticacaoRepository {

    var resultado: ResultadoAutenticacao = ResultadoAutenticacao.Sucesso

    /** Resultado específico da troca de senha; por padrão segue o [resultado] geral. */
    var resultadoAlterarSenha: ResultadoAutenticacao? = null

    /** Perfil criado no primeiro acesso: (email, username, papel, funcionarioId). */
    var perfilCriado: List<String>? = null
        private set
    var senhaAlterada: String? = null
        private set
    var saiuVezes = 0
        private set

    /** Perfil devolvido por [perfilAutenticado] — `null` é o sinal do primeiro acesso. */
    var perfil: PerfilAutenticado? = null

    /** Faz [criarPerfil] falhar, para cobrir o meio-caminho (senha trocada, perfil não criado). */
    var falharAoCriarPerfil = false

    override suspend fun autenticar(email: String, senha: String) = resultado

    override suspend fun recuperarSenha(email: String) = resultado

    override suspend fun alterarSenha(novaSenha: String): ResultadoAutenticacao {
        val r = resultadoAlterarSenha ?: resultado
        if (r is ResultadoAutenticacao.Sucesso) senhaAlterada = novaSenha
        return r
    }

    override suspend fun perfilAutenticado() = perfil

    override suspend fun criarPerfil(email: String, username: String, papel: String, funcionarioId: String) {
        if (falharAoCriarPerfil) throw RuntimeException("falha simulada ao criar perfil")
        perfilCriado = listOf(email, username, papel, funcionarioId)
    }

    override fun sair() {
        saiuVezes++
    }
}