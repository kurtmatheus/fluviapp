package dev.matheus.fluviapp.telemetry

/** Test double que grava as chamadas de telemetria, para asserção nos testes. */
class FakeTelemetry : Telemetry {

    data class EventoRegistrado(val nome: String, val params: Map<String, String>)

    val eventos = mutableListOf<EventoRegistrado>()
    val rastros = mutableListOf<String>()
    val naoFatais = mutableListOf<Throwable>()

    /**
     * As coordenadas de quem opera ([ADR-0032] D4), como a impl real as guarda.
     *
     * O fake as **aplica** ao evento em vez de só registrá-las: é assim que a `FirebaseTelemetry` se
     * comporta, e um fake que não fizesse isso deixaria passar um teste sobre um comportamento que a
     * produção não tem.
     */
    var coordenadas: Map<String, String> = emptyMap()
        private set

    override fun definirCoordenadas(coordenadas: Map<String, String>) {
        this.coordenadas = coordenadas
    }

    override fun evento(nome: String, params: Map<String, String>) {
        eventos.add(EventoRegistrado(nome, coordenadas + params))
    }

    override fun rastro(mensagem: String) {
        rastros.add(mensagem)
    }

    override fun naoFatal(erro: Throwable, chaves: Map<String, String>) {
        naoFatais.add(erro)
    }

    fun nomesDeEventos(): List<String> = eventos.map { it.nome }
}
