package dev.matheus.fluviapp.services.repository.cadastro.viagem

import com.google.firebase.firestore.FirebaseFirestore
import dev.matheus.fluviapp.di.module.SyncScope
import dev.matheus.fluviapp.domain.viagem.Embarcacao
import dev.matheus.fluviapp.services.repository.firebase.CodecFirestore
import dev.matheus.fluviapp.services.repository.firebase.ColecaoFirestore
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto
import dev.matheus.fluviapp.services.repository.firebase.FonteSnapshots
import dev.matheus.fluviapp.services.repository.firebase.documents.paraMapa
import dev.matheus.fluviapp.services.repository.firebase.documents.toEmbarcacao
import dev.matheus.fluviapp.telemetry.RegistroCadastro
import dev.matheus.fluviapp.telemetry.RegistroSincronizacao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** O codec da Embarcação: o pouco que é dela na fronteira (ADR-0019 D2). */
private object EmbarcacaoCodec : CodecFirestore<Embarcacao> {
    override val colecao = "embarcacoes"
    override val entidade = "embarcacao"
    override fun deDocumento(bruto: DocumentoBruto) = bruto.toEmbarcacao()
    override fun paraMapa(modelo: Embarcacao) = modelo.paraMapa()
    override fun id(modelo: Embarcacao) = modelo.id
    override fun comId(modelo: Embarcacao, id: String) = modelo.copy(id = id)
}

/**
 * Impl Firestore da porta [EmbarcacaoRepository] — **segunda coleção sem Room** (ADR-0017 D1), e a
 * primeira a nascer já sobre a [ColecaoFirestore].
 *
 * Repare no tamanho: o que era um repositório de noventa linhas virou o codec acima mais delegação. O
 * comportamento que sumiu daqui não sumiu do app — está na `ColecaoFirestore`, escrito e testado uma vez:
 * esperar o primeiro snapshot em vez de devolver vazio, escrita otimista com `pendenteDeSync`, id gerado
 * × id existente, listener idempotente.
 *
 * O espelho Room saiu junto (schema v4): o cache do SDK do Firestore já guarda entre execuções, e manter
 * os dois era pagar duas persistências para ter uma.
 */
@Singleton
class EmbarcacaoFirestoreRepository @Inject constructor(
    firestore: FirebaseFirestore,
    registroCadastro: RegistroCadastro,
    @SyncScope syncScope: CoroutineScope,
    registroSincronizacao: RegistroSincronizacao,
    fonteSnapshots: FonteSnapshots,
) : EmbarcacaoRepository {

    private val colecao = ColecaoFirestore(
        codec = EmbarcacaoCodec,
        firestore = firestore,
        fonteSnapshots = fonteSnapshots,
        syncScope = syncScope,
        registroCadastro = registroCadastro,
        registroSincronizacao = registroSincronizacao,
    )

    override fun observarTodas(): StateFlow<List<Embarcacao>> = colecao.observarTodos()

    override fun sincronizar() = colecao.sincronizar()

    override suspend fun obterTodos(): List<Embarcacao> = colecao.obterTodos()

    override suspend fun obterPorId(id: String): Embarcacao? = colecao.obterPorId(id)

    override suspend fun salvar(embarcacao: Embarcacao): String = colecao.salvar(embarcacao)

    override suspend fun deletar(id: String) = colecao.deletar(id)
}