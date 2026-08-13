package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem

import dev.matheus.fluviapp.domain.passagem.Passagem
import dev.matheus.fluviapp.domain.passagem.PassagemDePassageiro
import dev.matheus.fluviapp.domain.passagem.PassagemDeVeiculo
import dev.matheus.fluviapp.services.repository.cadastro.localidade.LocalidadeRepository
import dev.matheus.fluviapp.services.repository.cadastro.porto.PortoRepository
import dev.matheus.fluviapp.services.repository.cadastro.rota.RotaRepository
import dev.matheus.fluviapp.domain.viagem.OcorrenciaViagem
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmbarcacaoRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.ViagemRepository
import dev.matheus.fluviapp.services.repository.pool.ClienteRepository
import dev.matheus.fluviapp.services.repository.pool.VeiculoRepository
import dev.matheus.fluviapp.ui.viewmodel.helpers.inicio.rotuloCom
import javax.inject.Inject

/**
 * **A outra metade da junção: quem carrega** ([ADR-0025] D3).
 *
 * A tradução ([paraConferencia]) é pura; o I/O que ela pressupõe mora aqui, em um lugar só e à vista. É o
 * híbrido do `ContagemPassagensMapper` — repositório envolvendo função pura —, agora como regra.
 *
 * ### Dois regimes, e a diferença é de tamanho
 *
 * | O que se junta | Como | Por quê |
 * |---|---|---|
 * | viagem, rota, porto, localidade | **lookup sobre a coleção inteira**, que a sessão já mantém | coleção pequena, útil inteira |
 * | cliente, veículo | **leitura por ids, em lote** | pool cresce sem limite; pela razão do [ADR-0024] D9 não se observa inteiro |
 *
 * A correção que o estudo da camada fez ao ADR-0024 está aqui em código: *"resolver que porto é este id é
 * lookup em memória"* vale para as seis coleções pequenas, e **não vale** para os pools.
 *
 * ### Por que é uma classe, e não seis dependências no ViewModel
 *
 * Porque o ViewModel que junta não é um só — o embarque hoje, o bilhete e a lista adiante —, e cada um deles
 * teria de repetir as mesmas seis injeções e a mesma montagem de rótulo. O que ele recebe passa a ser **um**
 * colaborador com um método, e o teste dele troca esse colaborador por um fake em vez de seis.
 */
class ColetorDeReferencias @Inject constructor(
    private val clienteRepository: ClienteRepository,
    private val veiculoRepository: VeiculoRepository,
    private val viagemRepository: ViagemRepository,
    private val rotaRepository: RotaRepository,
    private val portoRepository: PortoRepository,
    private val localidadeRepository: LocalidadeRepository,
    private val embarcacaoRepository: EmbarcacaoRepository,
) {

    /**
     * **Só a travessia**: viagem, rota e portos. Nenhum dado pessoal é lido.
     *
     * É o que a conferência de embarque pede, e o método existe separado justamente para que *não ler PII*
     * seja uma escolha visível na chamada — e não um efeito de qual campo alguém deixou de usar depois.
     * *"O embarque confere bilhete e não pessoa"* (analista, 2026-08-13).
     */
    suspend fun daTravessia(passagem: Passagem): ReferenciasDaPassagem = daOcorrencia(passagem.ocorrencia)

    /**
     * A travessia a partir da **ocorrência**, sem passagem nenhuma — é o que a emissão precisa para montar o
     * cabeçalho antes de existir bilhete ([ADR-0028] D5).
     */
    suspend fun daOcorrencia(ocorrencia: OcorrenciaViagem): ReferenciasDaPassagem {
        val viagem = viagemRepository.obterPorId(ocorrencia.viagemId)
        val rota = viagem?.rotaId?.let { rotaRepository.obterPorId(it) }

        val portosPorId = if (rota == null) {
            emptyMap()
        } else {
            val localidades = localidadeRepository.obterTodas().associate { it.id to it.rotulo }
            portoRepository.obterTodos()
                .filter { it.id == rota.portoOrigemId || it.id == rota.portoDestinoId }
                .associate { it.id to it.rotuloCom(localidades) }
        }

        return ReferenciasDaPassagem(
            viagem = viagem,
            rota = rota,
            portosPorId = portosPorId,
            embarcacao = viagem?.embarcacaoId?.let { id ->
                embarcacaoRepository.obterTodos().firstOrNull { it.id == id }?.descricaoNome
            },
        )
    }

    /**
     * A travessia **mais os participantes** — para quem legitimamente precisa deles: o bilhete digital, que é
     * entregue a quem comprou, e a listagem da própria agência.
     *
     * O `when` sobre a categoria não é economia miúda: uma passagem de veículo não tem cliente a buscar, e ir
     * ao pool de pessoas para não achar nada seria uma leitura de dado pessoal paga por bilhete — do tipo que
     * fica invisível justamente porque ninguém a lê no código.
     *
     * Lembrete de quem chamar isto: o pool é recortado pela assinatura, então o que **esta** agência não
     * atendeu simplesmente não volta. Não é falha de carregamento; é a regra (ADR-0018 D3).
     */
    suspend fun completas(passagem: Passagem): ReferenciasDaPassagem = daTravessia(passagem).copy(
        clientesPorId = when (passagem) {
            is PassagemDePassageiro -> clienteRepository.obterPorIds(passagem.clientes).associateBy { it.id }
            is PassagemDeVeiculo -> passagem.responsavelRetirada
                ?.let { clienteRepository.obterPorIds(listOf(it)).associateBy { cliente -> cliente.id } }
                .orEmpty()
        },
        veiculosPorId = when (passagem) {
            is PassagemDeVeiculo -> veiculoRepository.obterPorIds(listOf(passagem.veiculoId))
                .associateBy { it.id }

            is PassagemDePassageiro -> emptyMap()
        },
    )
}