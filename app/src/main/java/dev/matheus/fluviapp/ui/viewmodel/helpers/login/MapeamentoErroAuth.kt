package dev.matheus.fluviapp.ui.viewmodel.helpers.login

import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import dev.matheus.fluviapp.R

/**
 * Mapeia a exceção de autenticação do Firebase para a mensagem de erro (res id). Decisão pura
 * (JVM-testável); o ramo `else` é o único garantidamente construível fora da rede — os tipos
 * específicos do Firebase são da borda de rede (motivação para o refactor de porta, item 2).
 */
internal fun mapearMensagemErroAuth(erro: Throwable): Int = when (erro) {
    is FirebaseAuthInvalidCredentialsException -> R.string.error_usuario_incorreto
    is FirebaseAuthInvalidUserException -> R.string.error_usuario_inexistente
    else -> R.string.error_falha_auth
}