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

    /**
     * **As coordenadas de quem opera** ([ADR-0032] D4) — acompanham todo evento e todo não-fatal daqui em
     * diante, sem que o ponto de emissão precise saber delas.
     *
     * São **agregáveis, nunca identidade**: agência, papel e cargo respondem *"quantas emissões por
     * agência nesta semana"*, e não *"o que a Ana fez às 14h"*. A segunda pergunta é do carimbo dentro do
     * documento (`MetadadosPassagem.funcionarioId`, `CarimboEmbarque.porId`), onde a auditoria mora —
     * **o evento conta, o documento prova**.
     *
     * ### Por que a sessão empurra, em vez de a telemetria puxar
     *
     * Porque puxar seria um ciclo, e não uma preferência: `Telemetry` é dependência de
     * `RegistroCadastro`, que é de `ColecaoFirestore`, que é do `FuncionarioRepository`, que é da
     * `SessaoUsuario`. Uma telemetria que injetasse a sessão fecharia o laço, e o Dagger recusaria o
     * grafo. Quem sabe quem está operando é quem avisa.
     */
    fun definirCoordenadas(coordenadas: Map<String, String>)
}
