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
 */
data class PerfilAutenticado(
    val id: String,
    val email: String,
    val nome: String,
    val cargo: String,
    /** Capacidades organizacionais do membro (ADR-0015 §2) — viajam junto p/ semear a sessão. */
    val agencia: String = "",
    val lotacao: String = "",
)