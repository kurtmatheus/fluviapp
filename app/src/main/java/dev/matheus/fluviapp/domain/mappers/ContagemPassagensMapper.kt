package dev.matheus.fluviapp.domain.mappers

import dev.matheus.fluviapp.domain.passagem.ModoPassagem
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Descricao.CAMINHAO
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Descricao.CARRETA
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Descricao.CARRO
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Descricao.GRATUIDADE
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Descricao.INTEIRA
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Descricao.MEIA
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Descricao.MOTO
import dev.matheus.fluviapp.domain.passagem.Passagem
import dev.matheus.fluviapp.domain.screendata.DadosContagemPassagem
import dev.matheus.fluviapp.domain.viagem.Embarcacao
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmbarcacaoRepository
import dev.matheus.fluviapp.util.Mapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContagemPassagensMapper @Inject constructor(
    private val embarcacaoRepository: EmbarcacaoRepository
) : Mapper<List<Passagem>, List<DadosContagemPassagem>> {

    override suspend fun map(entry: List<Passagem>): List<DadosContagemPassagem> {
        // Agrega pelo embarcacaoId CONGELADO na Passagem (ADR-0008 Fase 2): sem ida à Viagem viva, então
        // rename/reatribuição posterior não altera balanços históricos. obterTodos uma vez (não N+1);
        // associateBy dá lookup O(1) por id (era firstOrNull O(embarcacoes) por grupo).
        val embarcacoesPorId = embarcacaoRepository.obterTodos().associateBy { it.id }

        return entry
            .groupBy { it.embarcacaoId }
            .mapNotNull { (embarcacaoId, passagens) ->
                // Embarcacao removido → órfão detectável (lookup por id retorna null); descarta o grupo.
                val embarcacao = embarcacoesPorId[embarcacaoId] ?: return@mapNotNull null
                contarOcupacaoEmbarcacao(embarcacao, passagens)
            }
    }
}

/**
 * Contagem pura da ocupação de um embarcacao a partir das suas passagens — função testável sem repositório.
 *
 * Contagem **por bilhete/unidade** (decisão do analista, `balanco-passagens-mapper.md` §7): cada bilhete de
 * suíte ocupa **uma** suíte — `temPassageiro3` (trio; p3 ⇒ p2) cai no bucket de 3 pessoas, senão no de 2 (o
 * titular-solo conta). Rede/camarote/veículo contam 1 por bilhete. O breakdown inteira/meia/gratuidade vive
 * no ramo REDE porque o tipo tarifário só existe na rede (ADR-0013).
 */
internal fun contarOcupacaoEmbarcacao(
    embarcacao: Embarcacao,
    listaPassagem: List<Passagem>,
): DadosContagemPassagem {
    var preenchidasRede = 0
    var preenchidasInteiras = 0
    var preenchidasMeias = 0
    var preenchidasGratuidades = 0
    var preenchidasSuite = 0
    var preenchidasSuite2Pessoas = 0
    var preenchidasSuite3Pessoas = 0
    var preenchidasCamarote = 0
    var preenchidosVeiculos = 0
    var totalCarros = 0
    var totalMotos = 0
    var totalCaminhoes = 0
    var totalCarretas = 0

    listaPassagem.forEach { passagem ->
        if (!passagem.ehVeiculo) {
            // Comparação pelo TIPO, não pela String (ADR-0018 D6). Antes era `acomodacao == "REDE"`
            // contra um catálogo que gravava "Rede" e "Suíte p/ 2 Pessoas": **nada casava**, e a
            // contagem inteira caía no `else` — zerada, em silêncio.
            when (ModoPassagem.de(passagem.acomodacao)) {
                ModoPassagem.REDE -> {
                    preenchidasRede = preenchidasRede.inc()

                    when (passagem.tipoPassagem) {
                        INTEIRA.name -> preenchidasInteiras = preenchidasInteiras.inc()
                        MEIA.name -> preenchidasMeias = preenchidasMeias.inc()
                        GRATUIDADE.name -> preenchidasGratuidades = preenchidasGratuidades.inc()
                        else -> {}
                    }
                }

                ModoPassagem.SUITE -> {
                    // Uma suíte por bilhete; trio (p3) → bucket 3 pessoas, senão 2 (inclui o titular-solo).
                    preenchidasSuite = preenchidasSuite.inc()
                    if (passagem.temPassageiro3) {
                        preenchidasSuite3Pessoas = preenchidasSuite3Pessoas.inc()
                    } else {
                        preenchidasSuite2Pessoas = preenchidasSuite2Pessoas.inc()
                    }
                }

                ModoPassagem.CAMAROTE -> preenchidasCamarote = preenchidasCamarote.inc()

                else -> {}
            }
        } else {
            when (passagem.tipoVeiculo) {
                CARRO.name -> {
                    preenchidosVeiculos = preenchidosVeiculos.inc()
                    totalCarros = totalCarros.inc()
                }

                MOTO.name -> {
                    preenchidosVeiculos = preenchidosVeiculos.inc()
                    totalMotos = totalMotos.inc()
                }

                CAMINHAO.name -> {
                    preenchidosVeiculos = preenchidosVeiculos.inc()
                    totalCaminhoes = totalCaminhoes.inc()
                }

                CARRETA.name -> {
                    preenchidosVeiculos = preenchidosVeiculos.inc()
                    totalCarretas = totalCarretas.inc()
                }

                else -> {}
            }
        }
    }

    return DadosContagemPassagem(
        embarcacao = embarcacao.descricaoNome,
        preenchidasRedes = preenchidasRede.toString(),
        preenchidasInteiras = preenchidasInteiras.toString(),
        preenchidasMeias = preenchidasMeias.toString(),
        preenchidasGratuidades = preenchidasGratuidades.toString(),
        preenchidosVeiculo = preenchidosVeiculos.toString(),
        totalCarros = totalCarros.toString(),
        totalMotos = totalMotos.toString(),
        totalCaminhoes = totalCaminhoes.toString(),
        totalCarretas = totalCarretas.toString(),
        capacidadeVeiculos = embarcacao.capacidadeVeiculo.toString(),
        preenchidasSuitesGeral = preenchidasSuite.toString(),
        capacidadeSuitesGeral = (embarcacao.capacidadeSuite2 + embarcacao.capacidadeSuite3).toString(),
        preenchidasSuites2Pessoas = preenchidasSuite2Pessoas.toString(),
        capacidadeSuites2Pessoas = embarcacao.capacidadeSuite2.toString(),
        preenchidasSuites3Pessoas = preenchidasSuite3Pessoas.toString(),
        capacidadeSuites3Pessoas = embarcacao.capacidadeSuite3.toString(),
        preenchidosCamarotes = preenchidasCamarote.toString(),
        capacidadeCamarotes = embarcacao.capacidadeCamarote.toString()
    )
}