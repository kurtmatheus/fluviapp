package dev.matheus.fluviapp.services.repository.firebase

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    fun autenticarUsuarioFirebase(email: String, senha: String): Task<AuthResult> {
        return firebaseAuth.signInWithEmailAndPassword(email, senha)
    }
}
