package dev.matheus.fluviapp.domain.mappers

import dev.matheus.fluviapp.revitalizacao.ForaDoEscopo
import org.junit.experimental.categories.Category
import dev.matheus.fluviapp.extensions.formataParaMoedaBrasileira
import dev.matheus.fluviapp.fakes.FakeEmpresaRepository
import dev.matheus.fluviapp.fakes.FakeEmbarcacaoRepository
import dev.matheus.fluviapp.domain.passagem.Passagem
import dev.matheus.fluviapp.domain.passagem.TipoPassagem
import dev.matheus.fluviapp.domain.viagem.Empresa
import dev.matheus.fluviapp.domain.viagem.Embarcacao
import dev.matheus.fluviapp.domain.viagem.TipoEmbarcacao
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
@Category(ForaDoEscopo::class)
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

    private fun embarcacao(id: String, nome: String) = Embarcacao(
        id = id,
        descricaoNome = nome,
        tipo = TipoEmbarcacao.FERRY_BOAT,
        capacidadeVeiculo = 0,
        capacidadeSuite2 = 0,
        capacidadeSuite3 = 0,
        capacidadeCamarote = 0,
        empresaId = "empresa-1",
    )

    private fun passagem() = Passagem(
        id = "passagem-1",
        numero = "2444",
        viagemId = "viagem-abc",
        empresaId = "empresa-1",
        embarcacaoId = "embarcacao-1",
        // snapshots por valor defasados de propósito — NÃO devem ser usados p/ resolver empresa/embarcacao.
        empresa = "NOME ANTIGO",
        embarcacao = "F/B ANTIGO",
        codigoViagem = "PN-IC-001",
        origem = "Porto Norte",
        destino = "Ilha Central",
        dataViagem = "10/06/2024",
        horaViagem = "12:00",
        funcionarioResponsavel = "Operador",
        status = "A_EMITIR",
    )

    private fun mapper(empresas: List<Empresa>, embarcacoes: List<Embarcacao> = emptyList()) = PassagemDadosPassagemMapper(
        empresaRepository = FakeEmpresaRepository().apply { this.empresas = empresas },
        embarcacaoRepository = FakeEmbarcacaoRepository().apply { this.embarcacoes = embarcacoes },
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

    @Test
    fun `resolve embarcacao pelo id e ignora o nome defasado do snapshot`() = runTest {
        val embarcacoes = listOf(embarcacao("embarcacao-1", "F/B NOVO"))

        val dados = mapper(emptyList(), embarcacoes).map(passagem())

        // nome vem do cadastro vivo (por id), não do snapshot "F/B ANTIGO" — rename-safe (ADR-0008).
        assertEquals("F/B NOVO", dados.embarcacao)
    }

    @Test
    fun `embarcacao removido deixa o campo vazio sem estourar`() = runTest {
        // repo de embarcacoes vazio: obterPorId(embarcacaoId) -> null → nome vazio (órfão detectável).
        val dados = mapper(emptyList()).map(passagem())

        assertEquals("", dados.embarcacao)
    }

    // --- Preço tabelado (ADR-0013): derivado da tarifaBase congelada ---

    private fun moeda(valor: String) = BigDecimal(valor).formataParaMoedaBrasileira()

    @Test
    fun `inteira deriva a devida da tarifaBase e o desconto do residuo abaixo dela`() = runTest {
        val dados = mapper(emptyList()).map(
            passagem().copy(tarifaBase = 300.0, tipoPassagem = TipoPassagem.INTEIRA.name, valorDinheiro = 280.0),
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
    fun `sem tarifaBase degrada para o valor cobrado sem desconto`() = runTest {
        val dados = mapper(emptyList()).map(
            passagem().copy(tarifaBase = null, valorDinheiro = 100.0),
        )

        assertEquals(moeda("100.00"), dados.valorTotal)  // degrada para o cobrado
        assertEquals(moeda("0.00"), dados.desconto)      // desconto removido da persistência
        assertEquals(moeda("100.00"), dados.valorAPagar)
    }
}
