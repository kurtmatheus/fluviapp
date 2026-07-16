package dev.matheus.fluviapp.model.mappers

import dev.matheus.fluviapp.fakes.FakeAgenteRepository
import dev.matheus.fluviapp.fakes.FakeEmpresaRepository
import dev.matheus.fluviapp.model.passagem.Passagem
import dev.matheus.fluviapp.model.viagem.Empresa
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Trava a relação Passagem→Empresa por id (ADR-0008): a DadosPassagem resolve a empresa pelo
 * `empresaId` congelado, não pelo nome do snapshot. Consequências: rename-safe (empresa renomeada
 * ainda casa) e órfão detectável (empresa removida → campos vazios, sem estourar como `obterPorNome`
 * fazia). `idViagem` vem do `viagemId` congelado — sem ida à Viagem viva.
 */
class PassagemDadosPassagemMapperTest {

    private fun empresa(id: String, nome: String) = Empresa(
        id = id,
        nome = nome,
        razaoSocial = "$nome LTDA",
        cnpj = "00.000.000/0001-00",
        endereco = "Cais 1",
        telefone1 = "9999-0001",
        telefone2 = "9999-0002",
    )

    private fun passagem() = Passagem(
        id = "passagem-1",
        numero = "2444",
        viagemId = "viagem-abc",
        empresaId = "empresa-1",
        // snapshot por valor defasado de propósito — NÃO deve ser usado p/ resolver a empresa.
        empresa = "NOME ANTIGO",
        codigoViagem = "PN-IC-001",
        navio = "F/B Modelo",
        origem = "Porto Norte",
        destino = "Ilha Central",
        dataViagem = "10/06/2024",
        horaViagem = "12:00",
        funcionarioResponsavel = "Operador",
        status = "A_EMITIR",
    )

    private fun mapper(empresas: List<Empresa>) = PassagemDadosPassagemMapper(
        empresaRepository = FakeEmpresaRepository().apply { this.empresas = empresas },
        agenteRepository = FakeAgenteRepository(),
    )

    @Test
    fun `resolve empresa pelo id e ignora o nome defasado do snapshot`() {
        val empresas = listOf(empresa("empresa-1", "NOME NOVO"))

        val dados = mapper(empresas).map(passagem())

        // nome vem do cadastro vivo (por id), não do snapshot "NOME ANTIGO" — rename-safe.
        assertEquals("NOME NOVO", dados.empresaNome)
        assertEquals("NOME NOVO LTDA", dados.empresaRazaoSocial)
        assertEquals("00.000.000/0001-00", dados.empresaCnpj)
        // idViagem sai do viagemId congelado (sem lookup da Viagem).
        assertEquals("viagem-abc", dados.idViagem)
    }

    @Test
    fun `empresa removida deixa campos vazios sem estourar`() {
        // repo vazio: obterPorId(empresaId) -> null (antes, obterPorNome estourava).
        val dados = mapper(emptyList()).map(passagem())

        assertEquals("", dados.empresaNome)
        assertEquals("", dados.empresaCnpj)
        assertEquals("", dados.empresaEndereco)
        // resto da passagem segue mapeado normalmente.
        assertEquals("2444", dados.numero)
        assertEquals("viagem-abc", dados.idViagem)
    }
}