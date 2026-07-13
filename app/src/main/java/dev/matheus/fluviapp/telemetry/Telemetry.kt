package dev.matheus.fluviapp.telemetry

/**
 * Porta de observabilidade (ADR-0004/ADR-0007). Genérica: as primitivas são agnósticas de
 * domínio — emissão e cadastro reusam a mesma porta; a *semântica* (o que cada desfecho
 * significa) fica em camadas por domínio ([RegistroEmissao], [RegistroCadastro]).
 *
 * DIP: quem observa não conhece o Firebase — só esta interface. Impl real:
 * [FirebaseTelemetry]; nos testes, um fake.
 *
 * 4 pilares SRE (padrão importado do autorizacao-servico-app): erros ([naoFatal]),
 * eventos ([evento]), trilha/breadcrumb ([rastro]) e — futuramente — latência (trace).
 */
interface Telemetry {

    /** Evento navegável (Analytics). Ex.: sucesso/warning com dimensões em [params]. */
    fun evento(nome: String, params: Map<String, String> = emptyMap())

    /** Breadcrumb — trilha que acompanha um eventual fatal. */
    fun rastro(mensagem: String)

    /** Erro não-fatal: registrado sem derrubar o app (Crashlytics quando disponível). */
    fun naoFatal(erro: Throwable, chaves: Map<String, String> = emptyMap())
}
