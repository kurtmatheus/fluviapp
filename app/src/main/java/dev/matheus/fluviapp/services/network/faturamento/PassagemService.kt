package dev.matheus.fluviapp.services.network.faturamento

import dev.matheus.fluviapp.services.network.data.requests.faturamento.PassagemRequest
import dev.matheus.fluviapp.services.network.data.responses.faturamento.PassagemResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface PassagemService {

    @GET("api/v1/passagens/")
    suspend fun obterTodas(): Response<List<PassagemResponse>>

    @POST("api/v1/passagens")
    suspend fun salvar(@Body passagemRequest: PassagemRequest): Response<PassagemResponse>

    @PUT("api/v1/passagens/{id}")
    suspend fun atualizar(@Path("id") id: String, @Body passagemRequest: PassagemRequest): Response<PassagemResponse>

    @HTTP(method = "DELETE", path = "api/v1/passagens/{id}", hasBody = false)
    fun deletar(@Path("id") id: String): Call<PassagemResponse>
}
