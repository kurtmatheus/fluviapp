package dev.matheus.fluviapp.services.repository.firebase

import android.util.Log
import dev.matheus.fluviapp.database.dao.ContadorDao
import dev.matheus.fluviapp.database.dao.passagem.PassagemDao
import dev.matheus.fluviapp.extensions.isTextoNaoNulo
import dev.matheus.fluviapp.extensions.toPassagemDocumento
import dev.matheus.fluviapp.model.ContadorBilhete
import dev.matheus.fluviapp.model.operacoes.Usuario
import dev.matheus.fluviapp.model.passagem.Passagem
import dev.matheus.fluviapp.services.repository.firebase.documents.ContadorDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.PassagemDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.toContadorBilhete
import dev.matheus.fluviapp.services.repository.firebase.documents.toPassagem
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PassagemFirestoreRepository @Inject constructor(
    private val dao: PassagemDao,
    private val contadorDao: ContadorDao,
    private val firestore: FirebaseFirestore,
) {

    fun sincronizarNumeroBilheteEmTempoReal() {
        firestore.collection(COLLECTION_PASSAGENS)
            .document(DOCUMENT_CONTADOR)
            .addSnapshotListener { value, error ->
                value?.let { documentSnapshot ->
                    documentSnapshot.toObject<ContadorDocumento>()
                        ?.toContadorBilhete()
                        ?.let { contadorBilhete ->
                            runBlocking {
                                contadorDao.atualizarContagem(
                                    ContadorBilhete(contagem = contadorBilhete.contagem)
                                )
                            }
                        }
                }

                if (error != null) {
                    Log.e(TAG, "sincronizarNumeroBilheteEmTempoReal: Exception: ${error.message}")
                    throw RuntimeException("Falha na Requisicao: ${error.message}")
                }
            }
    }

    suspend fun salvar(id: String, passagem: Passagem): String {
        val documento = retornaDocumentReference(id)
        passagem.apply {
            val passagemComIdNumeroBilhete = copy(id = documento.id)
            try {
                dao.salvar(passagemComIdNumeroBilhete)
                val passagemDocumento = passagemComIdNumeroBilhete.toPassagemDocumento()
                documento.set(passagemDocumento)
                adicionarContador(id, passagemComIdNumeroBilhete.numero)
            } catch (e: Exception) {
                Log.e(TAG, "salvar: Exception: ${e.message}")
                throw RuntimeException("Falha no Processo: ${e.message}")
            }
        }
        return documento.id
    }

    private fun adicionarContador(id: String, numero: String) {
        if (!id.isTextoNaoNulo()) {
            runBlocking {
                atualizarContador(numero.toInt())
            }
        }
    }

    private fun retornaDocumentReference(id: String): DocumentReference {
        return if (id.isTextoNaoNulo()) {
            firestore.collection(COLLECTION_PASSAGENS).document(id)
        } else firestore.collection(COLLECTION_PASSAGENS).document()
    }

    suspend fun obterPorId(id: String) = dao.obterPorId(id).first()

    fun deletar(id: String) {
        try {
            firestore.collection(COLLECTION_PASSAGENS)
                .document(id)
                .delete()
        } catch (e: Exception) {
            Log.e(TAG, "deletar: Exception: ${e.message}")
            throw RuntimeException("Falha no Processo: ${e.message}")
        }
    }

    fun obterTodasPorData(data: String): Task<QuerySnapshot> {
        return firestore.collection(COLLECTION_PASSAGENS)
            .whereEqualTo(FIELD_DATA_VIAGEM, data)
            .get()
    }

    fun getListaNome(): List<String> {
        return emptyList()
    }

    suspend fun obterContagem() = contadorDao.obterContagem().first()

    fun obterTodasPorDataStatus(data: String, status: String, nomeFuncionario: String): Task<QuerySnapshot> {
        val queryStatusData = firestore.collection(COLLECTION_PASSAGENS)
            .whereEqualTo(FIELD_DATA_VIAGEM, data)
            .whereEqualTo(FIELD_STATUS, status)
        return if (nomeFuncionario != Usuario.GERAL) {
            queryStatusData
                .whereEqualTo(FIELD_NOME_FUNC, nomeFuncionario)
                .get()
                .addOnSuccessListener { snapshot ->
                    snapshot.documents.mapNotNull { document ->
                        document.toObject<PassagemDocumento>()?.toPassagem(document.id)
                    }.forEach {
                        runBlocking { dao.salvar(it) }
                    }
                }
        } else {
            queryStatusData
                .get()
                .addOnSuccessListener { snapshot ->
                snapshot.documents.mapNotNull { document ->
                    document.toObject<PassagemDocumento>()?.toPassagem(document.id)
                }.forEach {
                    runBlocking { dao.salvar(it) }
                }
            }
        }
    }

    fun atualizarSituacao(idPassagem: String, status: String) {
        firestore.collection(COLLECTION_PASSAGENS)
            .document(idPassagem)
            .update(mapOf(FIELD_STATUS to status))
    }

    private fun atualizarContador(numero: Int) {
        firestore.collection(COLLECTION_PASSAGENS)
            .document(DOCUMENT_CONTADOR)
            .update(mapOf(FIELD_NUMERO to numero))
    }

    companion object {
        private const val TAG = "viagemFirestoreRepository"
        private const val COLLECTION_PASSAGENS = "passagens"
        private const val DOCUMENT_CONTADOR = "contador"
        private const val FIELD_NUMERO = "numeroBilhete"
        private const val FIELD_DATA_VIAGEM = "dataViagem"
        private const val FIELD_STATUS = "status"
        private const val FIELD_NOME_FUNC = "funcionarioResponsavel"
    }
}