package dev.matheus.fluviapp.domain.screendata

import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Descricao.REDE

data class DadosPassagem(
    val idPassagem: String = "",
    val idViagem: String = "",
    val numero: String = "",
    val empresaNome: String = "",
    val empresaRazaoSocial: String = "",
    val empresaCnpj: String = "",
    val empresaEndereco: String = "",
    val empresaTelefone1: String = "",
    val empresaTelefone2: String = "",
    val navio: String = "",
    val dataViagem: String = "",
    val horaViagem: String = "",
    val origem: String = "",
    val destino: String = "",
    val agencia: String = "",
    val tarifa: String = "",
    val valorTotal: String = "",
    val valorPix: String = "",
    val valorDinheiro: String = "",
    val valorDebito: String = "",
    val valorCredito: String = "",
    val desconto: String = "",
    val valorAPagar: String = "",
    val observacao: String = "",
    val tipoPassagem: String = "",
    val tipoGratuidade: String = "",
    val situacao: String = "",
    val categoriaPassagem: String = "",
    val funcionario: String = "",
    val idPassageiro1: String = "",
    val nomePassageiro1: String = "",
    val tipoDocumentoPassageiro1: String = "",
    val documentoPassageiro1: String = "",
    val dataNascimento1: String = "",
    val idPassageiro2: String = "",
    val nomePassageiro2: String = "",
    val tipoDocumentoPassageiro2: String = "",
    val documentoPassageiro2: String = "",
    val dataNascimento2: String = "",
    val idPassageiro3: String = "",
    val nomePassageiro3: String = "",
    val tipoDocumentoPassageiro3: String = "",
    val documentoPassageiro3: String = "",
    val dataNascimento3: String = "",
    val acomodacao: String = "",
    val idVeiculo: String = "",
    val nomeResponsavelRetirada: String = "",
    val numeroDocumentoResponsavelRetirada: String = "",
    val tipoVeiculo: String = "",
    val modeloVeiculo: String = "",
    val placaVeiculo: String = "",
    val corVeiculo: String = "",
) {
    val tem2Pessoas = nomePassageiro2.isNotBlank()

    val tem3Pessoas = nomePassageiro3.isNotBlank()

    val ehVeiculo = placaVeiculo.isNotBlank()

    val temResponsavel = nomeResponsavelRetirada.isNotBlank() && numeroDocumentoResponsavelRetirada.isNotBlank()

    val temGratuidade = tipoGratuidade.isNotBlank()

    val ehRede = acomodacao == REDE.name
}
