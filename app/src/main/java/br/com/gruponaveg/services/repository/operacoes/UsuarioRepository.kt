package br.com.gruponaveg.services.repository.operacoes

import android.util.Log
import br.com.gruponaveg.database.dao.operacoes.UsuarioDao
import br.com.gruponaveg.model.operacoes.Usuario
import br.com.gruponaveg.services.repository.firebase.FirebaseAuthRepository
import br.com.gruponaveg.services.repository.firebase.documents.UsuarioDocumento
import br.com.gruponaveg.services.repository.firebase.documents.toUsuario
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsuarioRepository @Inject constructor(
    private val dao: UsuarioDao,
    private val firebaseAuthRepository: FirebaseAuthRepository,
    private val firestore: FirebaseFirestore,
) {
    suspend fun salvar(usuario: Usuario) = dao.salvar(usuario)

    fun autenticarUsuario(email: String, senha: String) = firebaseAuthRepository.autenticarUsuarioFirebase(email, senha)

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

    suspend fun obterPorEmailSenha(email: String, senha: String) = dao.obterPorUsuarioESenha(email, senha).first()

    suspend fun salvarUsuarioAutenticado(email: String, senha: String): Usuario? {
        limparUltimoUsuarioLogado()
        val usuarioAutenticado = dao.obterPorEmail(email = email).first()
        if (usuarioAutenticado != null) {
            dao.salvar(usuarioAutenticado.copy(senha = senha, ultimoUsuarioLogado = true))
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