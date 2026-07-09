package dev.matheus.fluviapp.services.repository.operacoes

import android.util.Log
import dev.matheus.fluviapp.database.dao.operacoes.UsuarioDao
import dev.matheus.fluviapp.model.operacoes.Usuario
import dev.matheus.fluviapp.services.repository.firebase.documents.UsuarioDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.toUsuario
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/** Perfil/leitura do usuário (Room + espelho Firestore). Autenticação vive na porta
 *  [dev.matheus.fluviapp.services.repository.firebase.autenticacao.AutenticacaoRepository]. */
@Singleton
class UsuarioRepository @Inject constructor(
    private val dao: UsuarioDao,
    private val firestore: FirebaseFirestore,
) {
    suspend fun salvar(usuario: Usuario) = dao.salvar(usuario)

    suspend fun carregarUsuarios() {
        firestore.collection(COLLECTION_USERS)
            .addSnapshotListener { value, error ->
                value?.documents?.mapNotNull { document ->
                    document.toObject<UsuarioDocumento>()?.toUsuario(document.id)
                }?.forEach { usuario ->
                    runBlocking {
                        dao.salvar(usuario)
                    }
                }

                if (error != null) {
                    Log.e(TAG, "carregarUsuarios: Exception: ${error.message}")
                    throw RuntimeException("Falha na Requisicao: ${error.message}", error)
                }
            }
    }

    suspend fun salvarUsuarioAutenticado(email: String): Usuario? {
        limparUltimoUsuarioLogado()
        val usuarioAutenticado = dao.obterPorEmail(email = email).first()
        if (usuarioAutenticado != null) {
            dao.salvar(usuarioAutenticado.copy(ultimoUsuarioLogado = true))
        }
        return usuarioAutenticado
    }

    private suspend fun limparUltimoUsuarioLogado() = dao.limparUltimoUsuarioLogado()

    suspend fun obterUltimoUsuarioLogado() = dao.obterUltimoUsuarioLogado().first()

    suspend fun obterTodos() = dao.obterTodos().first()

    companion object {
        private const val TAG = "usuarioRepository"
        const val COLLECTION_USERS = "users"
    }
}