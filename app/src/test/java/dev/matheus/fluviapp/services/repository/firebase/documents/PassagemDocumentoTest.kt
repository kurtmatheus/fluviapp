package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.domain.passagem.Acomodacao
import dev.matheus.fluviapp.domain.passagem.CarimboEmbarque
import dev.matheus.fluviapp.domain.passagem.FormaPagamento
import dev.matheus.fluviapp.domain.passagem.Lancamento
import dev.matheus.fluviapp.domain.passagem.MetadadosPassagem
import dev.matheus.fluviapp.domain.passagem.PassagemDePassageiro
import dev.matheus.fluviapp.domain.passagem.PassagemDeVeiculo
import dev.matheus.fluviapp.domain.passagem.StatusPassagem
import dev.matheus.fluviapp.domain.passagem.TipoGratuidade
import dev.matheus.fluviapp.domain.passagem.TipoPassagem
import dev.matheus.fluviapp.domain.passagem.total
import dev.matheus.fluviapp.domain.viagem.OcorrenciaViagem
import dev.matheus.fluviapp.revitalizacao.ForaDoEscopo
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.experimental.categories.Category
import java.math.BigDecimal
import java.time.LocalDate

/**
 * A fronteira da Passagem depois que ela saiu do Room (F9.2, [ADR-0024]).
 *
 * O que estes casos protegem é o **direito de recusa** do codec — a parte do contrato que a passagem toma da
 * fronteira compartilhada. Recusar é o comportamento mais fácil de quebrar sem perceber: um `orEmpty()` a mais
 * transforma "não é uma passagem" em "passagem com campos em branco", e o defeito só aparece na tela de alguém.
 *
 * Marcados fora do escopo pela régua da F9.1: a seção `PASSAGEM` não está em `SECOES_REVITALIZADAS`, e estes
 * casos voltam à suíte de escopo na F9.6.
 */
@Category(ForaDoEscopo::class)
class PassagemDocumentoTest {

    private val ocorrencia = OcorrenciaViagem(viagemId = "viagem-1", data = LocalDate.of(2026, 8, 18))

    private val metadados = MetadadosPassagem(
        status = StatusPassagem.EMITIDA,
        funcionarioId = "func-1",
        agenciaId = "empresa-1",
        criadoEm = "2026-08-13T09:30:00",
        alteradoEm = "2026-08-13T09:30:00",
    )

    private val passageiro = PassagemDePassageiro(
        id = "pas-1",
        numero = "12",
        ocorrencia = ocorrencia,
        lancamentos = listOf(
            Lancamento("lanc-1", FormaPagamento.PIX, BigDecimal("100.00")),
            Lancamento("lanc-2", FormaPagamento.DINHEIRO, BigDecimal("50.00")),
        ),
        observacao = "janela",
        metadados = metadados,
        acomodacao = Acomodacao.SUITE,
        tipo = TipoPassagem.INTEIRA,
        clientes = listOf("cli-7", "cli-9"),
    )

    private val veiculo = PassagemDeVeiculo(
        id = "pas-2",
        numero = "13",
        ocorrencia = ocorrencia,
        lancamentos = listOf(Lancamento("lanc-3", FormaPagamento.CREDITO, BigDecimal("300.00"))),
        metadados = metadados,
        veiculoId = "vei-3",
        responsavelRetirada = "cli-7",
    )

    private fun documento(id: String, dados: Map<String, Any?>) = DocumentoBruto(id, dados)

    private fun documentoDe(passagem: dev.matheus.fluviapp.domain.passagem.Passagem) =
        documento(passagem.id, passagem.paraMapa())

    // --- A ida e a volta ---

    @Test
    fun `passagem de passageiro atravessa e volta igual`() {
        assertEquals(passageiro, documentoDe(passageiro).toPassagem())
    }

    @Test
    fun `passagem de veiculo atravessa e volta igual`() {
        assertEquals(veiculo, documentoDe(veiculo).toPassagem())
    }

    @Test
    fun `o id vem do documento, nao do campo`() {
        assertEquals("outro", documento("outro", passageiro.paraMapa()).toPassagem()?.id)
    }

