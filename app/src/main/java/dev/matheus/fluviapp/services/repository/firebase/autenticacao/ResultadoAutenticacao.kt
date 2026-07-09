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
    DESCONHECIDO,
}