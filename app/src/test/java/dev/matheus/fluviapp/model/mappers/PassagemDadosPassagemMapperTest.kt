package dev.matheus.fluviapp.model.mappers

import dev.matheus.fluviapp.extensions.formataParaMoedaBrasileira
import dev.matheus.fluviapp.fakes.FakeAgenteRepository
import dev.matheus.fluviapp.fakes.FakeEmpresaRepository
import dev.matheus.fluviapp.model.passagem.Passagem
import dev.matheus.fluviapp.model.passagem.TipoPassagem
import dev.matheus.fluviapp.model.viagem.Empresa
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

/**
 * Trava a relação Passagem→Empresa por id (ADR-0008): a DadosPassagem resolve a empresa pelo
 * `empresaId` congelado, não pelo nome do snapshot. Consequências: rename-safe (empresa renomeada
 * ainda casa) e órfão detectável (empresa removida → campos vazios, sem estourar como `obterPorNome`
 * fazia). `idViagem` vem do `viagemId` congelado — sem ida à Viagem viva.
 */
@OptIn(ExperimentalCoroutinesApi::class)
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
    fun `resolve empresa pelo id e ignora o nome defasado do snapshot`() = runTest {
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
    fun `empresa removida deixa campos vazios sem estourar`() = runTest {
        // repo vazio: obterPorId(empresaId) -> null (antes, obterPorNome estourava).
        val dados = mapper(emptyList()).map(passagem())

        assertEquals("", dados.empresaNome)
        assertEquals("", dados.empresaCnpj)
        assertEquals("", dados.empresaEndereco)
        // resto da passagem segue mapeado normalmente.
        assertEquals("2444", dados.numero)
        assertEquals("viagem-abc", dados.idViagem)
    }

    // --- Preço tabelado (ADR-0013): derivado da tarifaBase congelada ---

    private fun moeda(valor: String) = BigDecimal(valor).formataParaMoedaBrasileira()

    @Test
    fun `inteira deriva a devida da tarifaBase e o desconto do residuo abaixo dela`() = runTest {
        val dados = mapper(emptyList()).map(
            passagem().copy(tarifaBase = 300.0, tipoPassagem = TipoPassagem.INTEIRA.name, valorPago = 280.0),
        )

        assertEquals(moeda("300.00"), dados.tarifa)      // base da célula
        assertEquals(moeda("300.00"), dados.valorTotal)  // devida (inteira = base)
        assertEquals(moeda("20.00"), dados.desconto)     // resíduo abaixo da devida
        assertEquals(moeda("280.00"), dados.valorAPagar)
    }

    @Test
    fun `meia deve metade da base e a reducao mandatoria nao vira desconto`() = runTest {
        val dados = mapper(emptyList()).map(
            passagem().copy(tarifaBase = 300.0, tipoPassagem = TipoPassagem.MEIA.name, valorDinheiro = 150.0),
        )

        assertEquals(moeda("150.00"), dados.valorTotal) // metade
        assertEquals(moeda("0.00"), dados.desconto)     // cobrou a devida → sem desconto
        assertEquals(moeda("150.00"), dados.valorAPagar)
    }

    @Test
    fun `gratuidade zera a devida e o desconto`() = runTest {
        val dados = mapper(emptyList()).map(
            passagem().copy(tarifaBase = 300.0, tipoPassagem = TipoPassagem.GRATUIDADE.name),
        )

        assertEquals(moeda("0.00"), dados.valorTotal)
        assertEquals(moeda("0.00"), dados.desconto)
    }

    @Test
    fun `sem tarifaBase degrada para o valor cobrado mais o desconto persistido`() = runTest {
        val dados = mapper(emptyList()).map(
            passagem().copy(tarifaBase = null, valorPago = 100.0, desconto = 10.0),
        )

        assertEquals(moeda("110.00"), dados.valorTotal)  // cobrado + desconto legado
        assertEquals(moeda("10.00"), dados.desconto)
        assertEquals(moeda("100.00"), dados.valorAPagar)
    }
}