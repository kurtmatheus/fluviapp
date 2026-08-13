package dev.matheus.fluviapp.services.repository.pool

import dev.matheus.fluviapp.domain.veiculo.Veiculo

/**
 * Porta do **pool de veículos** ([ADR-0018] D5) — mesmo regime do de clientes, com a chave melhor.
 *
 * A placa é única por construção, então este pool **não polui**: onde o de pessoas acumula duplicata legítima
 * (a mesma pessoa com CPF numa agência e RG noutra), aqui duplicata só nasce de digitação errada — e é contra
 * isso que existem a grafia canônica no codec e a máscara na entrada.
 */
interface VeiculoRepository {

    /** Registra o veículo e devolve o **id**, que é a placa canônica. */
    suspend fun criarOuAssinar(veiculo: Veiculo, agenciaId: String): String

    suspend fun obterPorId(id: String): Veiculo?

    suspend fun obterPorIds(ids: List<String>): List<Veiculo>

    /** O que **esta agência** já embarcou, por placa. */
    suspend fun consultarDaAgencia(agenciaId: String): List<Veiculo>
}