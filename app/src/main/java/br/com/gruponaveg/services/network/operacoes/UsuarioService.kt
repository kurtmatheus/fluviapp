package br.com.gruponaveg.services.network.operacoes

import br.com.gruponaveg.services.network.data.responses.operacoes.UsuarioResponse
import retrofit2.Response
import retrofit2.http.GET

interface UsuarioService {
    @GET("exec?table=usuario")
    suspend fun obterUsuarios(): Response<List<UsuarioResponse>>
}