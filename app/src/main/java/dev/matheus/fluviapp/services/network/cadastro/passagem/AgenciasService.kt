package dev.matheus.fluviapp.services.network.cadastro.passagem

import dev.matheus.fluviapp.services.network.data.responses.cadastro.passagem.AgenciaResponse
import dev.matheus.fluviapp.services.network.data.responses.cadastro.passagem.AgenteResponse
import retrofit2.Response
import retrofit2.http.GET

interface AgenciasService {
    @GET("exec?table=agencia")
    suspend fun obterTodasAgencias(): Response<List<AgenciaResponse>>

    @GET("exec?table=agente")
    suspend fun obterTodosAgentes(): Response<List<AgenteResponse>>
}