package dev.matheus.fluviapp.extensions

import dev.matheus.fluviapp.model.passagem.Passagem
import dev.matheus.fluviapp.services.repository.firebase.documents.toPassagem
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Rede de regressão do mapper flatten (Room) ↔ nested (Firestore). Trava a estrutura ANTES do
 * flip do snapshot (ADR-0004), que mexe justamente na forma da Passagem. Inclui o lock da
 * assimetria conhecida do passageiro 3 (usa `tipoDocumentoPassageiro3` onde p1/p2 usam
 * `documentoPassageiroN`) — comportamento intencional, não bug.
 */
class PassagemDocumentoMapperTest {

    private fun passagemModelo() = Passagem(
        id = "id-1",
        numero = "2444",
        viagemId = "viagem-abc",
        navioId = "navio-xyz",
        empresaId = "empresa-123",
        codigoViagem = "PN-IC-001",
        empresa = "Empresa Modelo",
        navio = "F/B Modelo",
        origem = "Porto Norte",
        destino = "Ilha Central",
        dataViagem = "10/06/2024",
        horaViagem = "12:00",
        agencia = "MATRIZ",
        agente = "Agente Modelo",
        documentoPassageiro1 = "RG",
        numeroDocumentoPassageiro1 = "111",
        nomePassageiro1 = "Passageiro Um",
        tipoDocumentoPassageiro3 = "CPF3",
        numeroDocumentoPassageiro3 = "333",
        nomePassageiro3 = "Passageiro Tres",
        placaVeiculo = "ABC1D23",
        tipoVeiculo = "MOTO",
        cilindrada = "250",
        tarifaBase = 300.0,
        funcionarioResponsavel = "Operador",
        funcionarioId = "uid-operador-1",
        status = "A_EMITIR",
    )

    @Test
    fun `toPassagemDocumento aninha viagem e passageiros`() {
        val doc = passagemModelo().toPassagemDocumento()

        // viagemId é FK top-level (ponteiro por id, ADR-0008), fora do snapshot `viagem` (por valor).
        assertEquals("viagem-abc", doc.viagemId)
        // navioId/empresaId ficam DENTRO do snapshot `viagem` embutido (ids congelados da viagem).
        assertEquals("navio-xyz", doc.viagem?.navioId)
        assertEquals("empresa-123", doc.viagem?.empresaId)
        assertEquals("PN-IC-001", doc.viagem?.codigo)
        assertEquals("F/B Modelo", doc.viagem?.navio)
        assertEquals("RG", doc.passageiro1?.documento)
        assertEquals("ABC1D23", doc.veiculo?.placaVeiculo)
        // agência/agente agora viajam para o Firestore (Path B do ADR-0002/0003).
        assertEquals("MATRIZ", doc.agencia)
        assertEquals("Agente Modelo", doc.agente)
        // Lock da assimetria do p3: o "documento" do doc vem de tipoDocumentoPassageiro3.
        assertEquals("CPF3", doc.passageiro3?.documento)
        // uid do dono viaja top-level para o Firestore (ADR-0010 Fase 2).
        assertEquals("uid-operador-1", doc.funcionarioId)
    }

    @Test
    fun `toPassagem reidrata os campos planos a partir do aninhado`() {
        val passagem = passagemModelo().toPassagemDocumento().toPassagem("id-restaurado")

        assertEquals("id-restaurado", passagem.id)
        assertEquals("viagem-abc", passagem.viagemId)
        assertEquals("navio-xyz", passagem.navioId)
        assertEquals("empresa-123", passagem.empresaId)
        assertEquals("PN-IC-001", passagem.codigoViagem)
        assertEquals("RG", passagem.documentoPassageiro1)
        // Lock da assimetria reversa do p3.
        assertEquals("CPF3", passagem.tipoDocumentoPassageiro3)
        assertEquals("ABC1D23", passagem.placaVeiculo)
        assertEquals("uid-operador-1", passagem.funcionarioId)
    }

    @Test
    fun `round-trip flatten-nest-flatten preserva os campos existentes`() {
        val original = passagemModelo()

        val roundTrip = original.toPassagemDocumento().toPassagem(original.id)

        assertEquals(original.numero, roundTrip.numero)
        assertEquals(original.viagemId, roundTrip.viagemId)
        assertEquals(original.navioId, roundTrip.navioId)
        assertEquals(original.empresaId, roundTrip.empresaId)
        assertEquals(original.codigoViagem, roundTrip.codigoViagem)
        assertEquals(original.empresa, roundTrip.empresa)
        assertEquals(original.nomePassageiro1, roundTrip.nomePassageiro1)
        assertEquals(original.documentoPassageiro1, roundTrip.documentoPassageiro1)
        assertEquals(original.tipoDocumentoPassageiro3, roundTrip.tipoDocumentoPassageiro3)
        assertEquals(original.agencia, roundTrip.agencia)
        assertEquals(original.agente, roundTrip.agente)
        assertEquals(original.placaVeiculo, roundTrip.placaVeiculo)
        assertEquals(original.status, roundTrip.status)
        assertEquals(original.funcionarioResponsavel, roundTrip.funcionarioResponsavel)
        assertEquals(original.funcionarioId, roundTrip.funcionarioId)
        // tarifaBase congelada (ADR-0013) sobrevive ao flatten↔nest.
        assertEquals(original.tarifaBase, roundTrip.tarifaBase)
        // cilindrada da moto (ADR-0013) idem.
        assertEquals(original.cilindrada, roundTrip.cilindrada)
    }
}