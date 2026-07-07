package br.com.gruponaveg.services.network.cadastro

import br.com.gruponaveg.services.network.data.responses.cadastro.constantes.ConstanteConteudoResponse
import retrofit2.Response
import retrofit2.http.GET

interface ConstantesService {
    @GET("exec?table=constante_conteudo")
    suspend fun obterTodasConteudos(): Response<List<ConstanteConteudoResponse>>
}