package br.com.gruponaveg.services.network.cadastro.viagem

import br.com.gruponaveg.services.network.data.requests.cadastro.ViagemRequest
import br.com.gruponaveg.services.network.data.responses.cadastro.viagem.ViagemResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ViagemService {

    @GET("api/v1/viagens/")
    suspend fun obterTodas(): Response<List<ViagemResponse>>

    @POST("api/v1/viagens")
    suspend fun salvar(@Body viagemRequest: ViagemRequest): Response<ViagemResponse>

    @PUT("api/v1/viagens/{id}")
    suspend fun atualizar(@Path("id") id: Int, @Body viagemRequest: ViagemRequest): Response<ViagemResponse>

    @HTTP(method = "DELETE", path = "api/v1/viagens/{id}", hasBody = false)
    fun deletar(@Path("id") id: Int): Call<ViagemResponse>
}
