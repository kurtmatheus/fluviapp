package dev.matheus.fluviapp.model.mappers

import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Categoria.MUNICIPIO
import dev.matheus.fluviapp.model.extrairPorDescricao
import dev.matheus.fluviapp.model.screendata.DadosViagemCard
import dev.matheus.fluviapp.model.viagem.Viagem
import dev.matheus.fluviapp.services.repository.cadastro.ConstanteRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.NavioRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monta o card de viagem a partir das fontes (empresa/navio/municípios). `map` é **suspenso** —
 * sem `runBlocking` (as leituras rodam no coroutine do chamador). Não implementa mais `Mapper`
 * porque a interface é síncrona; é usado concretamente.
 *
 * Nota: faz N+1 (busca as fontes por viagem) — otimização futura, mas agora não-bloqueante.
 */
@Singleton
class ViagemDadosViagemMapper @Inject constructor(
    private val empresaRepository: EmpresaRepository,
    private val navioRepository: NavioRepository,
    private val constanteRepository: ConstanteRepository,
) {
    suspend fun map(entry: Viagem): DadosViagemCard {
        // Resolve pelo id estável (ADR-0008) — rename-safe. Fallback ao nome-substrato guardado na
        // própria Viagem se o id não resolver (defensivo).
        val navio = navioRepository.obterPorId(entry.navioId)
        val empresa = empresaRepository.obterPorId(entry.empresaId)
        val listaMunicipios = constanteRepository.obterTodosPorCategoria(MUNICIPIO.name)

        val origem = listaMunicipios.extrairPorDescricao(entry.origem)
        val destino = listaMunicipios.extrairPorDescricao(entry.destino)

        return DadosViagemCard(
            idViagem = entry.id,
            codigo = entry.codigo,
            empresa = empresa?.nome ?: entry.empresa,
            navio = navio?.descricaoNome ?: entry.navio,
            origem = origem.descricaoNome,
            destino = destino.descricaoNome,
            capacidadeVeiculos = (navio?.capacidadeVeiculo ?: 0).toString(),
            capacidadeSuites2Pessoas = (navio?.capacidadeSuite2 ?: 0).toString(),
            capacidadeSuites3Pessoas = (navio?.capacidadeSuite3 ?: 0).toString(),
            capacidadeSuites = ((navio?.capacidadeSuite2 ?: 0) + (navio?.capacidadeSuite3 ?: 0)).toString(),
            capacidadeCamarotes = (navio?.capacidadeCamarote ?: 0).toString()
        )
    }
}
