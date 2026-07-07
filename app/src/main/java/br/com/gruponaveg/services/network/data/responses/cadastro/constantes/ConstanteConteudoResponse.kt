package br.com.gruponaveg.services.network.data.responses.cadastro.constantes

import br.com.gruponaveg.model.IObjetoSimplificado
import com.fasterxml.jackson.annotation.JsonProperty

data class ConstanteConteudoResponse(
    @JsonProperty("id") override val id: String,
    @JsonProperty("descricao") override val descricaoNome: String,
    @JsonProperty("constanteCategoriaId") val idConstanteCategoria: Int
) : IObjetoSimplificado
