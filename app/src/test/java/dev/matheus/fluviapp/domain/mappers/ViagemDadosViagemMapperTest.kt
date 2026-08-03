package dev.matheus.fluviapp.domain.mappers

import dev.matheus.fluviapp.revitalizacao.ForaDoEscopo
import org.junit.experimental.categories.Category
import dev.matheus.fluviapp.fakes.FakeConstanteRepository
import dev.matheus.fluviapp.fakes.FakeEmpresaRepository
import dev.matheus.fluviapp.fakes.FakeNavioRepository
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Categoria.MUNICIPIO
import dev.matheus.fluviapp.domain.viagem.Empresa
import dev.matheus.fluviapp.domain.viagem.Navio
import dev.matheus.fluviapp.domain.viagem.Viagem
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Card de viagem resolvido por id (ADR-0008) — rename-safe. */
@Category(ForaDoEscopo::class)
class ViagemDadosViagemMapperTest {

    private val fakeEmpresa = FakeEmpresaRepository().apply {
        empresas = listOf(Empresa("e1", "ACME ATUAL", "ACME LTDA", "1", "end", "1", "2"))
    }
    private val fakeNavio = FakeNavioRepository().apply {
        navios = listOf(Navio("n1", "F/B ATUAL", 60, 4, 5, 4, "e1"))
    }
    private val fakeConstante = FakeConstanteRepository().apply {
        constantes = listOf(
            Constante("1", "Porto Norte", MUNICIPIO.name),
            Constante("2", "Ilha Central", MUNICIPIO.name),
        )
    }
    private val mapper = ViagemDadosViagemMapper(fakeEmpresa, fakeNavio, fakeConstante)

    @Test
    fun `resolve empresa e navio pelo id (Viagem nao guarda mais os nomes)`() = runTest {
        val viagem = Viagem(
            id = "v1",
            codigo = "COD",
            origem = "Porto Norte",
            destino = "Ilha Central",
            empresaId = "e1",
            navioId = "n1",
        )

        val card = mapper.map(viagem)

        assertEquals("ACME ATUAL", card.empresa) // resolvido do id
        assertEquals("F/B ATUAL", card.navio)
        assertEquals("60", card.capacidadeVeiculos) // capacidades vêm do navio resolvido
        assertEquals("Porto Norte", card.origem)
        assertEquals("Ilha Central", card.destino)
    }

    @Test
    fun `id nao resolvido deixa empresa e navio vazios`() = runTest {
        val viagem = Viagem(
            id = "v1",
            codigo = "COD",
            origem = "Porto Norte",
            destino = "Ilha Central",
            empresaId = "", // não resolve
            navioId = "",
        )

        val card = mapper.map(viagem)

        assertEquals("", card.empresa)
        assertEquals("", card.navio)
        assertEquals("0", card.capacidadeVeiculos) // sem navio resolvido
    }
}
