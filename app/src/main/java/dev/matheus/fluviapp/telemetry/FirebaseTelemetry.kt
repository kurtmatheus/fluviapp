package dev.matheus.fluviapp.telemetry

import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Impl real da [Telemetry] (ADR-0004/ADR-0007). Único ponto que toca o Firebase.
 *
 * Espelho local↔remoto: além de mandar pro Firebase, escreve no logcat (tag [TAG]) — sucesso e
 * trilha ficam visíveis localmente E remoto numa só chamada.
 *
 * 4 pilares SRE ligados no projeto limpo `fluvi-app-dev`:
 * - [evento]  -> Analytics (DebugView em tempo real).
 * - [rastro]  -> Crashlytics.log (breadcrumb remoto que acompanha o próximo fatal).
 * - [naoFatal] -> custom keys + recordException (não-fatal navegável no Crashlytics).
 * Requer o plugin Gradle `com.google.firebase.crashlytics` (injeta o build ID que o SDK lê no
 * init; sem ele, FirebaseCrashlytics.init lança IllegalStateException em runtime).
 */
class FirebaseTelemetry(
    private val analytics: FirebaseAnalytics,
    private val crashlytics: FirebaseCrashlytics,
) : Telemetry {

    override fun evento(nome: String, params: Map<String, String>) {
        Log.i(TAG, "evento=$nome $params")
        analytics.logEvent(nome, Bundle().apply { params.forEach { (k, v) -> putString(k, v) } })
    }

    override fun rastro(mensagem: String) {
        Log.d(TAG, mensagem)
        crashlytics.log(mensagem)
    }

    override fun naoFatal(erro: Throwable, chaves: Map<String, String>) {
        Log.e(TAG, "naoFatal=${erro.message} $chaves", erro)
        chaves.forEach { (k, v) -> crashlytics.setCustomKey(k, v) }
        crashlytics.recordException(erro)
    }

    companion object {
        private const val TAG = "Telemetry"
    }
}
