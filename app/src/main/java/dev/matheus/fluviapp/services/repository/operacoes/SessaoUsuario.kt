package dev.matheus.fluviapp.services.repository.operacoes

import dev.matheus.fluviapp.domain.operacoes.ContextoUsuario
import dev.matheus.fluviapp.domain.operacoes.Usuario
import dev.matheus.fluviapp.preferences.SessaoLocal
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Porta fina (DIP) que responde **quem está operando** — os dois contextos resolvidos de uma vez
 * (ADR-0015 §8.1). Os ViewModels dependem desta interface, não do par de repositórios: assim o recorte
 * por papel/cargo/empresa é testável com um fake, e o caminho `usuário → funcionarioId → funcionário`
 * existe em **um** lugar em vez de repetido em cada tela.
 *
 * A F6.4 tinha acrescentado uma terceira pergunta — *em nome de qual empresa se opera* —, e a [ADR-0032]
 * D5 a retirou: com uma empresa no máximo, o vínculo responde sozinho. `escolherEmpresa` saiu com ela.
 */
interface SessaoUsuario {
    /**
     * **O contexto como estado observável** ([ADR-0032] D2) — quem entrou, quem saiu, e o que muda no meio.
     *
     * É a forma principal da porta desde 2026-09-07. Antes ela só sabia responder uma vez, e o menu, que
     * precisa reagir, lia as chaves do DataStore **por fora dela** — não por desleixo, mas porque era a
     * única saída. Com o fluxo, o motivo acabou.
     *
     * `null` enquanto não há ninguém logado.
     */
    fun observar(): Flow<ContextoUsuario?>

    /**
     * Foto do contexto agora — o atalho de quem **age uma vez** (emitir, salvar, conferir permissão) e não
     * quer assinar nada.
     *
     * Definida em termos de [observar] de propósito: uma implementação só, duas formas de consumir. Se
     * fossem dois caminhos independentes, a diferença entre eles seria descoberta por defeito.
     */
    suspend fun atual(): ContextoUsuario? = observar().first()

    /**
     * **Encerra a sessão local**: esquece quem entrou, e nada além disso.
     *
     * Existe para o logout parar de conhecer *onde* a sessão mora. Antes ele limpava duas coisas por dois
     * caminhos — `sessaoLocal.limpar()` e um `dataStore.edit` cru —, e um ViewModel que sabe quais chaves
     * existem é um ViewModel que precisa ser corrigido quando uma chave nasce.
     *
     * O **e-mail do último login não sai**: é conveniência do próximo acesso, não sessão.
     */
    suspend fun encerrar()
}

/**
 * Impl sobre a projeção local ([SessaoLocal]) e o Firestore.
 *
 * Chamava-se `SessaoUsuarioRoom`, e o próprio KDoc já dizia que o sufixo era do tempo em que as duas
 * leituras vinham do espelho. Em 2026-09-07 o usuário passou ao DataStore e o nome deixou de ter até esse
 * resto de verdade — o que a classe faz continua sendo o mesmo: juntar as peças num contexto só.
 *
 * Eram **três** peças até a [ADR-0032] D5; a escolha de empresa era a terceira, e saiu com o caso que a
 * exigia. O `combine` de dois fluxos do mesmo DataStore (e o `distinctUntilChanged` que existia só para
 * cortar a emissão em dobro que ele produzia) saíram junto — não por arrumação, mas porque sobrou um
 * fluxo, e um fluxo não se combina com nada.
 */
@Singleton
class SessaoUsuarioLocal @Inject constructor(
    private val sessaoLocal: SessaoLocal,
    private val funcionarioRepository: FuncionarioRepository,
    private val empresaRepository: EmpresaRepository,
) : SessaoUsuario {

    /**
     * Quem entrou, e o contexto remontado a cada mudança.
     *
     * Remontar é barato: as duas leituras abaixo vão à **coleção em memória** (`ColecaoFirestore` guarda o
     * snapshot num `StateFlow`), não à rede.
     */
    override fun observar(): Flow<ContextoUsuario?> =
        sessaoLocal.observarLogado().map { usuario -> usuario?.let { montar(it) } }

    private suspend fun montar(usuario: Usuario): ContextoUsuario {
        val funcionario = usuario.funcionarioId
            .takeIf { it.isNotBlank() }
            ?.let { funcionarioRepository.obterPorId(it) }

        val contexto = ContextoUsuario(usuario = usuario, funcionario = funcionario)

        // O nome da empresa é resolvido **aqui**, e só quando há vínculo: é o bilhete que precisa dele
        // (gente lê nome, não id), e uma leitura a mais por sessão é mais barata do que cada tela que
        // imprime algo repetir a consulta.
        val empresaAtiva = contexto.vinculoAtivo?.let { empresaRepository.obterPorId(it.empresaId) }
        return contexto.copy(empresaAtivaNome = empresaAtiva?.nome.orEmpty())
    }

    override suspend fun encerrar() = sessaoLocal.limpar()
}