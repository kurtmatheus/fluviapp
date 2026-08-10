package dev.matheus.fluviapp.services.repository.operacoes

import dev.matheus.fluviapp.domain.operacoes.Atuacao
import dev.matheus.fluviapp.domain.viagem.EscopoDoPool
import dev.matheus.fluviapp.domain.viagem.de
import dev.matheus.fluviapp.domain.viagem.escopoDoPool
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Porta fina (DIP) que responde **quanto do pool quem está operando enxerga**.
 *
 * Ela existe pelo mesmo motivo da [SessaoUsuario], um nível acima: o caminho
 * `contexto → vínculo ativo → empresa → atuação → escopo` tem quatro saltos, e ele é o mesmo em toda tela
 * que lista rota, viagem — e, adiante, o que a emissão oferece. Repetido em cada ViewModel, seria quatro
 * lugares para uma regra divergir; aqui é um, e é fakeável.
 *
 * A separação de responsabilidades é a razão de não ser um método da `SessaoUsuario`: aquela responde
 * **quem** é a pessoa, esta responde **o que lhe foi concedido**. Identidade e concessão mudam por razões
 * diferentes e em telas diferentes.
 */
interface EscopoDaSessao {
    suspend fun atual(): EscopoDoPool
}

/**
 * Impl sobre a sessão + as atuações da empresa ativa.
 *
 * Lê a atuação de **agenciamento**, e só ela: é a única que vende travessia (ADR-0016 §4). Uma empresa que
 * só transporta não oferta viagem — e o `null` daqui vira [EscopoDoPool.Nenhum], que é o fail-closed
 * correto: não é "vê tudo", é "não tem nada a ver".
 */
@Singleton
class EscopoDaSessaoPadrao @Inject constructor(
    private val sessaoUsuario: SessaoUsuario,
    private val empresaRepository: EmpresaRepository,
) : EscopoDaSessao {

    override suspend fun atual(): EscopoDoPool {
        val contexto = sessaoUsuario.atual()
        val empresaId = contexto?.vinculoAtivo?.empresaId

        // A leitura da atuação só acontece quando há vínculo: para quem administra a plataforma ela seria
        // uma ida ao Firestore cujo resultado o `escopoDoPool` descarta.
        val atuacao = empresaId
            ?.let { empresaRepository.obterAtuacoes(it) }
            ?.de(Atuacao.AGENCIAMENTO)

        return escopoDoPool(contexto?.papel, atuacao)
    }
}