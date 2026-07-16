package dev.matheus.fluviapp.services.repository.firebase

import android.util.Log
import dev.matheus.fluviapp.database.dao.ContadorDao
import dev.matheus.fluviapp.database.dao.passagem.PassagemDao
import dev.matheus.fluviapp.exceptions.EmissaoException
import dev.matheus.fluviapp.extensions.isTextoNaoNulo
import dev.matheus.fluviapp.extensions.toPassagemDocumento
import dev.matheus.fluviapp.model.ContadorBilhete
import dev.matheus.fluviapp.model.operacoes.Usuario
import dev.matheus.fluviapp.model.passagem.Passagem
import dev.matheus.fluviapp.services.repository.firebase.documents.ContadorDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.PassagemDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.toContadorBilhete
import dev.matheus.fluviapp.services.repository.firebase.documents.toPassagem
import dev.matheus.fluviapp.telemetry.RegistroEmissao
import dev.matheus.fluviapp.telemetry.RegistroSincronizacao
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.toObject
import dev.matheus.fluviapp.di.module.SyncScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PassagemFirestoreRepository @Inject constructor(
    private val dao: PassagemDao,
    private val contadorDao: ContadorDao,
    private val firestore: FirebaseFirestore,
    private val registroEmissao: RegistroEmissao,
    @SyncScope private val syncScope: CoroutineScope,
    private val registroSincronizacao: RegistroSincronizacao,
) {

    private var contadorJob: Job? = null

    // Contador de bilhete como Flow gerenciado (estudo sync, D2/D3): idempotente (mata o duplo-attach
    // Login+Main), grava sem runBlocking e NÃO lança dentro do callback (o `throw` antigo derrubava o
    // app). awaitClose remove a registration quando o escopo de sessão é cancelado (logout).
    fun sincronizarNumeroBilheteEmTempoReal() {
        if (contadorJob?.isActive == true) return
        contadorJob = callbackFlow {
            registroSincronizacao.iniciado(COLECAO_CONTADOR)
            val registration = firestore.collection(COLLECTION_PASSAGENS)
                .document(DOCUMENT_CONTADOR)
                .addSnapshotListener { value, error ->
                    if (error != null) {
                        registroSincronizacao.erro(COLECAO_CONTADOR, error)
                        return@addSnapshotListener
                    }
                    value?.let { snapshot ->
                        registroSincronizacao.snapshotRecebido(
                            COLECAO_CONTADOR,
                            docs = if (snapshot.exists()) 1 else 0,
                            doCache = snapshot.metadata.isFromCache,
                        )
                        snapshot.toObject<ContadorDocumento>()?.toContadorBilhete()
                            ?.let { trySend(it.contagem) }
                    }
                }
            awaitClose {
                registration.remove()
                registroSincronizacao.parado(COLECAO_CONTADOR)
            }
        }.onEach { contagem ->
            contadorDao.atualizarContagem(ContadorBilhete(contagem = contagem))
            registroSincronizacao.gravado(COLECAO_CONTADOR, 1)
        }.launchIn(syncScope)
    }

    suspend fun salvar(id: String, passagem: Passagem): String {
        val documento = retornaDocumentReference(id)
        val passagemComIdNumeroBilhete = passagem.copy(id = documento.id)
        val numero = passagemComIdNumeroBilhete.numero

        // volátil/cacheada -> sólida local (Room). Sucesso durável imediato.
        val passagemDocumento = try {
            dao.salvar(passagemComIdNumeroBilhete)
            passagemComIdNumeroBilhete.toPassagemDocumento()
        } catch (e: Exception) {
            Log.e(TAG, "salvar: Exception: ${e.message}")
            registroEmissao.falhou(EmissaoException.FalhaAoPersistir(e), numero)
            throw RuntimeException("Falha no Processo: ${e.message}")
        }
        registroEmissao.salvaLocal(numero)

        // Transmissão observada de forma não-bloqueante: offline apenas enfileira (nenhum
        // listener dispara); ack -> sincronizou; rejeição do servidor -> pendenteDeSync (warning).
        documento.set(passagemDocumento)
            .addOnSuccessListener { registroEmissao.sincronizou(numero) }
            .addOnFailureListener { e -> registroEmissao.pendenteDeSync(numero, e) }

        try {
            adicionarContador(id, numero)
        } catch (e: Exception) {
            Log.e(TAG, "salvar/contador: Exception: ${e.message}")
            registroEmissao.falhou(EmissaoException.NumeroIndisponivel(e.message ?: "contador"), numero)
            throw RuntimeException("Falha no Processo: ${e.message}")
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
        private const val TAG = "passagemFirestoreRepository"
        private const val COLLECTION_PASSAGENS = "passagens"
        private const val DOCUMENT_CONTADOR = "contador"
        private const val COLECAO_CONTADOR = "contador_bilhete"
        private const val FIELD_NUMERO = "numeroBilhete"
        private const val FIELD_DATA_VIAGEM = "dataViagem"
        private const val FIELD_STATUS = "status"
        private const val FIELD_NOME_FUNC = "funcionarioResponsavel"
    }
}