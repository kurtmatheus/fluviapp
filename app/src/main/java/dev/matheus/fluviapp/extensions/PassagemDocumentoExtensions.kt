package dev.matheus.fluviapp.extensions

import dev.matheus.fluviapp.model.passagem.Passagem
import dev.matheus.fluviapp.services.repository.firebase.documents.PassageiroDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.PassagemDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.VeiculoDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.ViagemDocumento

fun Passagem.toPassagemDocumento(): PassagemDocumento {
    return PassagemDocumento(
        numero = numero,
        viagemId = viagemId,
        viagem = ViagemDocumento(
            codigo = codigoViagem,
            empresa = empresa,
            navio = navio,
            origem = origem,
            destino = destino,
            // ids congelados no snapshot (ADR-0008): o balanço agrega por navioId (frozen).
            empresaId = empresaId,
            navioId = navioId,
        ),
        dataViagem = dataViagem,
        horaViagem = horaViagem,
        agencia = agencia,
        valorPix = valorPix,
        valorDinheiro = valorDinheiro,
        valorDebito = valorDebito,
        valorCredito = valorCredito,
        tarifaBase = tarifaBase,
        observacao = observacao,
        tipoPassagem = tipoPassagem,
        gratuidade = gratuidade,
        acomodacao = acomodacao,
        passageiro1 = PassageiroDocumento(
            nome = nomePassageiro1,
            documento = documentoPassageiro1,
            numeroDocumento = numeroDocumentoPassageiro1,
            dataNascimento = dataNascimentoPassageiro1
        ),
        passageiro2 = PassageiroDocumento(
            nome = nomePassageiro2,
            documento = documentoPassageiro2,
            numeroDocumento = numeroDocumentoPassageiro2,
            dataNascimento = dataNascimentoPassageiro2
        ),
        passageiro3 = PassageiroDocumento(
            nome = nomePassageiro3,
            documento = tipoDocumentoPassageiro3,
            numeroDocumento = numeroDocumentoPassageiro3,
            dataNascimento = dataNascimentoPassageiro3
        ),
        veiculo = VeiculoDocumento(
            nomeResponsavelRetirada = nomeResponsavelRetirada,
            documentoResponsavelRetirada = documentoResponsavelRetirada,
            numeroDocumentoResponsavelRetirada = numeroDocumentoResponsavelRetirada,
            tipoVeiculo = tipoVeiculo,
            modeloVeiculo = modeloVeiculo,
            placaVeiculo = placaVeiculo,
            corVeiculo = corVeiculo,
            cilindrada = cilindrada
        ),
        funcionarioResponsavel = funcionarioResponsavel,
        funcionarioId = funcionarioId,
        status = status,
        embarcadaPorId = embarcadaPorId,
        embarcadaPor = embarcadaPor,
        embarcadaEm = embarcadaEm
    )
}