package dev.matheus.fluviapp.fakes

import dev.matheus.fluviapp.domain.viagem.TarifaViagem
import dev.matheus.fluviapp.domain.viagem.Viagem
import dev.matheus.fluviapp.services.repository.firebase.ViagemRepository
import kotlinx.coroutines.flow.flowOf

/** Fake da porta [ViagemRepository] para testes de ViewModel. */
class FakeViagemRepository : ViagemRepository {
    var viagens: List<Viagem> = emptyList()
    val salvos = mutableListOf<Viagem>()
    var tarifasSalvas: List<TarifaViagem> = emptyList()
    var tarifasPorViagem: Map<String, List<TarifaViagem>> = emptyMap()
    val deletados = mutableListOf<String>()
    var falharAoDeletar = false

    override fun sincronizar() = Unit
    override suspend fun salvar(viagem: Viagem, tarifas: List<TarifaViagem>) {
        salvos += viagem
        tarifasSalvas = tarifas
    }
    override suspend fun obterTarifas(viagemId: String): List<TarifaViagem> =
        tarifasPorViagem[viagemId] ?: emptyList()
    override suspend fun obterPorId(id: String): Viagem = viagens.first { it.id == id }
    override suspend fun obterTodas(): List<Viagem> = viagens
    override fun observarTodas() = flowOf(viagens)
    override suspend fun atualizarDoServidor() = Unit
    override suspend fun deletar(id: String) {
        if (falharAoDeletar) throw RuntimeException("falha simulada")
        deletados += id
        viagens = viagens.filterNot { it.id == id }
    }
}
