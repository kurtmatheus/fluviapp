package dev.matheus.fluviapp.services.repository.firebase.autenticacao

/**
 * Resultado de autenticação em termos de DOMÍNIO (ADR-0005, item 2 / "apartar a regra da rede").
 * A borda (impl Firebase) traduz `Task`/exceções para isto; o ViewModel decide sobre um valor
 * puro — logo gate/reenviar/cadastro/mapeamento ficam JVM-testáveis com fake.
 */
sealed interface ResultadoAutenticacao {
    /**
     * O `emailVerificado` saiu em P2.2c: o gate de verificação era a garantia do **autocadastro** de que
     * a pessoa era dona do e-mail. Com o pré-cadastro (ADR-0015 §2.1) quem responde por isso é a gestão,
     * e a conta criada por ela nasce não-verificada — manter o gate trancaria justamente quem foi
     * cadastrado. A recuperação de senha continua provando posse do e-mail quando ela é necessária.
     */
    data object Sucesso : ResultadoAutenticacao
    data class Falha(val motivo: MotivoFalhaAuth) : ResultadoAutenticacao
}

// EMAIL_JA_CADASTRADO saiu com o autocadastro (P2.2c): colisão de e-mail só acontecia ao CRIAR conta,
// e o app não cria mais nenhuma.
enum class MotivoFalhaAuth {
    CREDENCIAL_INVALIDA,
    USUARIO_INEXISTENTE,
    DESCONHECIDO,
}

/**
 * Perfil autenticado em termos de domínio, lido de `users/{uid}` após o login. Usado para semear
 * a sessão (Room + DataStore) sem depender do listener de `carregarUsuarios` (evita corrida no
 * 1º login Google, quando o perfil acabou de ser criado).
 *
 * Carrega os **dois contextos** resolvidos de uma vez (ADR-0015 §8.3): o de sistema, que vem de
 * `users/{uid}`, e o de negócio ([cargo]/[nome]), que vem de `funcionarios/{funcionarioId}` — o mesmo
 * caminho de dois saltos que as regras do servidor percorrem. A sessão guarda o que **decide acesso**
 * (papel + cargo) e o que **identifica na tela** (nome/username); agência e lotação não vêm — quem
 * precisa delas resolve pelo funcionário, que já está espelhado no Room (§8.1).
 */
data class PerfilAutenticado(
    val id: String,
    val email: String,
    val username: String,
    val papel: String,
    val funcionarioId: String = "",
    /** Do `Funcionario` ligado (§8.3). Vazios quando o papel é puro de plataforma. */
    val cargo: String = "",
    val nome: String = "",
)