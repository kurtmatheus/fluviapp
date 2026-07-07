package br.com.gruponaveg.extensions

import br.com.gruponaveg.model.passagem.Passagem
import br.com.gruponaveg.services.repository.firebase.documents.PassageiroDocumento
import br.com.gruponaveg.services.repository.firebase.documents.PassagemDocumento
import br.com.gruponaveg.services.repository.firebase.documents.VeiculoDocumento
import br.com.gruponaveg.services.repository.firebase.documents.ViagemDocumento

fun Passagem.toPassagemDocumento(): PassagemDocumento {
    return PassagemDocumento(
        numero = numero,
        viagem = ViagemDocumento(
            codigo = codigoViagem,
            empresa = empresa,
            navio = navio,
            origem = origem,
            destino = destino,
        ),
        dataViagem = dataViagem,
        horaViagem = horaViagem,
        valorPago = valorPago,
        valorPix = valorPix,
        valorDinheiro = valorDinheiro,
        valorDebito = valorDebito,
        valorCredito = valorCredito,
        desconto = desconto,
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
            corVeiculo = corVeiculo
        ),
        funcionarioResponsavel = funcionarioResponsavel,
        status = status
    )
}