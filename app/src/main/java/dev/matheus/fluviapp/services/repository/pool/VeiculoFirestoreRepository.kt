package dev.matheus.fluviapp.services.repository.pool

import com.google.firebase.firestore.FirebaseFirestore
import dev.matheus.fluviapp.domain.veiculo.Veiculo
import dev.matheus.fluviapp.services.repository.firebase.CodecFirestore
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto
import dev.matheus.fluviapp.services.repository.firebase.documents.paraMapa
import dev.matheus.fluviapp.services.repository.firebase.documents.placaCanonica
import dev.matheus.fluviapp.services.repository.firebase.documents.toVeiculo
import dev.matheus.fluviapp.telemetry.RegistroCadastro
import javax.inject.Inject
import javax.inject.Singleton

/** O codec do Veículo. */
private object VeiculoCodec : CodecFirestore<Veiculo> {
    override val colecao = "veiculos"
    override val entidade = "veiculo"
    override fun deDocumento(bruto: DocumentoBruto) = bruto.toVeiculo()
    override fun paraMapa(modelo: Veiculo) = modelo.paraMapa()
    override fun id(modelo: Veiculo) = modelo.id
    override fun comId(modelo: Veiculo, id: String) = modelo.copy(id = id)
}

/**
 * Impl Firestore da porta [VeiculoRepository] — o segundo pool, com a mesma mecânica e a chave melhor.
 *
 * A chave natural é a **placa canônica**, e a normalização não é cosmética: `abc-1d23` e `ABC1D23` são o
 * mesmo veículo, e sem ela virariam dois documentos — reintroduzindo por digitação a duplicata que este pool,
 * ao contrário do de pessoas, não deveria ter.
 */
@Singleton
class VeiculoFirestoreRepository @Inject constructor(
    firestore: FirebaseFirestore,
    registroCadastro: RegistroCadastro,
) : VeiculoRepository {

    private val pool = PoolFirestore(
        codec = VeiculoCodec,
        firestore = firestore,
        registroCadastro = registroCadastro,
        chaveNaturalDe = { placaCanonica(it.placa) },
        comAssinatura = { veiculo, agenciaId -> veiculo.copy(agenciaIds = veiculo.agenciaIds + agenciaId) },
        campoDeOrdenacao = "placa",
    )

    override suspend fun criarOuAssinar(veiculo: Veiculo, agenciaId: String): String =
        pool.criarOuAssinar(veiculo, agenciaId)

    override suspend fun obterPorId(id: String): Veiculo? = pool.obterPorId(id)

    override suspend fun obterPorIds(ids: List<String>): List<Veiculo> = pool.obterPorIds(ids)

    override suspend fun consultarDaAgencia(agenciaId: String): List<Veiculo> =
        pool.consultarDaAgencia(agenciaId)
}