package dev.matheus.fluviapp.services.repository.firebase

import android.util.Log
import dev.matheus.fluviapp.database.dao.cadastro.viagem.ViagemDao
import dev.matheus.fluviapp.extensions.formatarCodigoViagemNavioFB
import dev.matheus.fluviapp.model.viagem.Viagem
import dev.matheus.fluviapp.model.viagem.toDocumento
import dev.matheus.fluviapp.services.repository.cadastro.viagem.NavioRepository
import dev.matheus.fluviapp.services.repository.firebase.documents.ViagemDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.toViagem
import dev.matheus.fluviapp.di.module.SyncScope
import dev.matheus.fluviapp.telemetry.RegistroCadastro
import dev.matheus.fluviapp.telemetry.RegistroSincronizacao
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Impl Firestore da porta [ViagemRepository] — espelha no Room (ADR-0003), contrato do molde (ADR-0006). */
@Singleton
class ViagemFirestoreRepository @Inject constructor(
    private val dao: ViagemDao,
    private val firestore: FirebaseFirestore,
    private val registroCadastro: RegistroCadastro,
    private val navioRepository: NavioRepository,
    @SyncScope private val syncScope: CoroutineScope,
    private val registroSincronizacao: RegistroSincronizacao,
) : ViagemRepository {

    private var syncJob: Job? = null

    // Idempotente (D2): não re-anexa se já ativo — mata o vazamento/duplo-attach. Grava em lote no
    // escopo de sessão, sem runBlocking (D3). Cancelado por SincronizacaoSessao.parar() no logout.
    override fun sincronizar() {
        if (syncJob?.isActive == true) return
        syncJob = firestore.sincronizarColecao(
            colecao = COLLECTION_VIAGENS,
            scope = syncScope,
            registro = registroSincronizacao,
            paraModelo = { it.toObject<ViagemDocumento>()?.toViagem(it.id) },
            salvarTodos = { dao.salvarTodas(*it.toTypedArray()) },
        )
    }

    override suspend fun salvar(viagem: Viagem) {
        val documento = if (viagem.id.isBlank()) {
            firestore.collection(COLLECTION_VIAGENS).document()
        } else {
            firestore.collection(COLLECTION_VIAGENS).document(viagem.id)
        }
        // codigo é derivado na persistência (a partir do navio); id vem do doc. O nome do navio é
        // resolvido do navioId (ADR-0008 Fase 3 — a Viagem não guarda mais o nome).
        val comId = viagem.copy(id = documento.id)
        val navioNome = navioRepository.obterPorId(comId.navioId)?.descricaoNome.orEmpty()
        val completo = comId.copy(codigo = comId.formatarCodigoViagemNavioFB(navioNome))

        // FALHA: Room não gravou — desfecho não recuperável, propaga pro VM tratar.
        try {
            dao.salvar(completo)
        } catch (e: Exception) {
            registroCadastro.falhou(ENTIDADE, e)
            throw RuntimeException("Falha ao salvar viagem: ${e.message}", e)
        }

        // Room já tem o dado (otimista). Aguarda o ack do Firestore: SUCESSO se confirmar,
        // WARNING (pendente-sync) se rejeitar/offline — não relança, o dado local reconcilia.
        try {
            documento.set(completo.toDocumento()).await()
            registroCadastro.salvou(ENTIDADE, completo.id)
        } catch (e: Exception) {
            registroCadastro.pendenteDeSync(ENTIDADE, completo.id, e)
        }
    }

    override suspend fun obterPorId(id: String) = dao.obterPorId(id).first()

    override suspend fun obterTodas() = dao.obterTodas().first()

    // Reativo (D1): devolve o Flow do DAO direto — a UI observa e reage; nada de .first() one-shot.
    override fun observarTodas() = dao.obterTodas()

    // D5: pull-to-refresh força a busca no SERVIDOR (ignora o cache do Firestore), grava em lote no
    // Room (o Flow reativo reflete). Reporta ao registro: servidor → limpa o banner; falha (offline)
    // → liga o banner. Não relança — o VM só encerra o spinner.
    override suspend fun atualizarDoServidor() {
        try {
            val snapshot = firestore.collection(COLLECTION_VIAGENS).get(Source.SERVER).await()
            val viagens = snapshot.documents.mapNotNull { it.toObject<ViagemDocumento>()?.toViagem(it.id) }
            dao.salvarTodas(*viagens.toTypedArray())
            registroSincronizacao.snapshotRecebido(COLLECTION_VIAGENS, snapshot.size(), snapshot.metadata.isFromCache)
        } catch (e: Exception) {
            registroSincronizacao.erro(COLLECTION_VIAGENS, e)
        }
    }

    override suspend fun deletar(id: String) {
        val viagem = obterPorId(id)
        dao.deletar(viagem)
        try {
            firestore.collection(COLLECTION_VIAGENS).document(id).delete()
        } catch (e: Exception) {
            Log.e(TAG, "deletar: ${e.message}", e)
            throw RuntimeException("Falha ao deletar viagem: ${e.message}", e)
        }
    }

    private companion object {
        const val TAG = "viagemFirestoreRepository"
        const val COLLECTION_VIAGENS = "viagens"
        const val ENTIDADE = "viagem"
    }
}