    /** O titular é a **posição 0**, e a ordem sobrevive à travessia — a ordem *é* o significado (D3). */
    @Test
    fun `o titular continua sendo o primeiro depois de ir e voltar`() {
        val lida = documentoDe(passageiro).toPassagem() as PassagemDePassageiro

        assertEquals("cli-7", lida.titularId)
        assertEquals(listOf("cli-9"), lida.acompanhantesIds)
    }

    /** Dinheiro entra `Double` na fronteira e volta `BigDecimal` sem perder centavo (D4). */
    @Test
    fun `o total é inferido dos lancamentos, e nao ha campo de total`() {
        val lida = documentoDe(passageiro).toPassagem()!!

        assertEquals(BigDecimal("150.00"), lida.lancamentos.total)
        assertNull(passageiro.paraMapa()["valorTotal"])
    }

    // --- O direito de recusa ---

    @Test
    fun `documento sem categoria nao vira passagem`() {
        assertNull(documento("pas-1", passageiro.paraMapa() - "categoria").toPassagem())
    }

    @Test
    fun `categoria desconhecida nao vira passagem de categoria padrao`() {
        assertNull(documento("pas-1", passageiro.paraMapa() + ("categoria" to "CARGA")).toPassagem())
    }

    @Test
    fun `data que nao e ISO nao vira ocorrencia`() {
        assertNull(documento("pas-1", passageiro.paraMapa() + ("data" to "18/08/2026")).toPassagem())
    }

    @Test
    fun `documento sem viagem nao vira passagem`() {
        assertNull(documento("pas-1", passageiro.paraMapa() + ("viagemId" to "")).toPassagem())
    }

    @Test
    fun `status fora da maquina de estados nao vira passagem`() {
        assertNull(documento("pas-1", passageiro.paraMapa() + ("status" to "EXPIRADA")).toPassagem())
    }

    @Test
    fun `passagem de passageiro sem nenhum cliente nao tem sujeito, e e recusada`() {
        assertNull(documento("pas-1", passageiro.paraMapa() + ("clientes" to emptyList<String>())).toPassagem())
    }

    @Test
    fun `passagem de veiculo sem veiculo nao tem sujeito, e e recusada`() {
        assertNull(documento("pas-2", veiculo.paraMapa() + ("veiculoId" to "")).toPassagem())
    }

    @Test
    fun `acomodacao desconhecida nao vira rede`() {
        assertNull(documento("pas-1", passageiro.paraMapa() + ("acomodacao" to "CABINE")).toPassagem())
    }

    // --- A gratuidade, que voltou ao agregado (ADR-0028 D2) ---

    @Test
    fun `gratuidade atravessa e volta com o subtipo`() {
        val gratuita = passageiro.copy(
            tipo = TipoPassagem.GRATUIDADE,
            gratuidade = TipoGratuidade.IDOSO,
            acomodacao = Acomodacao.REDE,
            clientes = listOf("cli-7"),
            lancamentos = emptyList(),
        )

        assertEquals(gratuita, documentoDe(gratuita).toPassagem())
        assertEquals("IDOSO", gratuita.paraMapa()["gratuidade"])
    }

    /** Bilhete que não é gratuito não carrega subtipo — o par ou existe inteiro, ou não existe. */
    @Test
    fun `passagem inteira nao grava subtipo de gratuidade`() {
        assertNull(passageiro.paraMapa()["gratuidade"])
    }

    /**
     * Subtipo ilegível **não recusa o bilhete**, ao contrário do `tipo`: sem tipo não se sabe o que se
     * cobrou; sem subtipo perde-se a razão de uma gratuidade que continua sendo gratuidade. A incoerência
     * fica visível em `pendencias()`, que é onde a tela a cobra.
     */
    @Test
    fun `subtipo desconhecido nao derruba o bilhete, mas vira pendencia`() {
        // Rede, e não suíte: gratuidade só existe na rede, e usar suíte aqui somaria uma segunda
        // pendência (`TIPO_NAO_ADMITIDO`) que não é o que este caso investiga.
        val gratuita = passageiro.copy(
            acomodacao = Acomodacao.REDE,
            clientes = listOf("cli-7"),
            tipo = TipoPassagem.GRATUIDADE,
            gratuidade = TipoGratuidade.PCD,
        )
        val comLixo = documento("pas-1", gratuita.paraMapa() + ("gratuidade" to "AMIGO_DO_DONO"))

        val lida = comLixo.toPassagem() as PassagemDePassageiro
        assertNull(lida.gratuidade)
        assertEquals(setOf(PassagemDePassageiro.Pendencia.GRATUIDADE_SEM_SUBTIPO), lida.pendencias())
    }

