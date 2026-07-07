package br.com.gruponaveg.services.network.data.responses.faturamento

import br.com.gruponaveg.services.network.data.responses.cadastro.constantes.ConstanteConteudoResponse
import br.com.gruponaveg.services.network.data.responses.cadastro.passagem.AgenteResponse
import br.com.gruponaveg.services.network.data.responses.cadastro.viagem.ViagemResponse
import com.fasterxml.jackson.annotation.JsonProperty

data class PassagemResponse(
    @JsonProperty("id") val id: String,
    @JsonProperty("numeroBilhete") val numeroBilhete: Int,
    @JsonProperty("viagemModel") val viagemResponse: ViagemResponse,
    @JsonProperty("dataViagem") val dataViagem: String,
    @JsonProperty("horaViagem") val horaViagem: String,
    @JsonProperty("agenteModel") val agenteResponse: AgenteResponse,
    @JsonProperty("valorAvulso") val valorAvulso: Double,
    @JsonProperty("valorPix") val valorPix: Double,
    @JsonProperty("valorDinheiro") val valorDinheiro: Double,
    @JsonProperty("valorDebito") val valorDebito: Double,
    @JsonProperty("valorCredito") val valorCredito: Double,
    @JsonProperty("valorDesconto") val valorDesconto: Double,
    @JsonProperty("categoriaModel") val categoriaResponse: ConstanteConteudoResponse,
    @JsonProperty("situacaoModel") val situacaoResponse: ConstanteConteudoResponse,
    @JsonProperty("funcionario") val funcionarioModel: String
)
