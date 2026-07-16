package dev.matheus.fluviapp.services.repository.cadastro

import dev.matheus.fluviapp.database.dao.cadastro.ConstanteDao
import dev.matheus.fluviapp.services.repository.cadastro.ConstanteRepository.Companion.COLLECTION_CONSTANTS
import dev.matheus.fluviapp.services.repository.firebase.documents.ConstanteDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.toConstante
import dev.matheus.fluviapp.di.module.SyncScope
import dev.matheus.fluviapp.services.repository.firebase.sincronizarColecao
import dev.matheus.fluviapp.telemetry.RegistroSincronizacao
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Impl Firestore da porta [ConstanteRepository] — espelha no Room (ADR-0003). */
@Singleton
class ConstanteFirestoreRepository @Inject constructor(
    private val dao: ConstanteDao,
    private val firestore: FirebaseFirestore,
    @SyncScope private val syncScope: CoroutineScope,
    private val registroSincronizacao: RegistroSincronizacao,
) : ConstanteRepository {

    private var syncJob: Job? = null

    override fun sincronizar() {
        if (syncJob?.isActive == true) return
        syncJob = firestore.sincronizarColecao(
            colecao = COLLECTION_CONSTANTS,
            scope = syncScope,
            registro = registroSincronizacao,
            paraModelo = { it.toObject<ConstanteDocumento>()?.toConstante(it.id) },
            salvarTodos = { dao.salvarTodas(*it.toTypedArray()) },
        )
    }

    override suspend fun obterTodosPorCategoria(categoria: String) =
        dao.obterTodosPorCategoria(categoria = categoria).first()

    override suspend fun obterTodas() = dao.obterTodos().first()
}
