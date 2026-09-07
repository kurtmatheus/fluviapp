package dev.matheus.fluviapp.services.repository.operacoes

import dev.matheus.fluviapp.domain.operacoes.ContextoUsuario
import dev.matheus.fluviapp.domain.operacoes.Usuario
import dev.matheus.fluviapp.preferences.EscolhaDeVinculo
import dev.matheus.fluviapp.preferences.SessaoLocal
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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
 * Desde a F6.4 ela responde uma terceira coisa: **em nome de quem** a pessoa opera, quando há mais de um
 * vínculo (ADR-0016 §6).
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
     * Guarda a empresa escolhida — a resposta à seleção de contexto.
     *
     * A porta **não valida** a escolha: quem decide se ela vale é o domínio, a cada leitura
     * (`ContextoUsuario.vinculoAtivo`). É o que faz um vínculo perdido invalidar a escolha sozinho, sem
     * depender de alguém lembrar de limpá-la.
     */
    suspend fun escolherEmpresa(empresaId: String)

    /**
     * **Encerra a sessão local**: esquece quem entrou e a escolha de contexto, e nada além disso.
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
 * Impl sobre a projeção local ([SessaoLocal]), o Firestore e a escolha persistida ([EscolhaDeVinculo]).
 *
 * Chamava-se `SessaoUsuarioRoom`, e o próprio KDoc já dizia que o sufixo era do tempo em que as duas
 * leituras vinham do espelho. Em 2026-09-07 o usuário passou ao DataStore e o nome deixou de ter até esse
 * resto de verdade — o que a classe faz continua sendo o mesmo: juntar as três peças num contexto só.
 */
@Singleton
class SessaoUsuarioLocal @Inject constructor(
    private val sessaoLocal: SessaoLocal,
    private val funcionarioRepository: FuncionarioRepository,
    private val empresaRepository: EmpresaRepository,
    private val escolhaDeVinculo: EscolhaDeVinculo,
) : SessaoUsuario {

    /**
     * As duas metades locais (quem entrou · o que escolheu) combinadas, e o contexto remontado a cada
     * mudança.
     *
     * O `distinctUntilChanged` não é otimização de estilo: as duas vêm do **mesmo** DataStore, então uma
     * gravação faria os dois fluxos emitirem e o `combine` produzir o par duas vezes. Cortar a repetição
     * aqui é o que impede a montagem de rodar em dobro.
     *
     * Remontar é barato: as duas leituras abaixo vão à **coleção em memória** (`ColecaoFirestore` guarda o
     * snapshot num `StateFlow`), não à rede.
     */
    override fun observar(): Flow<ContextoUsuario?> =
        combine(
            sessaoLocal.observarLogado(),
            escolhaDeVinculo.observarEmpresaEscolhida(),
        ) { usuario, empresaEscolhida -> usuario to empresaEscolhida }
            .distinctUntilChanged()
            .map { (usuario, empresaEscolhida) ->
                usuario?.let { montar(it, empresaEscolhida) }
            }

    private suspend fun montar(usuario: Usuario, empresaEscolhida: String?): ContextoUsuario {
        val funcionario = usuario.funcionarioId
            .takeIf { it.isNotBlank() }
            ?.let { funcionarioRepository.obterPorId(it) }

        val contexto = ContextoUsuario(
            usuario = usuario,
            funcionario = funcionario,
            empresaAtivaId = empresaEscolhida,
        )

        // O nome da empresa é resolvido **aqui**, e só quando há vínculo em vigor: é o bilhete que
        // precisa dele (gente lê nome, não id), e uma leitura a mais por sessão é mais barata do que
        // cada tela que imprime algo repetir a consulta.
        val empresaAtiva = contexto.vinculoAtivo?.let { empresaRepository.obterPorId(it.empresaId) }
        return contexto.copy(empresaAtivaNome = empresaAtiva?.nome.orEmpty())
    }

    override suspend fun escolherEmpresa(empresaId: String) = escolhaDeVinculo.guardar(empresaId)

    override suspend fun encerrar() {
        sessaoLocal.limpar()
        escolhaDeVinculo.limpar()
    }
}