    /**
     * A assimetria em relação ao `FuncionarioDocumento`, e a razão é o dinheiro: lá, item ilegível some da
     * lista; aqui ele **derruba o bilhete**. Descartar o lançamento faria a passagem valer R$ 100 onde
     * entraram R$ 150 — em silêncio, e esse número iria para o balanço.
     */
    @Test
    fun `lancamento com forma desconhecida recusa a passagem inteira`() {
        val comLixo = passageiro.paraMapa() + (
            "lancamentos" to listOf(
                mapOf("id" to "lanc-1", "forma" to "PIX", "valor" to 100.0),
                mapOf("id" to "lanc-2", "forma" to "BITCOIN", "valor" to 50.0),
            )
            )

        assertNull(documento("pas-1", comLixo).toPassagem())
    }

    @Test
    fun `lancamento sem valor numerico recusa a passagem inteira`() {
        val comLixo = passageiro.paraMapa() + (
            "lancamentos" to listOf(mapOf("id" to "lanc-1", "forma" to "PIX", "valor" to "cem reais"))
            )

        assertNull(documento("pas-1", comLixo).toPassagem())
    }

    /** Gratuidade não gera lançamento nenhum: lista vazia é legítima, e não se confunde com ilegível. */
    @Test
    fun `passagem sem lancamento nenhum e valida`() {
        val gratuita = passageiro.copy(lancamentos = emptyList(), tipo = TipoPassagem.INTEIRA)

        assertEquals(gratuita, documentoDe(gratuita).toPassagem())
    }

    // --- O carimbo: ausente ou inteiro ---

    @Test
    fun `sem embarque, o carimbo e ausente`() {
        assertNull(documentoDe(passageiro).toPassagem()?.metadados?.embarque)
    }

    @Test
    fun `com embarque, o carimbo volta inteiro`() {
        val embarcada = passageiro.copy(
            metadados = metadados.copy(
                status = StatusPassagem.EMBARCADA,
                embarque = CarimboEmbarque(porId = "uid-1", em = "2026-08-18T18:04:00"),
            ),
        )

        assertEquals(embarcada, documentoDe(embarcada).toPassagem())
    }

    /** Meio-preenchido não existe: autoria sem instante é descartada por completo, não remendada. */
    @Test
    fun `carimbo sem instante e descartado inteiro`() {
        val meio = passageiro.paraMapa() + ("embarque" to mapOf("porId" to "uid-1"))

        assertNull(documento("pas-1", meio).toPassagem()?.metadados?.embarque)
    }

    // --- O que a escrita não grava ---

    @Test
    fun `o mapa nao carrega o id, que e o nome do documento`() {
        assertNull(passageiro.paraMapa()["id"])
    }

    @Test
    fun `o mapa de passageiro nao carrega campos de veiculo, e vice-versa`() {
        assertNull(passageiro.paraMapa()["veiculoId"])
        assertNull(veiculo.paraMapa()["acomodacao"])
        assertNull(veiculo.paraMapa()["clientes"])
    }

    @Test
    fun `a categoria e gravada como discriminador`() {
        assertEquals("PASSAGEIRO", passageiro.paraMapa()["categoria"])
        assertEquals("VEICULO", veiculo.paraMapa()["categoria"])
    }

    /** A data vai como texto ISO — é ela que ordena, compara sem normalizar e serve de id de ocorrência. */
    @Test
    fun `a data e gravada em ISO`() {
        assertEquals("2026-08-18", passageiro.paraMapa()["data"])
    }
}