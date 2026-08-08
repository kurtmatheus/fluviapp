package dev.matheus.fluviapp.di.module

import dev.matheus.fluviapp.services.ibge.ConsultaMunicipioIbge
import dev.matheus.fluviapp.services.ibge.ConsultaMunicipioIbgeHttp
import dev.matheus.fluviapp.services.repository.cadastro.ConstanteFirestoreRepository
import dev.matheus.fluviapp.services.repository.cadastro.ConstanteRepository
import dev.matheus.fluviapp.services.repository.cadastro.localidade.LocalidadeFirestoreRepository
import dev.matheus.fluviapp.services.repository.cadastro.localidade.LocalidadeRepository
import dev.matheus.fluviapp.services.repository.cadastro.porto.PortoFirestoreRepository
import dev.matheus.fluviapp.services.repository.cadastro.porto.PortoRepository
import dev.matheus.fluviapp.services.repository.operacoes.ConviteFirestoreRepository
import dev.matheus.fluviapp.services.repository.operacoes.ConviteRepository
import dev.matheus.fluviapp.services.repository.operacoes.FuncionarioFirestoreRepository
import dev.matheus.fluviapp.services.repository.operacoes.FuncionarioRepository
import dev.matheus.fluviapp.services.repository.operacoes.SessaoUsuario
import dev.matheus.fluviapp.services.repository.operacoes.SessaoUsuarioRoom
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaFirestoreRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmbarcacaoFirestoreRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmbarcacaoRepository
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
    abstract fun bindEmbarcacaoRepository(impl: EmbarcacaoFirestoreRepository): EmbarcacaoRepository

    /** Capacidade da plataforma, na raiz (ADR-0016 §5). */
    @Binds
    @Singleton
    abstract fun bindLocalidadeRepository(impl: LocalidadeFirestoreRepository): LocalidadeRepository

    /** A outra capacidade da plataforma — o lugar físico, que referencia a localidade (ADR-0016 §5). */
    @Binds
    @Singleton
    abstract fun bindPortoRepository(impl: PortoFirestoreRepository): PortoRepository

    /** Quem pode entrar, e com que papel (F6.6) — a coleção que o `ADM` escreve. */
    @Binds
    @Singleton
    abstract fun bindConviteRepository(impl: ConviteFirestoreRepository): ConviteRepository

    /**
     * Preenchimento pelo IBGE — **porta de fora**, e a única do app. Fica ao lado dos repositórios porque
     * o papel é o mesmo (buscar dado que não é nosso), mas o que ela devolve não é persistido por ela:
     * quem grava é a `LocalidadeRepository`.
     */
    @Binds
    @Singleton
    abstract fun bindConsultaMunicipioIbge(impl: ConsultaMunicipioIbgeHttp): ConsultaMunicipioIbge

    /** Quem está operando: os dois contextos resolvidos num lugar só (ADR-0015 §8.1). */
    @Binds
    @Singleton
    abstract fun bindSessaoUsuario(impl: SessaoUsuarioRoom): SessaoUsuario

    /** Fonte de snapshots (seam testável do sync — §10 Nível 2). */
    @Binds
    @Singleton
    abstract fun bindFonteSnapshots(impl: FonteSnapshotsFirestore): FonteSnapshots
}
