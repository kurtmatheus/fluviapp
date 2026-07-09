package dev.matheus.fluviapp.di.module

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.matheus.fluviapp.services.repository.firebase.autenticacao.AutenticacaoRepository
import dev.matheus.fluviapp.services.repository.firebase.autenticacao.FirebaseAutenticacaoRepository
import dev.matheus.fluviapp.telemetry.EmissaoTelemetry
import dev.matheus.fluviapp.telemetry.FirebaseEmissaoTelemetry
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return Firebase.auth
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return Firebase.firestore
    }

    @Provides
    @Singleton
    fun provideFirebaseAnalytics(@ApplicationContext context: Context): FirebaseAnalytics {
        return FirebaseAnalytics.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideFirebaseCrashlytics(): FirebaseCrashlytics {
        return Firebase.crashlytics
    }

    @Provides
    @Singleton
    fun provideAutenticacaoRepository(impl: FirebaseAutenticacaoRepository): AutenticacaoRepository {
        return impl
    }

    @Provides
    @Singleton
    fun provideEmissaoTelemetry(
        analytics: FirebaseAnalytics,
        crashlytics: FirebaseCrashlytics,
    ): EmissaoTelemetry {
        return FirebaseEmissaoTelemetry(analytics, crashlytics)
    }
}