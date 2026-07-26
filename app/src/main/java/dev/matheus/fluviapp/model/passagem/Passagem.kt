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
    // Ponteiro estável para a Viagem (ADR-0008): id p/ relacionar/agregar. codigoViagem e
    // empresa/navio/origem/destino seguem como snapshot por valor (histórico imutável do bilhete).
    val viagemId: String = "",
    // Ids do navio/empresa congelados no momento da emissão (snapshot). O balanço agrega por navioId
    // (frozen) — rename/reatribuição posterior na Viagem não altera bilhetes históricos. empresaId
    // fica dormente até a relação Passagem→Empresa por id.
    val navioId: String = "",
    val empresaId: String = "",
    val codigoViagem: String,
    val empresa: String,
    val navio: String,
    val origem: String,
    val destino: String,
    val dataViagem: String,
    val horaViagem: String,
    val agencia: String = "",
    val agente: String = "",
    val valorPago: Double? = null,
    val valorPix: Double? = null,
    val valorDinheiro: Double? = null,
    val valorDebito: Double? = null,
    val valorCredito: Double? = null,
    // Tarifa da inteira congelada na emissão (ADR-0013): a célula da tabela da Viagem para a acomodação
    // escolhida. É a base de que a tarifa devida (meia = metade, gratuidade = 0) e o desconto derivam.
    // Aditivo; null cobre bilhetes anteriores e o veículo (tarifa por classe é Fase 3).
    val tarifaBase: Double? = null,
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
    // Cilindrada da moto (ADR-0013): o cc que justificou a tarifaBase; registro do bilhete. Aditivo.
    val cilindrada: String? = null,
    val funcionarioResponsavel: String,
    // Dono estável da passagem = uid do criador (ADR-0010 Fase 2). Congelado na emissão; o nome
    // (funcionarioResponsavel) segue como snapshot de exibição. Default "" cobre bilhetes anteriores.
    val funcionarioId: String = "",
    val status: String,
    // Registro do embarque (ADR-0012): quem validou o QR (uid, chave estável ADR-0008), o nome como
    // snapshot de exibição, e quando. Aditivos; default "" cobre bilhetes não embarcados/anteriores.
    val embarcadaPorId: String = "",
    val embarcadaPor: String = "",
    val embarcadaEm: String = "",
) {
    @Ignore
    val temPassageiro2 = !nomePassageiro2.isNullOrEmpty()

    @Ignore
    val temPassageiro3 = !nomePassageiro3.isNullOrEmpty()

    @Ignore
    val ehVeiculo = !placaVeiculo.isNullOrEmpty()
}



