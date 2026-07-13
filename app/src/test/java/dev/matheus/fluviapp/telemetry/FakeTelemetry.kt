package dev.matheus.fluviapp.telemetry

/** Test double que grava as chamadas de telemetria, para asserção nos testes. */
class FakeTelemetry : Telemetry {

    data class EventoRegistrado(val nome: String, val params: Map<String, String>)

    val eventos = mutableListOf<EventoRegistrado>()
    val rastros = mutableListOf<String>()
    val naoFatais = mutableListOf<Throwable>()

    override fun evento(nome: String, params: Map<String, String>) {
        eventos.add(EventoRegistrado(nome, params))
    }

    override fun rastro(mensagem: String) {
        rastros.add(mensagem)
    }

    override fun naoFatal(erro: Throwable, chaves: Map<String, String>) {
        naoFatais.add(erro)
    }

    fun nomesDeEventos(): List<String> = eventos.map { it.nome }
}
