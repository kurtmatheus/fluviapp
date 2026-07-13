package dev.matheus.fluviapp.services.repository.cadastro

import dev.matheus.fluviapp.database.dao.cadastro.ConstanteDao
import dev.matheus.fluviapp.services.repository.cadastro.ConstanteRepository.Companion.COLLECTION_CONSTANTS
import dev.matheus.fluviapp.services.repository.firebase.documents.ConstanteDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.toConstante
import dev.matheus.fluviapp.services.repository.firebase.sincronizarColecao
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Impl Firestore da porta [ConstanteRepository] — espelha no Room (ADR-0003). */
@Singleton
class ConstanteFirestoreRepository @Inject constructor(
    private val dao: ConstanteDao,
    private val firestore: FirebaseFirestore
) : ConstanteRepository {

    override fun sincronizar() = firestore.sincronizarColecao(
        colecao = COLLECTION_CONSTANTS,
        tag = TAG,
        paraModelo = { it.toObject<ConstanteDocumento>()?.toConstante(it.id) },
        salvarLocal = { dao.salvar(it) },
    )

    override suspend fun obterTodosPorCategoria(categoria: String) =
        dao.obterTodosPorCategoria(categoria = categoria).first()

    override suspend fun obterTodas() = dao.obterTodos().first()

    private companion object {
        const val TAG = "constantesRepository"
    }
}
