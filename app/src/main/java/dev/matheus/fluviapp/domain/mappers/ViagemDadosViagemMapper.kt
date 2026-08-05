package dev.matheus.fluviapp.domain.mappers

import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Categoria.MUNICIPIO
import dev.matheus.fluviapp.domain.extrairPorDescricao
import dev.matheus.fluviapp.domain.screendata.DadosViagemCard
import dev.matheus.fluviapp.domain.viagem.Viagem
import dev.matheus.fluviapp.services.repository.cadastro.ConstanteRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmbarcacaoRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monta o card de viagem a partir das fontes (empresa/embarcacao/municípios). `map` é **suspenso** —
 * sem `runBlocking` (as leituras rodam no coroutine do chamador). Não implementa mais `Mapper`
 * porque a interface é síncrona; é usado concretamente.
 *
 * Nota: faz N+1 (busca as fontes por viagem) — otimização futura, mas agora não-bloqueante.
 */
@Singleton
class ViagemDadosViagemMapper @Inject constructor(
    private val empresaRepository: EmpresaRepository,
    private val embarcacaoRepository: EmbarcacaoRepository,
    private val constanteRepository: ConstanteRepository,
) {
    suspend fun map(entry: Viagem): DadosViagemCard {
        // Resolve empresa/embarcacao pelo id estável (ADR-0008) — rename-safe. A Viagem não guarda mais os
        // nomes (Fase 3), então id não resolvido → vazio.
        val embarcacao = embarcacaoRepository.obterPorId(entry.embarcacaoId)
        val empresa = empresaRepository.obterPorId(entry.empresaId)
        val listaMunicipios = constanteRepository.obterTodosPorCategoria(MUNICIPIO.name)

        val origem = listaMunicipios.extrairPorDescricao(entry.origem)
        val destino = listaMunicipios.extrairPorDescricao(entry.destino)

        return DadosViagemCard(
            idViagem = entry.id,
            codigo = entry.codigo,
            empresa = empresa?.nome.orEmpty(),
            embarcacao = embarcacao?.descricaoNome.orEmpty(),
            origem = origem.descricaoNome,
            destino = destino.descricaoNome,
            capacidadeVeiculos = (embarcacao?.capacidadeVeiculo ?: 0).toString(),
            capacidadeSuites2Pessoas = (embarcacao?.capacidadeSuite2 ?: 0).toString(),
            capacidadeSuites3Pessoas = (embarcacao?.capacidadeSuite3 ?: 0).toString(),
            capacidadeSuites = ((embarcacao?.capacidadeSuite2 ?: 0) + (embarcacao?.capacidadeSuite3 ?: 0)).toString(),
            capacidadeCamarotes = (embarcacao?.capacidadeCamarote ?: 0).toString()
        )
    }
}
