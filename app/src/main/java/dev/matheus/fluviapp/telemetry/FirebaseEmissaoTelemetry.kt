package dev.matheus.fluviapp.telemetry

import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Impl real da [EmissaoTelemetry] (ADR-0004). Único ponto que toca o Firebase.
 *
 * Espelho local↔remoto: além de mandar pro Firebase, escreve no logcat (tag [TAG]) — sucesso e
 * trilha ficam visíveis localmente E remoto numa só chamada.
 *
 * Estado atual: eventos vão pro Analytics (já é dependência). Crashlytics (non-fatal + log
 * remoto) fica marcado como TODO — depende do projeto Firebase recriado (camada 4 do rename;
 * hoje o google-services.json ainda é o projeto da empresa). Por ora [naoFatal]/[rastro]
 * espelham no logcat; a troca é trivial quando o projeto novo entrar.
 */
class FirebaseEmissaoTelemetry(
    private val analytics: FirebaseAnalytics,
) : EmissaoTelemetry {

    override fun evento(nome: String, params: Map<String, String>) {
        Log.i(TAG, "evento=$nome $params")
        analytics.logEvent(nome, Bundle().apply { params.forEach { (k, v) -> putString(k, v) } })
    }

    override fun rastro(mensagem: String) {
        Log.d(TAG, mensagem)
        // TODO(firebase-novo): Firebase.crashlytics.log(mensagem)
    }

    override fun naoFatal(erro: Throwable, chaves: Map<String, String>) {
        Log.e(TAG, "naoFatal=${erro.message} $chaves", erro)
        // TODO(firebase-novo): chaves.forEach { (k, v) -> crashlytics.setCustomKey(k, v) }
        //                      Firebase.crashlytics.recordException(erro)
    }

    companion object {
        private const val TAG = "EmissaoTelemetry"
    }
}