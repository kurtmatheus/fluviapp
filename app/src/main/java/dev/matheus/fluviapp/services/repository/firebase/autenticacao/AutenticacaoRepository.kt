package dev.matheus.fluviapp.services.repository.firebase.autenticacao

/**
 * Porta de autenticação (DIP): o ViewModel depende desta interface, não do Firebase `Task`.
 * As operações de rede são `suspend` e devolvem [ResultadoAutenticacao] (domínio). Testes usam
 * um fake; produção usa a impl Firebase.
 *
 * **O que saiu em P2.2c** (ADR-0015 §2.1): `cadastrar`, `reenviarVerificacao` e `autenticarComGoogle`.
 * Não havia como o app ganhar conta por conta própria e ao mesmo tempo dizer que só entra quem foi
 * pré-cadastrado — as três eram portas do mesmo provisionamento automático. O que entrou no lugar é
 * [alterarSenha], que serve o primeiro acesso: a conta já existe, o que muda é a senha.
 */
interface AutenticacaoRepository {

    suspend fun autenticar(email: String, senha: String): ResultadoAutenticacao

    /** Envia o e-mail de redefinição (link built-in do Firebase). `Sucesso` = enviado. */
    suspend fun recuperarSenha(email: String): ResultadoAutenticacao

    /**
     * Troca a senha do usuário **já autenticado** (primeiro acesso, ADR-0015 §2.1). Não é criação de
     * conta: a conta nasceu no pré-cadastro, com a senha padrão.
     */
    suspend fun alterarSenha(novaSenha: String): ResultadoAutenticacao

    /**
     * Perfil autoritativo do usuário autenticado (`users/{uid}` + o `funcionarios/{id}` ligado).
     *
     * **Lido do servidor, nunca do cache** ([ResultadoPerfil.Indisponivel] quando não dá): é este perfil
     * que decide papel e cargo, e responder isso com o que sobrou da última sessão é decidir permissão
     * por memória. O ADR-0005 já dizia que o primeiro login exige rede; aqui isso deixa de ser aviso e
     * passa a ser tipo.
     */
    suspend fun perfilAutenticado(): ResultadoPerfil

    /** Nasce o `users/{uid}` do autenticado, já vinculado ao funcionário que o pré-cadastro criou. */
    suspend fun criarPerfil(email: String, username: String, papel: String, funcionarioId: String)

    fun sair()
}