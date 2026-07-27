package dev.matheus.fluviapp.di.module

import dev.matheus.fluviapp.services.repository.cadastro.ConstanteFirestoreRepository
import dev.matheus.fluviapp.services.repository.cadastro.ConstanteRepository
import dev.matheus.fluviapp.services.repository.operacoes.FuncionarioFirestoreRepository
import dev.matheus.fluviapp.services.repository.operacoes.FuncionarioRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaFirestoreRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.NavioFirestoreRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.NavioRepository
import dev.matheus.fluviapp.services.repository.firebase.FonteSnapshots
import dev.matheus.fluviapp.services.repository.firebase.FonteSnapshotsFirestore
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
    abstract fun bindFuncionarioRepository(impl: FuncionarioFirestoreRepository): FuncionarioRepository

    @Binds
    @Singleton
    abstract fun bindEmpresaRepository(impl: EmpresaFirestoreRepository): EmpresaRepository

    @Binds
    @Singleton
    abstract fun bindViagemRepository(impl: ViagemFirestoreRepository): ViagemRepository

    @Binds
    @Singleton
    abstract fun bindConstanteRepository(impl: ConstanteFirestoreRepository): ConstanteRepository

    @Binds
    @Singleton
    abstract fun bindNavioRepository(impl: NavioFirestoreRepository): NavioRepository

    /** Fonte de snapshots (seam testável do sync — §10 Nível 2). */
    @Binds
    @Singleton
    abstract fun bindFonteSnapshots(impl: FonteSnapshotsFirestore): FonteSnapshots
}
