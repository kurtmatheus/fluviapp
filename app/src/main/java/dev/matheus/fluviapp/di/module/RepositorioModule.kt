package dev.matheus.fluviapp.di.module

import dev.matheus.fluviapp.services.repository.cadastro.passagem.AgenteFirestoreRepository
import dev.matheus.fluviapp.services.repository.cadastro.passagem.AgenteRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaFirestoreRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaRepository
import dev.matheus.fluviapp.services.repository.firebase.ViagemFirestoreRepository
import dev.matheus.fluviapp.services.repository.firebase.ViagemRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Liga as portas de repositório de cadastro às impls Firestore (DIP; habilita fakes nos testes). */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositorioModule {

    @Binds
    @Singleton
    abstract fun bindAgenteRepository(impl: AgenteFirestoreRepository): AgenteRepository

    @Binds
    @Singleton
    abstract fun bindEmpresaRepository(impl: EmpresaFirestoreRepository): EmpresaRepository

    @Binds
    @Singleton
    abstract fun bindViagemRepository(impl: ViagemFirestoreRepository): ViagemRepository
}
