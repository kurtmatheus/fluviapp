package br.com.gruponaveg.services.network.data.responses

import br.com.gruponaveg.services.network.data.responses.cadastro.constantes.ConstanteConteudoResponse
import br.com.gruponaveg.services.network.data.responses.cadastro.passagem.PessoaResponse
import br.com.gruponaveg.services.network.data.responses.faturamento.PassagemResponse
import com.fasterxml.jackson.annotation.JsonProperty

data class PassagemPassageiroResponse(
    @JsonProperty("id") val id: String,
    @JsonProperty("passagemModel") val passagemResponse: PassagemResponse,
    @JsonProperty("passageiro1Model") val passageiro1Response: PessoaResponse,
    @JsonProperty("passageiro2Model") val passageiro2Response: PessoaResponse? = null,
    @JsonProperty("passageiro3Model") val passageiro3Response: PessoaResponse? = null,
    @JsonProperty("acomodacaoModel") val acomodacaoResponse: ConstanteConteudoResponse,
    @JsonProperty("tipoPassagemModel") val tipoPassagemResponse: ConstanteConteudoResponse,
    @JsonProperty("tipoGratuidadeModel") val tipoGratuidadeResponse: ConstanteConteudoResponse
)
