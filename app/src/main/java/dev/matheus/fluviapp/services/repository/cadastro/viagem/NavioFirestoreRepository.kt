package dev.matheus.fluviapp.services.repository.cadastro.viagem

import dev.matheus.fluviapp.database.dao.cadastro.viagem.NavioDao
import dev.matheus.fluviapp.model.viagem.Navio
import dev.matheus.fluviapp.services.repository.firebase.documents.NavioDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.toNavio
import dev.matheus.fluviapp.services.repository.firebase.sincronizarColecao
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Impl Firestore da porta [NavioRepository] — espelha no Room (ADR-0003). */
@Singleton
class NavioFirestoreRepository @Inject constructor(
    private val dao: NavioDao,
    private val firestore: FirebaseFirestore
) : NavioRepository {

    override fun sincronizar() = firestore.sincronizarColecao(
        colecao = COLLECTION_NAVIOS,
        tag = TAG,
        paraModelo = { it.toObject<NavioDocumento>()?.toNavio(it.id) },
        salvarLocal = { dao.salvar(it) },
    )

    override suspend fun obterTodos() = dao.obterTodos().first()

    override suspend fun obterPorId(id: String) = dao.obterPorId(id).first()

    override suspend fun salvar(navio: Navio) = dao.salvar(navio)

    override suspend fun obterPorNome(nome: String) = dao.obterPorNome(nome).first()

    private companion object {
        const val TAG = "navioRepository"
        const val COLLECTION_NAVIOS = "navios"
    }
}
