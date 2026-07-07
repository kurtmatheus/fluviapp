package dev.matheus.fluviapp.telemetry

/**
 * Porta de observabilidade da emissão de passagem (ADR-0004). DIP: o fluxo não conhece o
 * Firebase — só esta interface. Impl real: [FirebaseEmissaoTelemetry]; nos testes, um fake.
 *
 * 4 pilares SRE (padrão importado do autorizacao-servico-app): erros ([naoFatal]),
 * eventos ([evento]), trilha/breadcrumb ([rastro]) e — futuramente — latência (trace).
 */
interface EmissaoTelemetry {

    /** Evento navegável (Analytics). Ex.: sucesso/warning com dimensões em [params]. */
    fun evento(nome: String, params: Map<String, String> = emptyMap())

    /** Breadcrumb — trilha que acompanha um eventual fatal. */
    fun rastro(mensagem: String)

    /** Erro não-fatal: registrado sem derrubar o app (Crashlytics quando disponível). */
    fun naoFatal(erro: Throwable, chaves: Map<String, String> = emptyMap())
}