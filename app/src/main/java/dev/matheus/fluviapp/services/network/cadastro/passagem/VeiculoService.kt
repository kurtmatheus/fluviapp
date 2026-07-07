package dev.matheus.fluviapp.services.network.cadastro.passagem

import dev.matheus.fluviapp.services.network.data.requests.cadastro.passagem.VeiculoRequest
import dev.matheus.fluviapp.services.network.data.responses.cadastro.passagem.VeiculoResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface VeiculoService {
    @GET("api/v1/veiculos/")
    suspend fun obterTodos(): Response<List<VeiculoResponse>>

    @POST("api/v1/veiculos")
    suspend fun salvar(@Body veiculoRequest: VeiculoRequest): Response<VeiculoResponse>

    @PUT("api/v1/veiculos/{id}")
    suspend fun atualizar(@Path("id") id: String, @Body veiculoRequest: VeiculoRequest): Response<VeiculoResponse>

    @HTTP(method = "DELETE", path = "api/v1/veiculos/{id}", hasBody = false)
    fun deletar(@Path("id") id: String): Call<VeiculoResponse>
}
