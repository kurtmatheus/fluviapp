package dev.matheus.fluviapp.services.repository.pool

import com.google.firebase.firestore.FirebaseFirestore
import dev.matheus.fluviapp.domain.cliente.Cliente
import dev.matheus.fluviapp.services.repository.firebase.CodecFirestore
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto
import dev.matheus.fluviapp.services.repository.firebase.documents.paraMapa
import dev.matheus.fluviapp.services.repository.firebase.documents.toCliente
import dev.matheus.fluviapp.telemetry.RegistroCadastro
import javax.inject.Inject
import javax.inject.Singleton

/** O codec do Cliente: o pouco que é dele na fronteira (ADR-0019 D2). */
private object ClienteCodec : CodecFirestore<Cliente> {
    override val colecao = "clientes"
    override val entidade = "cliente"
    override fun deDocumento(bruto: DocumentoBruto) = bruto.toCliente()
    override fun paraMapa(modelo: Cliente) = modelo.paraMapa()
    override fun id(modelo: Cliente) = modelo.id
    override fun comId(modelo: Cliente, id: String) = modelo.copy(id = id)
}

/**
 * Impl Firestore da porta [ClienteRepository] — o primeiro **pool** do app (F9.3).
 *
 * Compõe a [PoolFirestore], que é onde mora tudo o que distingue um pool de uma coleção: criar-ou-assinar por
 * tentativa e queda, leitura por ids em lote e consulta recortada pela assinatura. Aqui fica só o que é do
 * cliente — a **chave natural**, que é o documento apresentado.
 */
@Singleton
class ClienteFirestoreRepository @Inject constructor(
    firestore: FirebaseFirestore,
    registroCadastro: RegistroCadastro,
) : ClienteRepository {

    private val pool = PoolFirestore(
        codec = ClienteCodec,
        firestore = firestore,
        registroCadastro = registroCadastro,
        // A identidade do pool é a credencial, não a pessoa (ADR-0018 D2) — e ela vem do domínio, onde o
        // telefone está de fora de propósito: dois telefones não fazem duas pessoas, dois documentos fazem.
        chaveNaturalDe = { it.chaveNatural },
        comAssinatura = { cliente, agenciaId -> cliente.copy(agenciaIds = cliente.agenciaIds + agenciaId) },
        campoDeOrdenacao = "nome",
    )

    override suspend fun criarOuAssinar(cliente: Cliente, agenciaId: String): String =
        pool.criarOuAssinar(cliente, agenciaId)

    override suspend fun obterPorId(id: String): Cliente? = pool.obterPorId(id)

    override suspend fun obterPorIds(ids: List<String>): List<Cliente> = pool.obterPorIds(ids)

    override suspend fun consultarDaAgencia(agenciaId: String): List<Cliente> =
        pool.consultarDaAgencia(agenciaId)
}