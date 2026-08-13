package dev.matheus.fluviapp.di.module

import dev.matheus.fluviapp.services.ibge.ConsultaMunicipioIbge
import dev.matheus.fluviapp.services.ibge.ConsultaMunicipioIbgeHttp
import dev.matheus.fluviapp.services.repository.cadastro.ConstanteFirestoreRepository
import dev.matheus.fluviapp.services.repository.cadastro.ConstanteRepository
import dev.matheus.fluviapp.services.repository.cadastro.localidade.LocalidadeFirestoreRepository
import dev.matheus.fluviapp.services.repository.cadastro.localidade.LocalidadeRepository
import dev.matheus.fluviapp.services.repository.cadastro.porto.PortoFirestoreRepository
import dev.matheus.fluviapp.services.repository.cadastro.porto.PortoRepository
import dev.matheus.fluviapp.services.repository.cadastro.rota.RotaFirestoreRepository
import dev.matheus.fluviapp.services.repository.cadastro.rota.RotaRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.ViagemFirestoreRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.ViagemRepository
import dev.matheus.fluviapp.services.repository.passagem.PassagemFirestoreRepository
import dev.matheus.fluviapp.services.repository.passagem.PassagemRepository
import dev.matheus.fluviapp.services.repository.operacoes.ConviteFirestoreRepository
import dev.matheus.fluviapp.services.repository.operacoes.ConviteRepository
import dev.matheus.fluviapp.services.repository.operacoes.FuncionarioFirestoreRepository
import dev.matheus.fluviapp.services.repository.operacoes.FuncionarioRepository
import dev.matheus.fluviapp.services.repository.operacoes.EscopoDaSessao
import dev.matheus.fluviapp.services.repository.operacoes.EscopoDaSessaoPadrao
import dev.matheus.fluviapp.services.repository.operacoes.SessaoUsuario
import dev.matheus.fluviapp.services.repository.operacoes.SessaoUsuarioRoom
import dev.matheus.fluviapp.util.Relogio
import dev.matheus.fluviapp.util.RelogioDoSistema
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaFirestoreRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmbarcacaoFirestoreRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmbarcacaoRepository
import dev.matheus.fluviapp.services.repository.firebase.FonteSnapshots
import dev.matheus.fluviapp.services.repository.firebase.FonteSnapshotsFirestore
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

    /** O pool compartilhado (ADR-0016 §7.1): a ligação entre dois portos, sem dono (F7). */
    @Binds
    @Singleton
    abstract fun bindRotaRepository(impl: RotaFirestoreRepository): RotaRepository

    /** O outro habitante do pool: a partida física sobre a rota (F8). */
    @Binds
    @Singleton
    abstract fun bindViagemRepository(impl: ViagemFirestoreRepository): ViagemRepository

    /**
     * A **passagem** (F9.2) — a primeira porta que se define pelas ausências: sem editar, sem deletar, sem
     * observar a coleção inteira ([ADR-0025] D1). Com ela, a entidade que era injetada como classe concreta em
     * dez lugares passa a ter fake, e o ViewModel da emissão fica testável pela primeira vez.
     */
    @Binds
    @Singleton
    abstract fun bindPassagemRepository(impl: PassagemFirestoreRepository): PassagemRepository

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

    /** Quanto do pool compartilhado quem está operando enxerga (F8.2). */
    @Binds
    @Singleton
    abstract fun bindEscopoDaSessao(impl: EscopoDaSessaoPadrao): EscopoDaSessao

    /** O instante presente — porta fina para "a saída das 06:00 já partiu" virar teste (F8.4). */
    @Binds
    @Singleton
    abstract fun bindRelogio(impl: RelogioDoSistema): Relogio

    /** Fonte de snapshots (seam testável do sync — §10 Nível 2). */
    @Binds
    @Singleton
    abstract fun bindFonteSnapshots(impl: FonteSnapshotsFirestore): FonteSnapshots
}
