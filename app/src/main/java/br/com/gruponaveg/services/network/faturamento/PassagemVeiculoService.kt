package br.com.gruponaveg.services.network.faturamento

import br.com.gruponaveg.services.network.data.requests.faturamento.PassagemVeiculoRequest
import br.com.gruponaveg.services.network.data.responses.faturamento.PassagemVeiculoResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface PassagemVeiculoService {
    @GET("api/v1/passagens-veiculos/")
    suspend fun obterTodas(): Response<List<PassagemVeiculoResponse>>

    @POST("api/v1/passagens-veiculos")
    suspend fun salvar(@Body passagemVeiculoRequest: PassagemVeiculoRequest): Response<PassagemVeiculoResponse>

    @PUT("api/v1/passagens-veiculos/{id}")
    suspend fun atualizar(@Path("id") id: String, @Body passagemVeiculoRequest: PassagemVeiculoRequest): Response<PassagemVeiculoResponse>

    @HTTP(method = "DELETE", path = "api/v1/passagens-veiculos/{id}", hasBody = false)
    fun deletar(@Path("id") id: String): Call<PassagemVeiculoResponse>

}
