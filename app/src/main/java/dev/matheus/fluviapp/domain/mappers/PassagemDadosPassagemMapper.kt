package dev.matheus.fluviapp.domain.mappers

import dev.matheus.fluviapp.extensions.converterParaBigDecimal
import dev.matheus.fluviapp.extensions.extrairDocumentoFormatado
import dev.matheus.fluviapp.extensions.formataParaMoedaBrasileira
import dev.matheus.fluviapp.extensions.getValorFormatadoOrEmpty
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Descricao.PASSAGEIRO
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Descricao.VEICULO
import dev.matheus.fluviapp.domain.passagem.Passagem
import dev.matheus.fluviapp.domain.passagem.StatusPassagem
import dev.matheus.fluviapp.domain.passagem.TipoPassagem
import dev.matheus.fluviapp.domain.passagem.descontoDerivado
import dev.matheus.fluviapp.domain.screendata.DadosPassagem
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmbarcacaoRepository
import dev.matheus.fluviapp.util.Mapper
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PassagemDadosPassagemMapper @Inject constructor(
    private val empresaRepository: EmpresaRepository,
    private val embarcacaoRepository: EmbarcacaoRepository,
) : Mapper<Passagem, DadosPassagem> {
    override suspend fun map(entry: Passagem): DadosPassagem {
        // Empresa resolvida por id (ADR-0008): rename-safe e órfão detectável (obterPorId → null),
        // onde obterPorNome estourava. cnpj/endereço/telefones seguem vivos (nunca foram snapshot).
        // Sem ida à Viagem: idViagem usa o viagemId congelado na Passagem (dropou ViagemRepository).
        val empresa = empresaRepository.obterPorId(entry.empresaId)

        // Embarcacao resolvido por id (ADR-0008), espelhando a empresa: usa o embarcacaoId congelado na Passagem,
        // rename-safe e órfão detectável (obterPorId → null → nome vazio). Fecha a dívida do §9 do doc de
        // domínio (o nome vinha do snapshot entry.embarcacao, que renomear a frota não atualizava).
        val embarcacao = embarcacaoRepository.obterPorId(entry.embarcacaoId)

        val valorPix = entry.valorPix.converterParaBigDecimal()
        val valorDinheiro = entry.valorDinheiro.converterParaBigDecimal()
        val valorDebito = entry.valorDebito.converterParaBigDecimal()
        val valorCredito = entry.valorCredito.converterParaBigDecimal()
        // Cobrado = soma das formas de pagamento. O "valor pago" avulso saiu (ADR-0015 §4a): era a
        // parcela de quando não se escolhia forma de pagamento, cenário que não existe mais.
        val valorCobrado = valorPix + valorDinheiro + valorDebito + valorCredito

        // Preço tabelado (ADR-0013): da tarifa da inteira congelada (tarifaBase) deriva a tarifa devida
        // por categoria (meia = metade, gratuidade = 0) e o desconto (resíduo abaixo da devida — só a
        // redução discricionária, sem embaralhar a meia). Sem tarifaBase (não deve ocorrer com a emissão
        // fail-closed) degrada para o valor cobrado, sem desconto (o desconto foi removido da persistência).
        val precos = if (entry.tarifaBase != null) {
            val tarifaBase = entry.tarifaBase.converterParaBigDecimal()
            val tarifaDevida = TipoPassagem.de(entry.tipoPassagem)?.tarifaDevida(tarifaBase) ?: tarifaBase
            val descontoDado = descontoDerivado(tarifaDevida, valorCobrado)
            Precos(tarifaBase, tarifaDevida, descontoDado, tarifaDevida - descontoDado)
        } else {
            Precos(valorCobrado, valorCobrado, BigDecimal.ZERO, valorCobrado)
        }

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
            embarcacao = embarcacao?.descricaoNome.orEmpty(),
            dataViagem = entry.dataViagem,
            horaViagem = entry.horaViagem,
            origem = entry.origem,
            destino = entry.destino,
            agencia = entry.agencia,
            tarifa = precos.tarifa.formataParaMoedaBrasileira(),
            valorTotal = precos.total.formataParaMoedaBrasileira(),
            valorPix = valorPix.getValorFormatadoOrEmpty(),
            valorDinheiro = valorDinheiro.getValorFormatadoOrEmpty(),
            valorDebito = valorDebito.getValorFormatadoOrEmpty(),
            valorCredito = valorCredito.getValorFormatadoOrEmpty(),
            desconto = precos.desconto.formataParaMoedaBrasileira(),
            valorAPagar = precos.aPagar.formataParaMoedaBrasileira(),
            observacao = entry.observacao.orEmpty(),
            tipoPassagem = entry.tipoPassagem.orEmpty(),
            tipoGratuidade = entry.gratuidade.orEmpty(),
            // Exibição pelo rótulo do tipo (ADR-0012); tolera grafia legada de bilhetes antigos.
            situacao = StatusPassagem.de(entry.status)?.rotulo() ?: entry.status,
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

}

/** Preços derivados de uma passagem (ADR-0013), em BigDecimal, prontos para formatar na projeção de tela. */
private data class Precos(
    val tarifa: BigDecimal,
    val total: BigDecimal,
    val desconto: BigDecimal,
    val aPagar: BigDecimal,
)