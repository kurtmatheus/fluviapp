package dev.matheus.fluviapp.services.repository.passagem

import dev.matheus.fluviapp.domain.operacoes.PermissoesUsuario.EscopoEmpresa
import dev.matheus.fluviapp.domain.passagem.CategoriaPassagem
import dev.matheus.fluviapp.domain.passagem.StatusPassagem
import dev.matheus.fluviapp.domain.viagem.OcorrenciaViagem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * A tradução critério → consulta ([ADR-0025] D2) — e a razão de ela existir como função pura está aqui: estes
 * casos rodam **sem Firebase**, que é a garantia que o `DocumentoBruto` criou para o mapeamento e que o
 * critério estende para a consulta.
 *
 * O caso que mais importa é o primeiro: *"não tenho empresa nenhuma"* **não vira consulta sem filtro**. Com a
 * `String` vazia de antes, ele virava — e abria a listagem inteira para quem não deveria ver nada.
 */
class CriterioPassagemTest {

    private val ocorrencia = OcorrenciaViagem(viagemId = "viagem-1", data = LocalDate.of(2026, 8, 18))

    private fun filtrosDe(criterio: CriterioPassagem): List<FiltroPassagem> =
        (criterio.traduzir() as PlanoDeConsulta.Filtrada).filtros

    // --- O escopo, que é fail-closed ---

    @Test
    fun `sem empresa nenhuma nao vira consulta`() {
        val plano = CriterioPassagem(escopo = EscopoEmpresa.Nenhuma).traduzir()

        assertEquals(PlanoDeConsulta.SemResultado, plano)
    }

    @Test
    fun `escopo padrao e o fechado, e nao o aberto`() {
        assertEquals(PlanoDeConsulta.SemResultado, CriterioPassagem().traduzir())
    }

    @Test
    fun `papel de plataforma atravessa empresas, sem filtro de agencia`() {
        val filtros = filtrosDe(CriterioPassagem(escopo = EscopoEmpresa.Todas))

        assertTrue(filtros.none { it is FiltroPassagem.Igual && it.campo == CAMPO_AGENCIA })
    }

    @Test
    fun `vinculo recorta pela empresa dele`() {
        val filtros = filtrosDe(CriterioPassagem(escopo = EscopoEmpresa.Apenas("empresa-1")))

        assertEquals(listOf(FiltroPassagem.Igual(CAMPO_AGENCIA, "empresa-1")), filtros)
    }

    // --- O recorte no tempo, que é um eixo só ---

    @Test
    fun `a ocorrencia vira dois filtros de igualdade`() {
        val filtros = filtrosDe(
            CriterioPassagem(escopo = EscopoEmpresa.Todas, recorte = RecorteTemporal.Ocorrencia(ocorrencia)),
        )

        assertEquals(
            listOf(
                FiltroPassagem.Igual(CAMPO_VIAGEM, "viagem-1"),
                FiltroPassagem.Igual(CAMPO_DATA, "2026-08-18"),
            ),
            filtros,
        )
    }

    @Test
    fun `o dia atravessa viagens, e nao filtra viagem nenhuma`() {
        val filtros = filtrosDe(
            CriterioPassagem(
                escopo = EscopoEmpresa.Todas,
                recorte = RecorteTemporal.Dia(LocalDate.of(2026, 8, 18)),
            ),
        )

        assertEquals(listOf(FiltroPassagem.Igual(CAMPO_DATA, "2026-08-18")), filtros)
    }

    /** A faixa funciona sem truque porque a data é texto ISO: ordena lexicograficamente = cronologicamente. */
    @Test
    fun `o periodo vira faixa fechada sobre texto ISO`() {
        val filtros = filtrosDe(
            CriterioPassagem(
                escopo = EscopoEmpresa.Todas,
                recorte = RecorteTemporal.Periodo(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)),
            ),
        )

        assertEquals(listOf(FiltroPassagem.NaFaixa(CAMPO_DATA, "2026-08-01", "2026-08-31")), filtros)
    }

    @Test
    fun `sem recorte temporal, nenhuma condicao de data entra`() {
        val filtros = filtrosDe(CriterioPassagem(escopo = EscopoEmpresa.Todas))

        assertTrue(filtros.isEmpty())
    }

    // --- Os demais eixos ---

    @Test
    fun `status e categoria entram pelo nome canonico`() {
        val filtros = filtrosDe(
            CriterioPassagem(
                escopo = EscopoEmpresa.Todas,
                status = StatusPassagem.CANCELADA,
                categoria = CategoriaPassagem.VEICULO,
            ),
        )

        assertEquals(
            listOf(
                FiltroPassagem.Igual(CAMPO_STATUS, "CANCELADA"),
                FiltroPassagem.Igual(CAMPO_CATEGORIA, "VEICULO"),
            ),
            filtros,
        )
    }

    /** *"Em que passagens esta pessoa viajou"* — uma consulta só, porque o titular está no array (D3). */
    @Test
    fun `o cliente e procurado dentro do array`() {
        val filtros = filtrosDe(CriterioPassagem(escopo = EscopoEmpresa.Todas, clienteId = "cli-7"))

        assertEquals(listOf(FiltroPassagem.ContemNoArray(CAMPO_CLIENTES, "cli-7")), filtros)
    }

    /** Texto em branco é ausência, e não um filtro por vazio — o defeito que o tipo veio corrigir. */
    @Test
    fun `funcionario e cliente em branco nao viram filtro`() {
        val filtros = filtrosDe(
            CriterioPassagem(escopo = EscopoEmpresa.Todas, funcionarioId = "  ", clienteId = ""),
        )

        assertTrue(filtros.isEmpty())
    }

    @Test
    fun `os eixos se combinam numa consulta so`() {
        val filtros = filtrosDe(
            CriterioPassagem(
                recorte = RecorteTemporal.Ocorrencia(ocorrencia),
                escopo = EscopoEmpresa.Apenas("empresa-1"),
                status = StatusPassagem.EMITIDA,
                funcionarioId = "func-1",
            ),
        )

        assertEquals(
            listOf(
                FiltroPassagem.Igual(CAMPO_AGENCIA, "empresa-1"),
                FiltroPassagem.Igual(CAMPO_VIAGEM, "viagem-1"),
                FiltroPassagem.Igual(CAMPO_DATA, "2026-08-18"),
                FiltroPassagem.Igual(CAMPO_STATUS, "EMITIDA"),
                FiltroPassagem.Igual(CAMPO_FUNCIONARIO, "func-1"),
            ),
            filtros,
        )
    }
}
