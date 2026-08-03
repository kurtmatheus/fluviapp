package dev.matheus.fluviapp.services.repository.firebase

import android.util.Log
import dev.matheus.fluviapp.database.dao.cadastro.viagem.TarifaViagemDao
import dev.matheus.fluviapp.database.dao.cadastro.viagem.ViagemDao
import dev.matheus.fluviapp.extensions.formatarCodigoViagemNavioFB
import dev.matheus.fluviapp.extensions.paraMapaTarifas
import dev.matheus.fluviapp.extensions.tarifasParaLinhas
import dev.matheus.fluviapp.domain.viagem.TarifaViagem
import dev.matheus.fluviapp.domain.viagem.Viagem
import dev.matheus.fluviapp.domain.viagem.toDocumento
import dev.matheus.fluviapp.services.repository.cadastro.viagem.NavioRepository
import dev.matheus.fluviapp.services.repository.firebase.documents.toViagem
import dev.matheus.fluviapp.services.repository.firebase.documents.toViagemDocumento
import dev.matheus.fluviapp.di.module.SyncScope
import dev.matheus.fluviapp.telemetry.RegistroCadastro
import dev.matheus.fluviapp.telemetry.RegistroSincronizacao
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
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
    private val tarifaDao: TarifaViagemDao,
    private val firestore: FirebaseFirestore,
    private val registroCadastro: RegistroCadastro,
    private val navioRepository: NavioRepository,
    @SyncScope private val syncScope: CoroutineScope,
    private val registroSincronizacao: RegistroSincronizacao,
    private val fonteSnapshots: FonteSnapshots,
) : ViagemRepository {

    private var syncJob: Job? = null

    // Idempotente (D2): não re-anexa se já ativo — mata o vazamento/duplo-attach. Grava em lote no
    // escopo de sessão, sem runBlocking (D3). Cancelado por SincronizacaoSessao.parar() no logout.
    override fun sincronizar() {
        if (syncJob?.isActive == true) return
        syncJob = sincronizarColecao(
            fonte = fonteSnapshots,
            colecao = COLLECTION_VIAGENS,
            scope = syncScope,
            registro = registroSincronizacao,
            // Espelha a viagem E sua tabela de tarifas (ADR-0013): o doc traz o mapa aninhado, achatado
            // em linhas TarifaViagem por viagemId.
            paraModelo = { bruto ->
                val doc = bruto.toViagemDocumento()
                doc.toViagem(bruto.id) to doc.tarifasParaLinhas(bruto.id)
            },
            salvarTodos = { pares ->
                dao.salvarTodas(*pares.map { it.first }.toTypedArray())
                espelharTarifas(pares.map { it.first.id to it.second })
            },
        )
    }

    override suspend fun salvar(viagem: Viagem, tarifas: List<TarifaViagem>) {
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

        // Tarifas (ADR-0013): re-carimba o viagemId (na criação o id só nasce aqui) e substitui o
        // conjunto local (delete+insert). O mesmo conjunto vira o mapa aninhado do doc — o `.set`
        // reescreve o doc inteiro, então a tabela tem de ir junta ou seria apagada.
        val linhas = tarifas.map { it.copy(viagemId = completo.id) }
        tarifaDao.deletarPorViagem(completo.id)
        if (linhas.isNotEmpty()) tarifaDao.salvarTodas(*linhas.toTypedArray())

        // Room já tem o dado (otimista). Aguarda o ack do Firestore: SUCESSO se confirmar,
        // WARNING (pendente-sync) se rejeitar/offline — não relança, o dado local reconcilia.
        try {
            documento.set(completo.toDocumento().copy(tarifas = linhas.paraMapaTarifas())).await()
            registroCadastro.salvou(ENTIDADE, completo.id)
        } catch (e: Exception) {
            registroCadastro.pendenteDeSync(ENTIDADE, completo.id, e)
        }
    }

    override suspend fun obterTarifas(viagemId: String) = tarifaDao.obterPorViagemAgora(viagemId)

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
            val docs = snapshot.documents.map { doc ->
                val bruto = DocumentoBruto(doc.id, doc.data.orEmpty()).toViagemDocumento()
                doc.id to bruto
            }
            dao.salvarTodas(*docs.map { (id, doc) -> doc.toViagem(id) }.toTypedArray())
            espelharTarifas(docs.map { (id, doc) -> id to doc.tarifasParaLinhas(id) })
            registroSincronizacao.snapshotRecebido(COLLECTION_VIAGENS, snapshot.size(), snapshot.metadata.isFromCache)
        } catch (e: Exception) {
            registroSincronizacao.erro(COLLECTION_VIAGENS, e)
        }
    }

    /**
     * Espelha o conjunto de tarifas de cada viagem no Room (ADR-0013). Delete+insert por viagem = espelho
     * honesto: uma célula removida no Firestore não fica órfã localmente (diferente do upsert-só das
     * entidades). O balanço lê tarifas por viagemId, então a fidelidade importa.
     */
    private suspend fun espelharTarifas(porViagem: List<Pair<String, List<TarifaViagem>>>) {
        porViagem.forEach { (viagemId, tarifas) ->
            tarifaDao.deletarPorViagem(viagemId)
            if (tarifas.isNotEmpty()) tarifaDao.salvarTodas(*tarifas.toTypedArray())
        }
    }

    override suspend fun deletar(id: String) {
        val viagem = obterPorId(id)
        dao.deletar(viagem)
        tarifaDao.deletarPorViagem(id) // limpa a tabela de tarifas da viagem (ADR-0013)
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
