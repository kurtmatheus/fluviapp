package dev.matheus.fluviapp.services.repository.cadastro.rota

import com.google.firebase.firestore.FirebaseFirestore
import dev.matheus.fluviapp.di.module.SyncScope
import dev.matheus.fluviapp.domain.rota.Rota
import dev.matheus.fluviapp.services.repository.firebase.CodecFirestore
import dev.matheus.fluviapp.services.repository.firebase.ColecaoFirestore
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto
import dev.matheus.fluviapp.services.repository.firebase.FonteSnapshots
import dev.matheus.fluviapp.services.repository.firebase.documents.paraMapa
import dev.matheus.fluviapp.services.repository.firebase.documents.toRota
import dev.matheus.fluviapp.telemetry.RegistroCadastro
import dev.matheus.fluviapp.telemetry.RegistroSincronizacao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Porta do repositório de rotas (DIP) — o **pool compartilhado** do ADR-0016 §7.1.
 *
 * Duas ausências dizem o desenho melhor do que qualquer método presente:
 *
 * - **não há `editar`.** Rota é imutável: corrige-se criando outra e inativando esta. Uma porta com
 *   `salvar` genérico convidaria justamente o que a decisão proíbe;
 * - **não há `deletar`.** O descartado vira registro — é o que responde o que as passagens antigas
 *   significavam, e é matéria-prima da fase analítica.
 *
 * O que existe no lugar do segundo é [inativar], e o nome é literal: ela some dos seletores e continua
 * resolvendo o que aponta para ela.
 */
interface RotaRepository {
    fun sincronizar()
    fun observarTodas(): StateFlow<List<Rota>>

    /** Cria e devolve **o id**. Rota existente não é reescrita — ver a nota da porta. */
    suspend fun criar(rota: Rota): String

    suspend fun obterTodas(): List<Rota>
    suspend fun obterPorId(id: String): Rota?

    /** Tira de circulação sem apagar: o registro fica. */
    suspend fun inativar(id: String)

    companion object {
        const val COLLECTION_ROTAS = "rotas"
    }
}

/** O codec da Rota: o pouco que é dela na fronteira (ADR-0019 D2). */
private object RotaCodec : CodecFirestore<Rota> {
    override val colecao = RotaRepository.COLLECTION_ROTAS
    override val entidade = "rota"
    override fun deDocumento(bruto: DocumentoBruto) = bruto.toRota()
    override fun paraMapa(modelo: Rota) = modelo.paraMapa()
    override fun id(modelo: Rota) = modelo.id
    override fun comId(modelo: Rota, id: String) = modelo.copy(id = id)
}

/**
 * Impl Firestore da porta [RotaRepository] — sexta coleção sem Room (ADR-0017 D1), e a primeira do
 * **pool compartilhado**: sem dono, sem `empresaId`, legível por todos.
 */
@Singleton
class RotaFirestoreRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    registroCadastro: RegistroCadastro,
    @SyncScope syncScope: CoroutineScope,
    registroSincronizacao: RegistroSincronizacao,
    fonteSnapshots: FonteSnapshots,
) : RotaRepository {

    private val colecao = ColecaoFirestore(
        codec = RotaCodec,
        firestore = firestore,
        fonteSnapshots = fonteSnapshots,
        syncScope = syncScope,
        registroCadastro = registroCadastro,
        registroSincronizacao = registroSincronizacao,
    )

    override fun sincronizar() = colecao.sincronizar()

    override fun observarTodas(): StateFlow<List<Rota>> = colecao.observarTodos()

    override suspend fun criar(rota: Rota): String = colecao.salvar(rota.copy(id = ""))

    override suspend fun obterTodas(): List<Rota> = colecao.obterTodos()

    override suspend fun obterPorId(id: String): Rota? = colecao.obterPorId(id)

    /**
     * Inativa em vez de apagar — e aqui a razão é mais forte do que na Localidade e no Porto: a rota é
     * **compartilhada**, então apagá-la não quebraria só o histórico de quem a criou, mas o de quem nem
     * sabe que ela existiu.
     */
    override suspend fun inativar(id: String) {
        val rota = obterPorId(id) ?: return
        colecao.salvar(rota.copy(ativo = false))
    }
}