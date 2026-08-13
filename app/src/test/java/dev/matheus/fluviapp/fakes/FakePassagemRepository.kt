package dev.matheus.fluviapp.fakes

import dev.matheus.fluviapp.domain.passagem.CarimboEmbarque
import dev.matheus.fluviapp.domain.passagem.Passagem
import dev.matheus.fluviapp.domain.passagem.PassagemDePassageiro
import dev.matheus.fluviapp.domain.passagem.PassagemDeVeiculo
import dev.matheus.fluviapp.domain.passagem.ResultadoEmbarque
import dev.matheus.fluviapp.domain.passagem.StatusPassagem
import dev.matheus.fluviapp.domain.viagem.OcorrenciaViagem
import dev.matheus.fluviapp.domain.operacoes.PermissoesUsuario.EscopoEmpresa
import dev.matheus.fluviapp.services.repository.passagem.CriterioPassagem
import dev.matheus.fluviapp.services.repository.passagem.PassagemRepository
import dev.matheus.fluviapp.services.repository.passagem.RecorteTemporal

/**
 * Fake da porta [PassagemRepository] — **o fake que não existia**.
 *
 * Ele é a razão prática de o [ADR-0025] D1 ter criado a porta: a passagem era a única entidade sem uma, com a
 * classe concreta injetada em dez lugares, e é por isso que **nunca houve teste de ViewModel de passagem**.
 * Não era desleixo: sem porta, não há o que substituir.
 *
 * A FSM é aplicada **aqui também**, e de propósito. Um fake que aceitasse qualquer transição faria o teste
 * passar por um motivo que produção não tem — e a guarda que ele deveria proteger (não reembarcar o já usado)
 * ficaria sem cobertura nenhuma.
 */
class FakePassagemRepository : PassagemRepository {

    var passagens: List<Passagem> = emptyList()

    /** Simula o QR lido num aparelho sem rede: `obterDoServidorPorId` lança, como o SDK faria. */
    var falharAoLerDoServidor = false

    val emitidas = mutableListOf<Passagem>()
    val transicoes = mutableListOf<Pair<String, StatusPassagem>>()

    /** Números que [reservarNumero] devolve, em ordem de chamada. */
    var proximoNumero = 1

    override suspend fun emitir(passagem: Passagem): String {
        val id = "passagem-${emitidas.size + 1}"
        val comId = passagem.comId(id)
        emitidas += comId
        passagens = passagens + comId
        return id
    }

    override suspend fun obterPorId(id: String): Passagem? = passagens.find { it.id == id }

    override suspend fun obterDoServidorPorId(id: String): Passagem? {
        if (falharAoLerDoServidor) throw RuntimeException("sem rede")
        return obterPorId(id)
    }

    /**
     * Aplica o critério de verdade, e não devolve tudo.
     *
     * Um fake que ignorasse os filtros faria o caso da **cota** passar por acidente: ele contaria as
     * gratuidades certas por não haver outras no cenário, e continuaria verde no dia em que o ViewModel
     * esquecesse de filtrar por categoria de gratuidade. O fake tem de errar onde a produção erraria.
     */
    override suspend fun consultar(criterio: CriterioPassagem): List<Passagem> {
        if (criterio.escopo is EscopoEmpresa.Nenhuma) return emptyList()

        return passagens.filter { passagem ->
            val recorte = criterio.recorte
            val naOcorrencia = recorte !is RecorteTemporal.Ocorrencia || passagem.ocorrencia == recorte.ocorrencia
            val doDia = recorte !is RecorteTemporal.Dia || passagem.ocorrencia.data == recorte.data
            val daEmpresa = when (val escopo = criterio.escopo) {
                is EscopoEmpresa.Apenas -> passagem.metadados.agenciaId == escopo.empresaId
                else -> true
            }
            val doStatus = criterio.status == null || passagem.metadados.status == criterio.status
            val daCategoria = criterio.categoria == null || passagem.categoria == criterio.categoria
            val daGratuidade = criterio.gratuidade == null ||
                (passagem as? PassagemDePassageiro)?.gratuidade == criterio.gratuidade

            naOcorrencia && doDia && daEmpresa && doStatus && daCategoria && daGratuidade
        }
    }

    override suspend fun transicionar(id: String, novo: StatusPassagem) {
        val passagem = obterPorId(id) ?: return
        if (!passagem.metadados.status.podeTransicionarPara(novo)) return
        transicoes += id to novo
        substituir(passagem.comStatus(novo))
    }

    override suspend fun confirmarEmbarque(id: String, operadorId: String): ResultadoEmbarque {
        val passagem = obterDoServidorPorId(id) ?: return ResultadoEmbarque.NaoEncontrada
        val atual = passagem.metadados.status

        if (atual == StatusPassagem.EMBARCADA) {
            return ResultadoEmbarque.JaEmbarcada(passagem.metadados.embarque ?: CarimboEmbarque("", ""))
        }
        if (!atual.podeTransicionarPara(StatusPassagem.EMBARCADA)) return ResultadoEmbarque.NaoEmitida

        val carimbo = CarimboEmbarque(porId = operadorId, em = INSTANTE_FIXO)
        val embarcada = passagem.comEmbarque(carimbo)
        substituir(embarcada)
        return ResultadoEmbarque.Confirmada(embarcada)
    }

    override suspend fun reservarNumero(ocorrencia: OcorrenciaViagem): Int = proximoNumero++

    private fun substituir(passagem: Passagem) {
        passagens = passagens.map { if (it.id == passagem.id) passagem else it }
    }

    private fun Passagem.comId(novoId: String): Passagem = when (this) {
        is PassagemDePassageiro -> copy(id = novoId)
        is PassagemDeVeiculo -> copy(id = novoId)
    }

    private fun Passagem.comStatus(novo: StatusPassagem): Passagem = when (this) {
        is PassagemDePassageiro -> copy(metadados = metadados.copy(status = novo))
        is PassagemDeVeiculo -> copy(metadados = metadados.copy(status = novo))
    }

    private fun Passagem.comEmbarque(carimbo: CarimboEmbarque): Passagem = when (this) {
        is PassagemDePassageiro ->
            copy(metadados = metadados.copy(status = StatusPassagem.EMBARCADA, embarque = carimbo))

        is PassagemDeVeiculo ->
            copy(metadados = metadados.copy(status = StatusPassagem.EMBARCADA, embarque = carimbo))
    }

    private companion object {
        /** Fixo porque o teste compara o carimbo, e um relógio real faria o caso variar por execução. */
        const val INSTANTE_FIXO = "2026-08-18T18:04:00"
    }
}