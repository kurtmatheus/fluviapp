package dev.matheus.fluviapp.services.repository.firebase.autenticacao

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Borda do Credential Manager (UI / Play Services): abre o seletor de contas do sistema e
 * devolve o **ID token** do Google. Fica FORA da porta [AutenticacaoRepository] porque precisa de
 * um Context de Activity para renderizar a UI — o ViewModel recebe apenas o idToken (String) e
 * permanece testável com o fake da porta.
 *
 * `serverClientId` = `R.string.default_web_client_id` (gerado pelo plugin google-services a partir
 * do `oauth_client` type 3 no google-services.json).
 */
object GoogleCredentialProvider {

    suspend fun obterIdToken(context: Context, serverClientId: String): String {
        val opcao = GetGoogleIdOption.Builder()
            .setServerClientId(serverClientId)
            .setFilterByAuthorizedAccounts(false) // permite 1º login (conta ainda não autorizada)
            .setAutoSelectEnabled(false)
            .build()

        val requisicao = GetCredentialRequest.Builder()
            .addCredentialOption(opcao)
            .build()

        val resposta = CredentialManager.create(context).getCredential(context, requisicao)
        val credencial = resposta.credential

        if (credencial is CustomCredential &&
            credencial.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return GoogleIdTokenCredential.createFrom(credencial.data).idToken
        }
        error("Credencial inesperada do Credential Manager: ${credencial.type}")
    }
}