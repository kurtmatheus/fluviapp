package dev.matheus.fluviapp.fakes

import dev.matheus.fluviapp.services.ibge.ConsultaMunicipioIbge
import dev.matheus.fluviapp.services.ibge.ResultadoConsultaIbge

/**
 * Fake da porta [ConsultaMunicipioIbge] — é ele que torna a ajuda de digitação testável **sem rede**.
 *
 * O padrão é [ResultadoConsultaIbge.Indisponivel] de propósito: o teste que não declara o que o IBGE
 * responde está, na prática, testando o app sem IBGE — que é o cenário que precisa continuar funcionando.
 */
class FakeConsultaMunicipioIbge : ConsultaMunicipioIbge {

    var resultado: ResultadoConsultaIbge = ResultadoConsultaIbge.Indisponivel

    val consultados = mutableListOf<String>()

    override suspend fun consultar(codigoIbge: String): ResultadoConsultaIbge {
        consultados += codigoIbge
        return resultado
    }
}