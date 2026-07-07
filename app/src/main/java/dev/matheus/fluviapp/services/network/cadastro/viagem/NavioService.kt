package dev.matheus.fluviapp.services.network.cadastro.viagem

import dev.matheus.fluviapp.services.network.data.responses.cadastro.viagem.NavioResponse
import retrofit2.Response
import retrofit2.http.GET

interface NavioService {
    @GET("exec?table=navio")
    suspend fun obterTodos(): Response<List<NavioResponse>>
}
