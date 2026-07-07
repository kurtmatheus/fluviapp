package br.com.gruponaveg.services.network.cadastro.passagem

import br.com.gruponaveg.services.network.data.responses.cadastro.passagem.AgenciaResponse
import br.com.gruponaveg.services.network.data.responses.cadastro.passagem.AgenteResponse
import retrofit2.Response
import retrofit2.http.GET

interface AgenciasService {
    @GET("exec?table=agencia")
    suspend fun obterTodasAgencias(): Response<List<AgenciaResponse>>

    @GET("exec?table=agente")
    suspend fun obterTodosAgentes(): Response<List<AgenteResponse>>
}