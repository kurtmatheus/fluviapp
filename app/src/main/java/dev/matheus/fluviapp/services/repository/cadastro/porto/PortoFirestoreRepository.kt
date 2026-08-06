package dev.matheus.fluviapp.services.repository.cadastro.porto

import com.google.firebase.firestore.FirebaseFirestore
import dev.matheus.fluviapp.di.module.SyncScope
import dev.matheus.fluviapp.domain.porto.Porto
import dev.matheus.fluviapp.services.repository.firebase.CodecFirestore
import dev.matheus.fluviapp.services.repository.firebase.ColecaoFirestore
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto
import dev.matheus.fluviapp.services.repository.firebase.FonteSnapshots
import dev.matheus.fluviapp.services.repository.firebase.documents.paraMapa
import dev.matheus.fluviapp.services.repository.firebase.documents.toPorto
import dev.matheus.fluviapp.telemetry.RegistroCadastro
import dev.matheus.fluviapp.telemetry.RegistroSincronizacao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** O codec do Porto: o pouco que é dele na fronteira (ADR-0019 D2). */
private object PortoCodec : CodecFirestore<Porto> {
    override val colecao = "portos"
    override val entidade = "porto"
    override fun deDocumento(bruto: DocumentoBruto) = bruto.toPorto()
    override fun paraMapa(modelo: Porto) = modelo.paraMapa()
    override fun id(modelo: Porto) = modelo.id
    override fun comId(modelo: Porto, id: String) = modelo.copy(id = id)
}

/**
 * Impl Firestore da porta [PortoRepository] — quarta coleção sem Room (ADR-0017 D1, ADR-0016 F4).
 *
 * O CRUD comum é composto da [ColecaoFirestore]. **Só o `deletar` é próprio**, pela mesma razão da
 * Localidade: delete lógico não é uma variação de apagar, é outra operação — uma escrita.
 */
@Singleton
class PortoFirestoreRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    registroCadastro: RegistroCadastro,
    @SyncScope syncScope: CoroutineScope,
    registroSincronizacao: RegistroSincronizacao,
    fonteSnapshots: FonteSnapshots,
) : PortoRepository {

    private val colecao = ColecaoFirestore(
        codec = PortoCodec,
        firestore = firestore,
        fonteSnapshots = fonteSnapshots,
        syncScope = syncScope,
        registroCadastro = registroCadastro,
        registroSincronizacao = registroSincronizacao,
    )

    override fun observarTodos(): StateFlow<List<Porto>> = colecao.observarTodos()

    override fun sincronizar() = colecao.sincronizar()

    override suspend fun obterTodos(): List<Porto> = colecao.obterTodos()

    override suspend fun obterPorId(id: String): Porto? = colecao.obterPorId(id)

    override suspend fun salvar(porto: Porto): String = colecao.salvar(porto)

    /**
     * Inativa em vez de apagar: o porto é referenciado pela rota e pela concessão (ADR-0016 §5/§7).
     * Apagar o documento deixaria as duas apontando para o nada — e nenhuma regra de aplicativo recupera
     * um documento que já não existe.
     *
     * Reusa o `salvar` de propósito: é uma escrita do agregado inteiro, com o listener emitindo o novo
     * estado como em qualquer edição. Id que não existe simplesmente não vira nada.
     */
    override suspend fun deletar(id: String) {
        val porto = obterPorId(id) ?: return
        colecao.salvar(porto.copy(ativo = false))
    }
}