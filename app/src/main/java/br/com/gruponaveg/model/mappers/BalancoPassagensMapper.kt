package br.com.gruponaveg.model.mappers

import br.com.gruponaveg.model.cadastro.constantes.Constante.Descricao.CAMAROTE
import br.com.gruponaveg.model.cadastro.constantes.Constante.Descricao.CAMINHAO
import br.com.gruponaveg.model.cadastro.constantes.Constante.Descricao.CARRO
import br.com.gruponaveg.model.cadastro.constantes.Constante.Descricao.GRATUIDADE
import br.com.gruponaveg.model.cadastro.constantes.Constante.Descricao.INTEIRA
import br.com.gruponaveg.model.cadastro.constantes.Constante.Descricao.MEIA
import br.com.gruponaveg.model.cadastro.constantes.Constante.Descricao.MOTO
import br.com.gruponaveg.model.cadastro.constantes.Constante.Descricao.REDE
import br.com.gruponaveg.model.cadastro.constantes.Constante.Descricao.SUITE
import br.com.gruponaveg.model.extrairPorDescricao
import br.com.gruponaveg.model.passagem.Passagem
import br.com.gruponaveg.model.screendata.DadosBalancoPassagem
import br.com.gruponaveg.model.viagem.Navio
import br.com.gruponaveg.services.repository.cadastro.viagem.NavioRepository
import br.com.gruponaveg.services.repository.firebase.ViagemFirestoreRepository
import br.com.gruponaveg.util.Mapper
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BalancoPassagensMapper @Inject constructor(
    private val viagemRepository: ViagemFirestoreRepository,
    private val navioRepository: NavioRepository
) : Mapper<List<Passagem>, List<DadosBalancoPassagem>> {

    override fun map(entry: List<Passagem>): List<DadosBalancoPassagem> {
        val mapViagem = entry.groupBy {
            val viagem = runBlocking { viagemRepository.obterPorCodigo(it.codigoViagem) }
            val navio = runBlocking { navioRepository.obterTodos() }.extrairPorDescricao(viagem.navio)
            navio
        }

        return mapViagem.map {
            contador(it.key as Navio, it.value)
        }
    }

    private fun contador(
        navio: Navio,
        listaPassagem: List<Passagem>,
    ): DadosBalancoPassagem {
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

        listaPassagem.forEach { passagem ->
            if (!passagem.ehVeiculo) {
                when (passagem.acomodacao) {
                    REDE.name -> {
                        preenchidasRede = preenchidasRede.inc()

                        when (passagem.tipoPassagem) {
                            INTEIRA.name -> {
                                preenchidasInteiras = preenchidasInteiras.inc()
                            }

                            MEIA.name -> {
                                preenchidasMeias = preenchidasMeias.inc()
                            }

                            GRATUIDADE.name -> {
                                preenchidasGratuidades = preenchidasGratuidades.inc()
                            }

                            else -> {}
                        }
                    }

                    SUITE.name -> {
                        if (passagem.temPassageiro2) {
                            preenchidasSuite = preenchidasSuite.inc()
                            preenchidasSuite2Pessoas = preenchidasSuite2Pessoas.inc()
                        }

                        if (passagem.temPassageiro3) {
                            preenchidasSuite = preenchidasSuite.inc()
                            preenchidasSuite3Pessoas = preenchidasSuite3Pessoas.inc()
                        }
                    }

                    CAMAROTE.name -> {
                        preenchidasCamarote = preenchidasCamarote.inc()
                    }

                    else -> {}
                }
            } else {
                val tipoVeiculo = passagem.tipoVeiculo
                when (tipoVeiculo) {
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

                    else -> {}
                }
            }
        }

        return DadosBalancoPassagem(
            navio = navio.descricaoNome,
            preenchidasRedes = preenchidasRede.toString(),
            preenchidasInteiras = preenchidasInteiras.toString(),
            preenchidasMeias = preenchidasMeias.toString(),
            preenchidasGratuidades = preenchidasGratuidades.toString(),
            preenchidosVeiculo = preenchidosVeiculos.toString(),
            totalCarros = totalCarros.toString(),
            totalMotos = totalMotos.toString(),
            totalCaminhoes = totalCaminhoes.toString(),
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
}