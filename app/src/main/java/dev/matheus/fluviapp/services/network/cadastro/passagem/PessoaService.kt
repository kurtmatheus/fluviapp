package dev.matheus.fluviapp.services.network.cadastro.passagem

import dev.matheus.fluviapp.services.network.data.requests.cadastro.passagem.PessoaRequest
import dev.matheus.fluviapp.services.network.data.responses.cadastro.passagem.PessoaResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface PessoaService {
    @GET("api/v1/pessoas/")
    suspend fun obterTodas(): Response<List<PessoaResponse>>

    @POST("api/v1/pessoas")
    suspend fun salvar(@Body pessoaRequest: PessoaRequest): Response<PessoaResponse>

    @PUT("api/v1/pessoas/{id}")
    suspend fun atualizar(@Path("id") id: String, @Body pessoaRequest: PessoaRequest): Response<PessoaResponse>

    @HTTP(method = "DELETE", path = "api/v1/pessoas/{id}", hasBody = false)
    fun deletar(@Path("id") id: String): Call<PessoaResponse>
}
