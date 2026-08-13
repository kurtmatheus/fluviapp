package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem

import dev.matheus.fluviapp.domain.passagem.ClasseVeiculo
import dev.matheus.fluviapp.domain.passagem.MetadadosPassagem
import dev.matheus.fluviapp.domain.passagem.PassagemDeVeiculo
import dev.matheus.fluviapp.domain.passagem.StatusPassagem
import dev.matheus.fluviapp.domain.veiculo.Veiculo
import dev.matheus.fluviapp.domain.viagem.OcorrenciaViagem
import dev.matheus.fluviapp.revitalizacao.ForaDoEscopo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    /** O bilhete de veículo se anuncia pela categoria: o que embarca é um veículo, e é isso que a doca vê. */
    @Test
    fun `passagem de veiculo se anuncia pela categoria`() {
        val conferencia = bilhete.paraConferencia(ReferenciasDaPassagem())

        assertEquals("#13", conferencia.numero)
        assertEquals("Veículo", conferencia.bilhete)
    }

    /**
     * **A placa não entra na conferência**, mesmo quando o veículo está carregado: *o embarque confere
     * bilhete e não pessoa*, e placa é dado pessoal indireto. Passar a referência não muda o resultado — é
     * o que garante que a decisão vive no mapper, e não na sorte de quem chamou.
     */
    @Test
    fun `a placa nao entra na conferencia nem quando o veiculo esta carregado`() {
        val conferencia = bilhete.paraConferencia(
            ReferenciasDaPassagem(veiculosPorId = mapOf(moto.id to moto)),
        )

        assertEquals("Veículo", conferencia.bilhete)
        assertTrue(moto.placa !in conferencia.toString())
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