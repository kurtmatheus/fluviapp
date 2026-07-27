package dev.matheus.fluviapp.model.mappers

import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.CAMAROTE
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.CAMINHAO
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.CARRETA
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.CARRO
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.GRATUIDADE
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.INTEIRA
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.MEIA
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.MOTO
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.REDE
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.SUITE
import dev.matheus.fluviapp.model.passagem.Passagem
import dev.matheus.fluviapp.model.screendata.DadosContagemPassagem
import dev.matheus.fluviapp.model.viagem.Navio
import dev.matheus.fluviapp.services.repository.cadastro.viagem.NavioRepository
import dev.matheus.fluviapp.util.Mapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContagemPassagensMapper @Inject constructor(
    private val navioRepository: NavioRepository
) : Mapper<List<Passagem>, List<DadosContagemPassagem>> {

    override suspend fun map(entry: List<Passagem>): List<DadosContagemPassagem> {
        // Agrega pelo navioId CONGELADO na Passagem (ADR-0008 Fase 2): sem ida à Viagem viva, então
        // rename/reatribuição posterior não altera balanços históricos. obterTodos uma vez (não N+1);
        // associateBy dá lookup O(1) por id (era firstOrNull O(navios) por grupo).
        val naviosPorId = navioRepository.obterTodos().associateBy { it.id }

        return entry
            .groupBy { it.navioId }
            .mapNotNull { (navioId, passagens) ->
                // Navio removido → órfão detectável (lookup por id retorna null); descarta o grupo.
                val navio = naviosPorId[navioId] ?: return@mapNotNull null
                contarOcupacaoNavio(navio, passagens)
            }
    }
}

/**
 * Contagem pura da ocupação de um navio a partir das suas passagens — função testável sem repositório.
 *
 * Contagem **por bilhete/unidade** (decisão do analista, `balanco-passagens-mapper.md` §7): cada bilhete de
 * suíte ocupa **uma** suíte — `temPassageiro3` (trio; p3 ⇒ p2) cai no bucket de 3 pessoas, senão no de 2 (o
 * titular-solo conta). Rede/camarote/veículo contam 1 por bilhete. O breakdown inteira/meia/gratuidade vive
 * no ramo REDE porque o tipo tarifário só existe na rede (ADR-0013).
 */
internal fun contarOcupacaoNavio(
    navio: Navio,
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
            when (passagem.acomodacao) {
                REDE.name -> {
                    preenchidasRede = preenchidasRede.inc()

                    when (passagem.tipoPassagem) {
                        INTEIRA.name -> preenchidasInteiras = preenchidasInteiras.inc()
                        MEIA.name -> preenchidasMeias = preenchidasMeias.inc()
                        GRATUIDADE.name -> preenchidasGratuidades = preenchidasGratuidades.inc()
                        else -> {}
                    }
                }

                SUITE.name -> {
                    // Uma suíte por bilhete; trio (p3) → bucket 3 pessoas, senão 2 (inclui o titular-solo).
                    preenchidasSuite = preenchidasSuite.inc()
                    if (passagem.temPassageiro3) {
                        preenchidasSuite3Pessoas = preenchidasSuite3Pessoas.inc()
                    } else {
                        preenchidasSuite2Pessoas = preenchidasSuite2Pessoas.inc()
                    }
                }

                CAMAROTE.name -> preenchidasCamarote = preenchidasCamarote.inc()

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
        navio = navio.descricaoNome,
        preenchidasRedes = preenchidasRede.toString(),
        preenchidasInteiras = preenchidasInteiras.toString(),
        preenchidasMeias = preenchidasMeias.toString(),
        preenchidasGratuidades = preenchidasGratuidades.toString(),
        preenchidosVeiculo = preenchidosVeiculos.toString(),
        totalCarros = totalCarros.toString(),
        totalMotos = totalMotos.toString(),
        totalCaminhoes = totalCaminhoes.toString(),
        totalCarretas = totalCarretas.toString(),
        capacidadeVeiculos = navio.capacidadeVeiculo.toString(),
        preenchidasSuitesGeral = preenchidasSuite.toString(),
        capacidadeSuitesGeral = (navio.capacidadeSuite2 + navio.capacidadeSuite3).toString(),
        preenchidasSuites2Pessoas = preenchidasSuite2Pessoas.toString(),
        capacidadeSuites2Pessoas = navio.capacidadeSuite2.toString(),
        preenchidasSuites3Pessoas = preenchidasSuite3Pessoas.toString(),
        capacidadeSuites3Pessoas = navio.capacidadeSuite3.toString(),
        preenchidosCamarotes = preenchidasCamarote.toString(),
        capacidadeCamarotes = navio.capacidadeCamarote.toString()
    )
}