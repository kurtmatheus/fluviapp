package dev.matheus.fluviapp.model.mappers

import dev.matheus.fluviapp.extensions.converterParaBigDecimal
import dev.matheus.fluviapp.extensions.extrairDocumentoFormatado
import dev.matheus.fluviapp.extensions.formataParaMoedaBrasileira
import dev.matheus.fluviapp.extensions.getValorFormatadoOrEmpty
import dev.matheus.fluviapp.extensions.toBigDecimal
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.GRATUIDADE
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.MEIA
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.PASSAGEIRO
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.REDE
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.VEICULO
import dev.matheus.fluviapp.model.passagem.Passagem
import dev.matheus.fluviapp.model.passagem.Passagem.Companion.TARIFA_ANTAC
import dev.matheus.fluviapp.model.screendata.DadosPassagem
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaRepository
import dev.matheus.fluviapp.services.repository.firebase.ViagemFirestoreRepository
import dev.matheus.fluviapp.util.Mapper
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PassagemDadosPassagemMapper @Inject constructor(
    private val viagemRepository: ViagemFirestoreRepository,
    private val empresaRepository: EmpresaRepository,
) : Mapper<Passagem, DadosPassagem> {
    override fun map(entry: Passagem): DadosPassagem {
        val viagem = runBlocking { viagemRepository.obterPorCodigo(entry.codigoViagem) }
        val empresa = runBlocking { empresaRepository.obterPorNome(entry.empresa) }

        val valorPagoAvulso = entry.valorPago.converterParaBigDecimal()
        val valorPix = entry.valorPix.converterParaBigDecimal()
        val valorDinheiro = entry.valorDinheiro.converterParaBigDecimal()
        val valorDebito = entry.valorDebito.converterParaBigDecimal()
        val valorCredito = entry.valorCredito.converterParaBigDecimal()
        val desconto = entry.desconto.converterParaBigDecimal()

        val valorTotal = if (entry.acomodacao != null &&
            entry.acomodacao == REDE.name &&
            entry.tipoPassagem != null &&
            entry.tipoPassagem != GRATUIDADE.name
        ) {
            obterTotalTarifa(entry.tipoPassagem == MEIA.name)
        } else {
            getValorTotal(valorPagoAvulso, valorPix, valorDinheiro, valorDebito, valorCredito, desconto)
        }

        val valorAPagar = valorTotal - desconto

        return DadosPassagem(
            idPassagem = entry.id,
            idViagem = viagem.id,
            numero = entry.numero,
            empresaNome = empresa.nome,
            empresaRazaoSocial = empresa.razaoSocial,
            empresaCnpj = empresa.cnpj,
            empresaEndereco = empresa.endereco,
            empresaTelefone1 = empresa.telefone1,
            empresaTelefone2 = empresa.telefone2,
            navio = entry.navio,
            dataViagem = entry.dataViagem,
            horaViagem = entry.horaViagem,
            origem = entry.origem,
            destino = entry.destino,
            tarifa = BigDecimal(TARIFA_ANTAC).formataParaMoedaBrasileira(),
            valorTotal = valorTotal.formataParaMoedaBrasileira(),
            valorPix = valorPix.getValorFormatadoOrEmpty(),
            valorDinheiro = valorDinheiro.getValorFormatadoOrEmpty(),
            valorDebito = valorDebito.getValorFormatadoOrEmpty(),
            valorCredito = valorCredito.getValorFormatadoOrEmpty(),
            desconto = desconto.formataParaMoedaBrasileira(),
            valorAPagar = valorAPagar.formataParaMoedaBrasileira(),
            observacao = entry.observacao.orEmpty(),
            tipoPassagem = entry.tipoPassagem.orEmpty(),
            tipoGratuidade = entry.gratuidade.orEmpty(),
            situacao = entry.status,
            categoriaPassagem = if (entry.ehVeiculo) VEICULO.name else PASSAGEIRO.name,
            funcionario = entry.funcionarioResponsavel,
            idPassageiro1 = "",
            nomePassageiro1 = entry.nomePassageiro1.orEmpty(),
            tipoDocumentoPassageiro1 = entry.documentoPassageiro1.orEmpty(),
            documentoPassageiro1 = entry.numeroDocumentoPassageiro1?.extrairDocumentoFormatado(
                true,
                entry.documentoPassageiro1
            ).orEmpty(),
            dataNascimento1 = entry.dataNascimentoPassageiro1.orEmpty(),
            idPassageiro2 = "",
            nomePassageiro2 = entry.nomePassageiro2.orEmpty(),
            tipoDocumentoPassageiro2 = entry.documentoPassageiro2.orEmpty(),
            documentoPassageiro2 = entry.numeroDocumentoPassageiro2?.extrairDocumentoFormatado(
                true,
                entry.documentoPassageiro2
            ).orEmpty(),
            dataNascimento2 = entry.dataNascimentoPassageiro2.orEmpty(),
            nomePassageiro3 = entry.nomePassageiro3.orEmpty(),
            tipoDocumentoPassageiro3 = entry.tipoDocumentoPassageiro3.orEmpty(),
            documentoPassageiro3 = entry.numeroDocumentoPassageiro3?.extrairDocumentoFormatado(
                true,
                entry.tipoDocumentoPassageiro3
            ).orEmpty(),
            dataNascimento3 = entry.dataNascimentoPassageiro3.orEmpty(),
            acomodacao = entry.acomodacao.orEmpty(),
            nomeResponsavelRetirada = entry.nomeResponsavelRetirada.orEmpty(),
            numeroDocumentoResponsavelRetirada = entry.numeroDocumentoResponsavelRetirada?.extrairDocumentoFormatado(
                true,
                entry.documentoResponsavelRetirada
            ).orEmpty(),
            idVeiculo = "",
            tipoVeiculo = entry.tipoVeiculo.orEmpty(),
            modeloVeiculo = entry.modeloVeiculo.orEmpty(),
            placaVeiculo = entry.placaVeiculo.orEmpty(),
            corVeiculo = entry.corVeiculo.orEmpty()
        )
    }

    private fun obterTotalTarifa(isMeia: Boolean): BigDecimal {
        return if (isMeia) {
            TARIFA_ANTAC.toBigDecimal().divide(BigDecimal("2"), RoundingMode.UP)
        } else {
            TARIFA_ANTAC.toBigDecimal()
        }
    }
}

private fun getValorTotal(
    valorPago: BigDecimal,
    valorPix: BigDecimal,
    valorDinheiro: BigDecimal,
    valorDebito: BigDecimal,
    valorCredito: BigDecimal,
    desconto: BigDecimal,
): BigDecimal {
    return valorPago +
            valorPix +
            valorDinheiro +
            valorDebito +
            valorCredito +
            desconto
}