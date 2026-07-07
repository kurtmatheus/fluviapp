package dev.matheus.fluviapp.services.network.cadastro.viagem

import dev.matheus.fluviapp.services.network.data.responses.cadastro.viagem.EmpresaResponse
import retrofit2.Response
import retrofit2.http.GET

interface EmpresaService {
    @GET("exec?table=empresa")
    suspend fun obterTodas(): Response<List<EmpresaResponse>>
}
