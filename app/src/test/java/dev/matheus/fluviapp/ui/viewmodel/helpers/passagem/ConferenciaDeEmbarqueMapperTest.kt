package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem

import dev.matheus.fluviapp.domain.passagem.ClasseVeiculo
import dev.matheus.fluviapp.domain.passagem.MetadadosPassagem
import dev.matheus.fluviapp.domain.passagem.PassagemDeVeiculo
import dev.matheus.fluviapp.domain.passagem.StatusPassagem
import dev.matheus.fluviapp.domain.veiculo.Veiculo
import dev.matheus.fluviapp.domain.viagem.OcorrenciaViagem
import dev.matheus.fluviapp.revitalizacao.ForaDoEscopo
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.experimental.categories.Category
import java.time.LocalDate

/**
 * A junção **sem fake nenhum** — que é o argumento do [ADR-0025] D3 em forma executável: entram objetos, sai
 * objeto, e nada aqui suspende. Enquanto a busca morava dentro da tradução, um teste como este precisaria de
 * repositório, e por isso não existia.
 *
 * O que ele cobre é o **ramo do veículo**, que o teste do ViewModel não alcança — e é justamente onde o
 * `when` exaustivo por categoria vai cobrar decisão quando a carga existir.
 */
@Category(ForaDoEscopo::class)
class ConferenciaDeEmbarqueMapperTest {

    private val ocorrencia = OcorrenciaViagem(viagemId = "viagem-1", data = LocalDate.of(2026, 8, 18))

    private val bilhete = PassagemDeVeiculo(
        id = "pas-2",
        numero = "13",
        ocorrencia = ocorrencia,
        lancamentos = emptyList(),
        metadados = MetadadosPassagem(
            status = StatusPassagem.EMITIDA,
            funcionarioId = "func-1",
            agenciaId = "empresa-1",
            criadoEm = "2026-08-13T09:00:00",
            alteradoEm = "2026-08-13T09:00:00",
        ),
        veiculoId = "ABC1D23",
        responsavelRetirada = null,
    )

    private val moto = Veiculo(
        id = "ABC1D23",
        placa = "ABC1D23",
        tipo = ClasseVeiculo.MOTO,
        modelo = "Fan 150",
        cilindrada = 150,
    )

    /** No bilhete de veículo, quem embarca **é o veículo**: a placa é a identificação. */
    @Test
    fun `passagem de veiculo se identifica pela placa`() {
        val conferencia = bilhete.paraConferencia(
            ReferenciasDaPassagem(veiculosPorId = mapOf(moto.id to moto)),
        )

        assertEquals("#13", conferencia.numero)
        assertEquals("ABC1D23", conferencia.identificacao)
    }

    /**
     * Bilhete de veículo **sem responsável nomeado é a forma normal** (ADR-0023): a ausência não pode virar
     * "indisponível", que significa outra coisa — que existe e não se pode ver.
     */
    @Test
    fun `veiculo sem responsavel continua identificado pela placa`() {
        val conferencia = bilhete.paraConferencia(
            ReferenciasDaPassagem(veiculosPorId = mapOf(moto.id to moto), clientesPorId = emptyMap()),
        )

        assertEquals("ABC1D23", conferencia.identificacao)
    }

    @Test
    fun `veiculo que o pool nao entregou fica indisponivel`() {
        val conferencia = bilhete.paraConferencia(ReferenciasDaPassagem())

        assertEquals(IDENTIFICACAO_INDISPONIVEL, conferencia.identificacao)
    }

    /** Sem nenhuma referência, o bilhete ainda diz o que ele é — e é isso que o mantém conferível. */
    @Test
    fun `sem referencia nenhuma, sobra o que o bilhete carrega em si`() {
        val conferencia = bilhete.paraConferencia(ReferenciasDaPassagem())

        assertEquals("#13", conferencia.numero)
        assertEquals("Terça-feira, 18/08", conferencia.partida)
        assertEquals("", conferencia.travessia)
        assertEquals("EMITIDA", conferencia.status)
    }
}