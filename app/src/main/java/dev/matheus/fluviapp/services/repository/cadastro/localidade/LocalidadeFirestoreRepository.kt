package dev.matheus.fluviapp.services.repository.cadastro.localidade

import dev.matheus.fluviapp.di.module.SyncScope
import dev.matheus.fluviapp.domain.localidade.Localidade
import dev.matheus.fluviapp.services.repository.firebase.CodecFirestore
import dev.matheus.fluviapp.services.repository.firebase.ColecaoFirestore
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto
import dev.matheus.fluviapp.services.repository.firebase.FonteSnapshots
import dev.matheus.fluviapp.services.repository.firebase.documents.paraMapa
import dev.matheus.fluviapp.services.repository.firebase.documents.toLocalidade
import dev.matheus.fluviapp.telemetry.RegistroCadastro
import dev.matheus.fluviapp.telemetry.RegistroSincronizacao
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** O codec da Localidade: o pouco que é dela na fronteira (ADR-0019 D2). */
private object LocalidadeCodec : CodecFirestore<Localidade> {
    override val colecao = "localidades"
    override val entidade = "localidade"
    override fun deDocumento(bruto: DocumentoBruto) = bruto.toLocalidade()
    override fun paraMapa(modelo: Localidade) = modelo.paraMapa()
    override fun id(modelo: Localidade) = modelo.id
    override fun comId(modelo: Localidade, id: String) = modelo.copy(id = id)
}

/**
 * Impl Firestore da porta [LocalidadeRepository] — **terceira coleção sem Room**, e a primeira que nasce
 * sem nunca ter tido espelho (ADR-0017 D1, ADR-0016 F4).
 *
 * O CRUD comum é composto da [ColecaoFirestore]. **Só o `deletar` é próprio**, e é por isso que ele está
 * escrito aqui em vez de virar opção da classe genérica: delete lógico não é uma variação de apagar, é
 * outra operação — uma escrita. Empurrá-la para a `ColecaoFirestore` obrigaria toda coleção a carregar um
 * conceito de `ativo` que Empresa e Embarcação não têm.
 */
@Singleton
class LocalidadeFirestoreRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    registroCadastro: RegistroCadastro,
    @SyncScope syncScope: CoroutineScope,
    registroSincronizacao: RegistroSincronizacao,
    fonteSnapshots: FonteSnapshots,
) : LocalidadeRepository {

    private val colecao = ColecaoFirestore(
        codec = LocalidadeCodec,
        firestore = firestore,
        fonteSnapshots = fonteSnapshots,
        syncScope = syncScope,
        registroCadastro = registroCadastro,
        registroSincronizacao = registroSincronizacao,
    )

    override fun observarTodas(): StateFlow<List<Localidade>> = colecao.observarTodos()

    override fun sincronizar() = colecao.sincronizar()

    override suspend fun obterTodas(): List<Localidade> = colecao.obterTodos()

    override suspend fun obterPorId(id: String): Localidade? = colecao.obterPorId(id)

    override suspend fun salvar(localidade: Localidade): String = colecao.salvar(localidade)

    /**
     * Inativa em vez de apagar (decisão do analista): a localidade é **referenciada** pelo porto, e o
     * porto pela rota. Apagar o documento deixaria histórico apontando para o nada — o relatório antigo
     * passaria a exibir um vazio onde havia uma cidade.
     *
     * Reusa o `salvar` de propósito: é uma escrita do agregado inteiro, com o listener emitindo o novo
     * estado como em qualquer edição. Id que não existe simplesmente não vira nada — não se inventa uma
     * localidade inativa a partir de um id solto.
     */
    override suspend fun deletar(id: String) {
        val localidade = obterPorId(id) ?: return
        colecao.salvar(localidade.copy(ativo = false))
    }
}