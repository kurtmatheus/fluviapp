package dev.matheus.fluviapp.fakes

import dev.matheus.fluviapp.domain.cliente.Cliente
import dev.matheus.fluviapp.domain.veiculo.Veiculo
import dev.matheus.fluviapp.services.repository.pool.ClienteRepository
import dev.matheus.fluviapp.services.repository.pool.VeiculoRepository

/**
 * Fakes dos dois pools (F9.3).
 *
 * O que eles **imitam de propósito** é o recorte da leitura: no servidor, quem não assinou não lê (é PII), e
 * é isso que faz a conferência de embarque de um bilhete vendido por outra agência aparecer sem nome. Um fake
 * que devolvesse tudo esconderia justamente o caso que a tela precisa saber tratar — e ele não é uma falha,
 * é uma decisão do ADR-0018 D3 chegando à apresentação.
 */
class FakeClienteRepository : ClienteRepository {

    var clientes: List<Cliente> = emptyList()

    /**
     * A agência de quem lê. `null` = plataforma (lê tudo); preenchida, só o que ela assinou — como a regra.
     */
    var agenciaQueLe: String? = null

    val criados = mutableListOf<Pair<Cliente, String>>()

    override suspend fun criarOuAssinar(cliente: Cliente, agenciaId: String): String {
        criados += cliente to agenciaId
        val existente = clientes.find { it.chaveNatural == cliente.chaveNatural }
        val salvo = (existente ?: cliente).let { it.copy(id = it.chaveNatural, agenciaIds = it.agenciaIds + agenciaId) }
        clientes = clientes.filterNot { it.chaveNatural == salvo.chaveNatural } + salvo
        return salvo.id
    }

    override suspend fun obterPorId(id: String): Cliente? = visiveis().find { it.id == id }

    override suspend fun obterPorIds(ids: List<String>): List<Cliente> = visiveis().filter { it.id in ids }

    override suspend fun consultarDaAgencia(agenciaId: String): List<Cliente> =
        clientes.filter { it.assinadoPor(agenciaId) }.sortedBy { it.nome }

    private fun visiveis(): List<Cliente> =
        agenciaQueLe?.let { agencia -> clientes.filter { it.assinadoPor(agencia) } } ?: clientes
}

class FakeVeiculoRepository : VeiculoRepository {

    var veiculos: List<Veiculo> = emptyList()

    var agenciaQueLe: String? = null

    val criados = mutableListOf<Pair<Veiculo, String>>()

    override suspend fun criarOuAssinar(veiculo: Veiculo, agenciaId: String): String {
        criados += veiculo to agenciaId
        val existente = veiculos.find { it.placa == veiculo.placa }
        val salvo = (existente ?: veiculo).let { it.copy(id = it.placa, agenciaIds = it.agenciaIds + agenciaId) }
        veiculos = veiculos.filterNot { it.placa == salvo.placa } + salvo
        return salvo.id
    }

    override suspend fun obterPorId(id: String): Veiculo? = visiveis().find { it.id == id }

    override suspend fun obterPorIds(ids: List<String>): List<Veiculo> = visiveis().filter { it.id in ids }

    override suspend fun consultarDaAgencia(agenciaId: String): List<Veiculo> =
        veiculos.filter { agenciaId in it.agenciaIds }.sortedBy { it.placa }

    private fun visiveis(): List<Veiculo> =
        agenciaQueLe?.let { agencia -> veiculos.filter { agencia in it.agenciaIds } } ?: veiculos
}