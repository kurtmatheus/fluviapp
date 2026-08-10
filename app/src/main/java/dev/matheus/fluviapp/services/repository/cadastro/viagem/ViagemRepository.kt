package dev.matheus.fluviapp.services.repository.cadastro.viagem

import com.google.firebase.firestore.FirebaseFirestore
import dev.matheus.fluviapp.di.module.SyncScope
import dev.matheus.fluviapp.domain.viagem.Viagem
import dev.matheus.fluviapp.services.repository.firebase.CodecFirestore
import dev.matheus.fluviapp.services.repository.firebase.ColecaoFirestore
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto
import dev.matheus.fluviapp.services.repository.firebase.FonteSnapshots
import dev.matheus.fluviapp.services.repository.firebase.documents.paraMapa
import dev.matheus.fluviapp.services.repository.firebase.documents.toViagem
import dev.matheus.fluviapp.telemetry.RegistroCadastro
import dev.matheus.fluviapp.telemetry.RegistroSincronizacao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Porta do repositório de viagens (DIP) — o segundo habitante do **pool compartilhado** (ADR-0016 §7.1).
 *
 * A porta tem a mesma forma da Rota, e as mesmas duas ausências: **não há `editar`** e **não há
 * `deletar`**. Na viagem elas pesam mais, porque é para ela que a passagem aponta — reescrever o horário
 * mudaria a hora impressa em bilhetes de terceiros já emitidos.
 *
 * Repare que também **não há `obterPorEmpresa`**: o recorte da empresa não é uma consulta, é uma leitura
 * sobre a concessão (`EscopoDoPool`), feita em memória sobre a coleção inteira. Coleções pequenas, junção
 * no cliente — a mesma escolha do "Porto X — Belém/PA", e a única possível num pool sem `empresaId`.
 */
interface ViagemRepository {
    fun sincronizar()
    fun observarTodas(): StateFlow<List<Viagem>>

    /** Cria e devolve **o id**. Viagem existente não é reescrita — ver a nota da porta. */
    suspend fun criar(viagem: Viagem): String

    suspend fun obterTodas(): List<Viagem>
    suspend fun obterPorId(id: String): Viagem?

    /** Tira de circulação sem apagar: o registro fica, e as passagens antigas continuam resolvendo. */
    suspend fun inativar(id: String)

    companion object {
        const val COLLECTION_VIAGENS = "viagens"
    }
}

/** O codec da Viagem: o pouco que é dela na fronteira (ADR-0019 D2). */
private object ViagemCodec : CodecFirestore<Viagem> {
    override val colecao = ViagemRepository.COLLECTION_VIAGENS
    override val entidade = "viagem"
    override fun deDocumento(bruto: DocumentoBruto) = bruto.toViagem()
    override fun paraMapa(modelo: Viagem) = modelo.paraMapa()
    override fun id(modelo: Viagem) = modelo.id
    override fun comId(modelo: Viagem, id: String) = modelo.copy(id = id)
}

/**
 * Impl Firestore da porta [ViagemRepository] — sétima coleção sem Room (ADR-0017 D1).
 *
 * A coleção `viagens/` já existe no projeto com documentos da **Viagem-trecho**, demolida na F8.0. Eles
 * continuam lá e são **recusados pelo codec** — não têm `rotaId`, e documento sem rota não é viagem. É o
 * mesmo desfecho dos documentos antigos de `navios/` depois do rename da Flotilha: invisíveis, não
 * migrados, e recadastrar pelo painel é o caminho.
 */
@Singleton
class ViagemFirestoreRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    registroCadastro: RegistroCadastro,
    @SyncScope syncScope: CoroutineScope,
    registroSincronizacao: RegistroSincronizacao,
    fonteSnapshots: FonteSnapshots,
) : ViagemRepository {

    private val colecao = ColecaoFirestore(
        codec = ViagemCodec,
        firestore = firestore,
        fonteSnapshots = fonteSnapshots,
        syncScope = syncScope,
        registroCadastro = registroCadastro,
        registroSincronizacao = registroSincronizacao,
    )

    override fun sincronizar() = colecao.sincronizar()

    override fun observarTodas(): StateFlow<List<Viagem>> = colecao.observarTodos()

    override suspend fun criar(viagem: Viagem): String = colecao.salvar(viagem.copy(id = ""))

    override suspend fun obterTodas(): List<Viagem> = colecao.obterTodos()

    override suspend fun obterPorId(id: String): Viagem? = colecao.obterPorId(id)

    override suspend fun inativar(id: String) {
        val viagem = obterPorId(id) ?: return
        colecao.salvar(viagem.copy(ativo = false))
    }
}