package dev.matheus.fluviapp.services.repository.firebase.autenticacao

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import dev.matheus.fluviapp.services.repository.firebase.documents.UsuarioDocumento
import dev.matheus.fluviapp.services.repository.operacoes.UsuarioRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Impl Firebase da [AutenticacaoRepository]. Único ponto que toca `Task`/exceções do Firebase;
 * converte para [ResultadoAutenticacao] na borda (via [motivoDe]). Escrita de perfil é
 * fire-and-forget (offline-first, como as demais).
 */
class FirebaseAutenticacaoRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : AutenticacaoRepository {

    override suspend fun autenticar(email: String, senha: String): ResultadoAutenticacao = try {
        firebaseAuth.signInWithEmailAndPassword(email, senha).await()
        ResultadoAutenticacao.Sucesso(firebaseAuth.currentUser?.isEmailVerified == true)
    } catch (e: Exception) {
        ResultadoAutenticacao.Falha(motivoDe(e))
    }

    override suspend fun cadastrar(email: String, senha: String): ResultadoAutenticacao = try {
        firebaseAuth.createUserWithEmailAndPassword(email, senha).await()
        firebaseAuth.currentUser?.sendEmailVerification()?.await()
        ResultadoAutenticacao.Sucesso(emailVerificado = false)
    } catch (e: Exception) {
        ResultadoAutenticacao.Falha(motivoDe(e))
    }

    override suspend fun reenviarVerificacao(email: String, senha: String): ResultadoAutenticacao = try {
        firebaseAuth.signInWithEmailAndPassword(email, senha).await()
        firebaseAuth.currentUser?.sendEmailVerification()?.await()
        firebaseAuth.signOut()
        ResultadoAutenticacao.Sucesso(emailVerificado = false)
    } catch (e: Exception) {
        ResultadoAutenticacao.Falha(motivoDe(e))
    }

    override suspend fun criarPerfil(email: String, nome: String, cargo: String) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        firestore.collection(UsuarioRepository.COLLECTION_USERS).document(uid)
            .set(UsuarioDocumento(email = email, nome = nome, cargo = cargo))
    }

    override fun sair() {
        firebaseAuth.signOut()
    }
}

/** Tradução da exceção do Firebase (borda) para o motivo de domínio. */
internal fun motivoDe(erro: Throwable): MotivoFalhaAuth = when (erro) {
    is FirebaseAuthInvalidCredentialsException -> MotivoFalhaAuth.CREDENCIAL_INVALIDA
    is FirebaseAuthInvalidUserException -> MotivoFalhaAuth.USUARIO_INEXISTENTE
    is FirebaseAuthUserCollisionException -> MotivoFalhaAuth.EMAIL_JA_CADASTRADO
    else -> MotivoFalhaAuth.DESCONHECIDO
}