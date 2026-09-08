package dev.matheus.fluviapp.services.repository.operacoes

import dev.matheus.fluviapp.domain.operacoes.ContextoUsuario
import dev.matheus.fluviapp.domain.operacoes.Perfil
import dev.matheus.fluviapp.domain.operacoes.Usuario
import dev.matheus.fluviapp.preferences.PerfilAtivo
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
 * A F6.4 tinha acrescentado uma terceira pergunta — *em nome de qual empresa se opera* —, e a [ADR-0032]
 * D5 a retirou: com uma empresa no máximo, o vínculo responde sozinho. No lugar dela entrou a que a D5
 * criou: **por qual dos dois perfis a pessoa está olhando o app** ([trocarPerfil]).
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
     * Guarda o perfil ativo — a resposta à troca no menu ([ADR-0032] D5).
     *
     * A porta **não valida** a troca: quem decide se ela vale é o domínio, a cada leitura
     * (`ContextoUsuario.perfilAtivo`). É o que faz um vínculo perdido devolver a pessoa ao perfil de
     * plataforma sozinho, sem depender de alguém lembrar de limpar a preferência.
     */
    suspend fun trocarPerfil(perfil: Perfil)

    /**
     * **Encerra a sessão local**: esquece quem entrou e por qual perfil olhava, e nada além disso.
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
 * Impl sobre a projeção local ([SessaoLocal]), o Firestore e o perfil ativo ([PerfilAtivo]).
 *
 * Chamava-se `SessaoUsuarioRoom`, e o próprio KDoc já dizia que o sufixo era do tempo em que as duas
 * leituras vinham do espelho. Em 2026-09-07 o usuário passou ao DataStore e o nome deixou de ter até esse
 * resto de verdade — o que a classe faz continua sendo o mesmo: juntar as três peças num contexto só.
 *
 * A terceira peça era a escolha **entre empresas** e passou a ser o **perfil ativo** ([ADR-0032] D5):
 * mesma mecânica, outro eixo.
 */
@Singleton
class SessaoUsuarioLocal @Inject constructor(
    private val sessaoLocal: SessaoLocal,
    private val funcionarioRepository: FuncionarioRepository,
    private val empresaRepository: EmpresaRepository,
    private val perfilAtivo: PerfilAtivo,
) : SessaoUsuario {

    /**
     * As duas metades locais (quem entrou · por qual perfil olha) combinadas, e o contexto remontado a
     * cada mudança.
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
            perfilAtivo.observarPerfil(),
        ) { usuario, perfil -> usuario to perfil }
            .distinctUntilChanged()
            .map { (usuario, perfil) -> usuario?.let { montar(it, perfil) } }

    private suspend fun montar(usuario: Usuario, perfilEscolhido: Perfil?): ContextoUsuario {
        val funcionario = usuario.funcionarioId
            .takeIf { it.isNotBlank() }
            ?.let { funcionarioRepository.obterPorId(it) }

        val contexto = ContextoUsuario(
            usuario = usuario,
            funcionario = funcionario,
            perfilEscolhido = perfilEscolhido,
        )

        // O nome da empresa é resolvido **aqui**, e só quando há vínculo: é o bilhete que precisa dele
        // (gente lê nome, não id), e uma leitura a mais por sessão é mais barata do que cada tela que
        // imprime algo repetir a consulta.
        //
        // A leitura acompanha o **vínculo**, e não a lente ([ADR-0032] D5): o menu de quem está sob o
        // perfil de plataforma precisa nomear a empresa para oferecer a troca. Quem imprime bilhete lê
        // `contexto.agencia`, que aplica a lente.
        val empresa = funcionario?.vinculo?.let { empresaRepository.obterPorId(it.empresaId) }
        return contexto.copy(empresaDoVinculo = empresa?.nome.orEmpty())
    }

    override suspend fun trocarPerfil(perfil: Perfil) = perfilAtivo.guardar(perfil)

    override suspend fun encerrar() {
        sessaoLocal.limpar()
        perfilAtivo.limpar()
    }
}