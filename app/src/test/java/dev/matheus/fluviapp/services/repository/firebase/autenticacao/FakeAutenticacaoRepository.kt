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

    /**
     * O que [perfilAutenticado] devolve. O default é [ResultadoPerfil.Ausente] — o sinal do primeiro
     * acesso —, e [ResultadoPerfil.Indisponivel] permite exercitar o que antes não tinha como ser
     * exercitado: o login sem conseguir falar com o servidor.
     */
    var resultadoPerfil: ResultadoPerfil = ResultadoPerfil.Ausente

    /** Atalho: define o perfil encontrado sem montar o [ResultadoPerfil] à mão. */
    var perfil: PerfilAutenticado?
        get() = (resultadoPerfil as? ResultadoPerfil.Encontrado)?.perfil
        set(valor) {
            resultadoPerfil = valor?.let { ResultadoPerfil.Encontrado(it) } ?: ResultadoPerfil.Ausente
        }

    /** Faz [criarPerfil] falhar, para cobrir o meio-caminho (senha trocada, perfil não criado). */
    var falharAoCriarPerfil = false

    override suspend fun autenticar(email: String, senha: String) = resultado

    override suspend fun recuperarSenha(email: String) = resultado

    override suspend fun alterarSenha(novaSenha: String): ResultadoAutenticacao {
        val r = resultadoAlterarSenha ?: resultado
        if (r is ResultadoAutenticacao.Sucesso) senhaAlterada = novaSenha
        return r
    }

    override suspend fun perfilAutenticado() = resultadoPerfil

    override suspend fun criarPerfil(email: String, username: String, papel: String, funcionarioId: String) {
        if (falharAoCriarPerfil) throw RuntimeException("falha simulada ao criar perfil")
        perfilCriado = listOf(email, username, papel, funcionarioId)
    }

    override fun sair() {
        saiuVezes++
    }
}