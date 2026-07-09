package dev.matheus.fluviapp.ui.viewmodel.helpers.login

import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.services.repository.firebase.autenticacao.MotivoFalhaAuth

/**
 * Mapeia o motivo de falha de auth (domínio) para a mensagem (res id). Puro e JVM-testável em
 * TODOS os ramos — a tradução da exceção do Firebase para o enum acontece na borda (motivoDe).
 */
internal fun mapearMensagemErroAuth(motivo: MotivoFalhaAuth): Int = when (motivo) {
    MotivoFalhaAuth.CREDENCIAL_INVALIDA -> R.string.error_usuario_incorreto
    MotivoFalhaAuth.USUARIO_INEXISTENTE -> R.string.error_usuario_inexistente
    MotivoFalhaAuth.EMAIL_JA_CADASTRADO -> R.string.error_email_ja_cadastrado
    MotivoFalhaAuth.DESCONHECIDO -> R.string.error_falha_auth
}