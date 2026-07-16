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
import dev.matheus.fluviapp.services.repository.cadastro.passagem.AgenteRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaRepository
import dev.matheus.fluviapp.util.Mapper
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PassagemDadosPassagemMapper @Inject constructor(
    private val empresaRepository: EmpresaRepository,
    private val agenteRepository: AgenteRepository,
) : Mapper<Passagem, DadosPassagem> {
    override suspend fun map(entry: Passagem): DadosPassagem {
        // Empresa resolvida por id (ADR-0008): rename-safe e órfão detectável (obterPorId → null),
        // onde obterPorNome estourava. cnpj/endereço/telefones seguem vivos (nunca foram snapshot).
        // Sem ida à Viagem: idViagem usa o viagemId congelado na Passagem (dropou ViagemRepository).
        val empresa = empresaRepository.obterPorId(entry.empresaId)

        // Flip da capability (ADR-0002/0003): deriva do agente que vendeu a passagem.
        // Best-effort — o agente é texto livre no form; casa por agência + nome no cadastro.
        val podeSelecionarFormaPagamento = if (entry.agencia.isNotBlank()) {
            agenteRepository.obterAgentesPorAgencia(entry.agencia)
                .firstOrNull { it.descricaoNome == entry.agente }
                ?.podeSelecionarFormaPagamento ?: false
        } else {
            false
        }

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
            idViagem = entry.viagemId,
            numero = entry.numero,
            empresaNome = empresa?.nome.orEmpty(),
            empresaRazaoSocial = empresa?.razaoSocial.orEmpty(),
            empresaCnpj = empresa?.cnpj.orEmpty(),
            empresaEndereco = empresa?.endereco.orEmpty(),
            empresaTelefone1 = empresa?.telefone1.orEmpty(),
            empresaTelefone2 = empresa?.telefone2.orEmpty(),
            navio = entry.navio,
            dataViagem = entry.dataViagem,
            horaViagem = entry.horaViagem,
            origem = entry.origem,
            destino = entry.destino,
            agencia = entry.agencia,
            agente = entry.agente,
            podeSelecionarFormaPagamento = podeSelecionarFormaPagamento,
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