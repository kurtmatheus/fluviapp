package br.com.gruponaveg.model.mappers

import br.com.gruponaveg.model.cadastro.constantes.Constante.Categoria.MUNICIPIO
import br.com.gruponaveg.model.extrairPorDescricao
import br.com.gruponaveg.model.screendata.DadosViagemCard
import br.com.gruponaveg.model.viagem.Viagem
import br.com.gruponaveg.services.repository.cadastro.ConstanteRepository
import br.com.gruponaveg.services.repository.cadastro.viagem.EmpresaRepository
import br.com.gruponaveg.services.repository.cadastro.viagem.NavioRepository
import br.com.gruponaveg.util.Mapper
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ViagemDadosViagemMapper @Inject constructor(
    private val empresaRepository: EmpresaRepository,
    private val navioRepository: NavioRepository,
    private val constatesRepository: ConstanteRepository
) : Mapper<Viagem, DadosViagemCard> {
    override fun map(entry: Viagem): DadosViagemCard {
        val navio = runBlocking { navioRepository.obterPorNome(entry.navio) }
        val empresa = runBlocking { empresaRepository.obterPorNome(entry.empresa) }

        val listaMunicipios = runBlocking { constatesRepository.obterTodosPorCategoria(MUNICIPIO.name) }

        val origem = listaMunicipios.extrairPorDescricao(entry.origem)
        val destino = listaMunicipios.extrairPorDescricao(entry.destino)

        return DadosViagemCard(
            idViagem = entry.id,
            codigo = entry.codigo,
            empresa = empresa.nome,
            navio = navio.descricaoNome,
            origem = origem.descricaoNome,
            destino = destino.descricaoNome,
            capacidadeVeiculos = navio.capacidadeVeiculo.toString(),
            capacidadeSuites2Pessoas = navio.capacidadeSuite2.toString(),
            capacidadeSuites3Pessoas = navio.capacidadeSuite3.toString(),
            capacidadeSuites = (navio.capacidadeSuite2 + navio.capacidadeSuite3).toString(),
            capacidadeCamarotes = navio.capacidadeCamarote.toString()
        )
    }
}