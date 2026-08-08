package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.domain.passagem.Passagem

data class PassagemDocumento(
    val numero: String = "",
    // FK da Passagem -> Viagem por id (ADR-0008). Fica top-level (é ponteiro que a passagem possui),
    // separado do `viagem` embutido, que é o snapshot por valor. Default "" cobre docs antigos.
    val viagemId: String = "",
    val viagem: ViagemDocumento? = null,
    val dataViagem: String = "",
    val horaViagem: String = "",
    val passageiro1: PassageiroDocumento? = null,
    val passageiro2: PassageiroDocumento? = null,
    val passageiro3: PassageiroDocumento? = null,
    val veiculo: VeiculoDocumento? = null,
    val agencia: String = "",
    /** A mesma agência, **por id** (F7): é ela que a F9 vai usar para recortar por empresa. */
    val agenciaId: String = "",
    val valorPix: Double? = null,
    val valorDinheiro: Double? = null,
    val valorDebito: Double? = null,
    val valorCredito: Double? = null,
    // Tarifa da inteira congelada na emissão (ADR-0013), top-level e schemaless; null cobre docs antigos.
    val tarifaBase: Double? = null,
    val observacao: String? = null,
    val tipoPassagem: String? = null,
    val gratuidade: String? = null,
    val acomodacao: String? = null,
    val funcionarioResponsavel: String = "",
    // uid do criador (ADR-0010 Fase 2). Top-level, schemaless; default "" cobre docs anteriores.
    val funcionarioId: String = "",
    val status: String = "",
    // Registro do embarque (ADR-0012), top-level e schemaless; default "" cobre docs anteriores.
    val embarcadaPorId: String = "",
    val embarcadaPor: String = "",
    val embarcadaEm: String = ""
)

fun PassagemDocumento.toPassagem(id: String): Passagem {
    return Passagem(
        id = id,
        numero = numero,
        viagemId = viagemId,
        // embarcacaoId/empresaId vêm do snapshot embutido (ViagemDocumento já os carrega — ADR-0008 Fase 0).
        embarcacaoId = viagem?.embarcacaoId.orEmpty(),
        empresaId = viagem?.empresaId.orEmpty(),
        codigoViagem = viagem?.codigo.orEmpty(),
        empresa = viagem?.empresa.orEmpty(),
        embarcacao = viagem?.embarcacao.orEmpty(),
        origem = viagem?.origem.orEmpty(),
        destino = viagem?.destino.orEmpty(),
        dataViagem = dataViagem,
        horaViagem = horaViagem,
        agencia = agencia,
        agenciaId = agenciaId,
        valorPix = valorPix,
        valorDinheiro = valorDinheiro,
        valorDebito = valorDebito,
        valorCredito = valorCredito,
        tarifaBase = tarifaBase,
        observacao = observacao,
        tipoPassagem = tipoPassagem,
        gratuidade = gratuidade,
        acomodacao = acomodacao,
        nomePassageiro1 = passageiro1?.nome,
        documentoPassageiro1 = passageiro1?.documento,
        numeroDocumentoPassageiro1 = passageiro1?.numeroDocumento,
        dataNascimentoPassageiro1 = passageiro1?.dataNascimento,
        nomePassageiro2 = passageiro2?.nome,
        documentoPassageiro2 = passageiro2?.documento,
        numeroDocumentoPassageiro2 = passageiro2?.numeroDocumento,
        dataNascimentoPassageiro2 = passageiro2?.dataNascimento,
        nomePassageiro3 = passageiro3?.nome,
        tipoDocumentoPassageiro3 = passageiro3?.documento,
        numeroDocumentoPassageiro3 = passageiro3?.numeroDocumento,
        dataNascimentoPassageiro3 = passageiro3?.dataNascimento,
        nomeResponsavelRetirada = veiculo?.nomeResponsavelRetirada,
        documentoResponsavelRetirada = veiculo?.documentoResponsavelRetirada,
        numeroDocumentoResponsavelRetirada = veiculo?.numeroDocumentoResponsavelRetirada,
        tipoVeiculo = veiculo?.tipoVeiculo,
        modeloVeiculo = veiculo?.modeloVeiculo,
        placaVeiculo = veiculo?.placaVeiculo,
        corVeiculo = veiculo?.corVeiculo,
        cilindrada = veiculo?.cilindrada,
        funcionarioResponsavel = funcionarioResponsavel,
        funcionarioId = funcionarioId,
        status = status,
        embarcadaPorId = embarcadaPorId,
        embarcadaPor = embarcadaPor,
        embarcadaEm = embarcadaEm
    )
}
