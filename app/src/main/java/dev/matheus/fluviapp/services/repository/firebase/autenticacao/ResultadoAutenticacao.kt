package dev.matheus.fluviapp.services.repository.firebase.autenticacao

/**
 * Resultado de autenticação em termos de DOMÍNIO (ADR-0005, item 2 / "apartar a regra da rede").
 * A borda (impl Firebase) traduz `Task`/exceções para isto; o ViewModel decide sobre um valor
 * puro — logo gate/reenviar/cadastro/mapeamento ficam JVM-testáveis com fake.
 */
sealed interface ResultadoAutenticacao {
    data class Sucesso(val emailVerificado: Boolean) : ResultadoAutenticacao
    data class Falha(val motivo: MotivoFalhaAuth) : ResultadoAutenticacao
}

enum class MotivoFalhaAuth {
    CREDENCIAL_INVALIDA,
    USUARIO_INEXISTENTE,
    EMAIL_JA_CADASTRADO,
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