package dev.matheus.fluviapp.model.passagem

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index("id")])
data class Passagem(
    @PrimaryKey
    val id: String,
    val numero: String,
    val codigoViagem: String,
    val empresa: String,
    val navio: String,
    val origem: String,
    val destino: String,
    val dataViagem: String,
    val horaViagem: String,
    val valorPago: Double? = null,
    val valorPix: Double? = null,
    val valorDinheiro: Double? = null,
    val valorDebito: Double? = null,
    val valorCredito: Double? = null,
    val desconto: Double? = null,
    val observacao: String? = null,
    val tipoPassagem: String? = null,
    val gratuidade: String? = null,
    val acomodacao: String? = null,
    val nomePassageiro1: String? = null,
    val documentoPassageiro1: String? = null,
    val numeroDocumentoPassageiro1: String? = null,
    val dataNascimentoPassageiro1: String? = null,
    val nomePassageiro2: String? = null,
    val documentoPassageiro2: String? = null,
    val numeroDocumentoPassageiro2: String? = null,
    val dataNascimentoPassageiro2: String? = null,
    val nomePassageiro3: String? = null,
    val tipoDocumentoPassageiro3: String? = null,
    val numeroDocumentoPassageiro3: String? = null,
    val dataNascimentoPassageiro3: String? = null,
    val nomeResponsavelRetirada: String? = null,
    val documentoResponsavelRetirada: String? = null,
    val numeroDocumentoResponsavelRetirada: String? = null,
    val tipoVeiculo: String? = null,
    val modeloVeiculo: String? = null,
    val placaVeiculo: String? = null,
    val corVeiculo: String? = null,
    val funcionarioResponsavel: String,
    val status: String,
) {
    @Ignore
    val temPassageiro2 = !nomePassageiro2.isNullOrEmpty()

    @Ignore
    val temPassageiro3 = !nomePassageiro3.isNullOrEmpty()

    @Ignore
    val ehVeiculo = !placaVeiculo.isNullOrEmpty()

    companion object {
        const val TARIFA_ANTAC = "300"
        const val DESCONTO_ANTAC = "50"
    }
}



