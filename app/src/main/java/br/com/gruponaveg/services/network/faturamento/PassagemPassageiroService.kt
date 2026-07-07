package br.com.gruponaveg.services.network.faturamento

import br.com.gruponaveg.services.network.data.requests.faturamento.PassagemPassageiroRequest
import br.com.gruponaveg.services.network.data.responses.PassagemPassageiroResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface PassagemPassageiroService {
    @GET("api/v1/passagens-passageiros/")
    suspend fun obterTodas(): Response<List<PassagemPassageiroResponse>>

    @POST("api/v1/passagens-passageiros")
    suspend fun salvar(@Body passagemVeiculoRequest: PassagemPassageiroRequest): Response<PassagemPassageiroResponse>

    @PUT("api/v1/passagens-passageiros/{id}")
    suspend fun atualizar(
        @Path("id") id: String,
        @Body passagemVeiculoRequest: PassagemPassageiroRequest
    ): Response<PassagemPassageiroResponse>

    @HTTP(method = "DELETE", path = "api/v1/passagens-passageiros/{id}", hasBody = false)
    fun deletar(@Path("id") id: String): Call<PassagemPassageiroResponse>
}
