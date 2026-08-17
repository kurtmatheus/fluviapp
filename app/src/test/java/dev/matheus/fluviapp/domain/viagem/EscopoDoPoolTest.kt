package dev.matheus.fluviapp.domain.viagem

import dev.matheus.fluviapp.domain.operacoes.Atuacao
import dev.matheus.fluviapp.domain.rota.Rota
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

/**
 * O recorte do pool pela **atuação** (decisão do analista, 2026-08-10): o painel da empresa mostra o que
 * é da empresa, e o pool sem dono não é exceção a isso.
 *
 * Estes casos são o contrato de "ver e vender são a mesma pergunta" — se um dia divergirem, é aqui que a
 * divergência aparece.
 */
class EscopoDoPoolTest {

    private val belem = "porto-belem"
    private val parintins = "porto-parintins"
    private val santarem = "porto-santarem"

    private val rotaConcedida = Rota(id = "r1", portoOrigemId = belem, portoDestinoId = parintins)
    private val rotaComUmaPonta = Rota(id = "r2", portoOrigemId = belem, portoDestinoId = santarem)
    private val rotaDeFora = Rota(id = "r3", portoOrigemId = santarem, portoDestinoId = "porto-manaus")

    private val rotasPorId = listOf(rotaConcedida, rotaComUmaPonta, rotaDeFora).associateBy { it.id }

    private val atuacao = AtuacaoDaEmpresa(
        atuacao = Atuacao.AGENCIAMENTO,
        embarcacaoIds = setOf("emb-1"),
        portoIds = setOf(belem, parintins),
    )

    private fun viagem(id: String, rotaId: String, embarcacaoId: String = "emb-1") = Viagem(
        id = id,
        rotaId = rotaId,
        embarcacaoId = embarcacaoId,
        diaSemana = DayOfWeek.TUESDAY,
        horaMin = 18 * 60,
    )

    // --- Derivação do escopo ---

    @Test
    fun `papel de plataforma ve o pool inteiro`() {
        assertEquals(EscopoDoPool.Todo, escopoDoPool("ADM", null))
        assertEquals(EscopoDoPool.Todo, escopoDoPool("GESTOR", atuacao))
    }

    @Test
    fun `quem tem atuacao ve o que ela concede`() {
        assertEquals(EscopoDoPool.Concedido(atuacao), escopoDoPool("OPERADOR", atuacao))
    }

    /**
     * Sem papel de plataforma e sem atuação **não é** "sem filtro": ausência de recorte e ausência de
     * concessão pareceriam iguais e abririam o pool para quem não deveria ver nada.
     */
    @Test
    fun `sem plataforma e sem atuacao nao ve nada`() {
        assertEquals(EscopoDoPool.Nenhum, escopoDoPool("OPERADOR", null))
        assertEquals(EscopoDoPool.Nenhum, escopoDoPool(null, null))
    }

    // --- Rotas ---

    @Test
    fun `a plataforma ve todas as rotas`() {
        val rotas = rotasPorId.values.toList()

        assertEquals(rotas, rotas.noEscopo(EscopoDoPool.Todo))
    }

    /** Duas pontas concedidas, ou nada: ter só uma delas não é operar a linha. */
    @Test
    fun `a empresa ve so as rotas entre portos concedidos`() {
        val vistas = rotasPorId.values.toList().noEscopo(EscopoDoPool.Concedido(atuacao))

        assertEquals(listOf(rotaConcedida), vistas)
    }

    @Test
    fun `escopo nenhum nao ve rota alguma`() {
        assertTrue(rotasPorId.values.toList().noEscopo(EscopoDoPool.Nenhum).isEmpty())
    }

    // --- Viagens ---

    @Test
    fun `a plataforma ve todas as viagens`() {
        val viagens = listOf(viagem("v1", "r1"), viagem("v2", "r3", embarcacaoId = "emb-9"))

        assertEquals(viagens, viagens.noEscopo(EscopoDoPool.Todo, rotasPorId))
    }

    /** Os dois eixos, e a viagem é quem os junta: a rota traz os portos, ela traz a embarcação. */
    @Test
    fun `a empresa ve a viagem com rota e embarcacao concedidas`() {
        val vista = viagem("v1", rotaConcedida.id, embarcacaoId = "emb-1")

        assertEquals(
            listOf(vista),
            listOf(vista).noEscopo(EscopoDoPool.Concedido(atuacao), rotasPorId),
        )
    }

    @Test
    fun `embarcacao nao concedida esconde a viagem, mesmo em rota concedida`() {
        val viagens = listOf(viagem("v1", rotaConcedida.id, embarcacaoId = "emb-alheia"))

        assertTrue(viagens.noEscopo(EscopoDoPool.Concedido(atuacao), rotasPorId).isEmpty())
    }

    @Test
    fun `rota nao concedida esconde a viagem, mesmo com embarcacao concedida`() {
        val viagens = listOf(viagem("v1", rotaComUmaPonta.id), viagem("v2", rotaDeFora.id))

        assertTrue(viagens.noEscopo(EscopoDoPool.Concedido(atuacao), rotasPorId).isEmpty())
    }

    /**
     * Viagem cuja rota não está no mapa é **órfã** — e mostrá-la seria oferecer uma travessia sem origem
     * nem destino. Fail-closed, como o resto do modelo.
     */
    @Test
    fun `viagem com rota desconhecida some da lista`() {
        val viagens = listOf(viagem("v1", "rota-que-nao-existe"))

        assertTrue(viagens.noEscopo(EscopoDoPool.Concedido(atuacao), rotasPorId).isEmpty())
    }

    /**
     * **Rota inativada esconde a viagem** — o defeito de 2026-08-17.
     *
     * Só a ausência da rota derrubava a viagem, e a porta da Rota não tem `deletar`: a rota tirada de
     * circulação continuava no mapa e continuava autorizando as viagens dela. Se o cadastro não oferece rota
     * inativada, a lista não pode oferecer viagem sobre ela — ver e vender são a mesma pergunta.
     */
    @Test
    fun `rota inativada esconde a viagem, como se ela nao existisse`() {
        val inativada = rotaConcedida.copy(ativo = false)
        val viagens = listOf(viagem("v1", inativada.id))

        assertTrue(
            viagens.noEscopo(
                EscopoDoPool.Concedido(atuacao),
                mapOf(inativada.id to inativada),
            ).isEmpty(),
        )
    }

    /** Nada foi apagado: reativar a rota devolve as viagens dela, sem recadastro. */
    @Test
    fun `reativar a rota devolve a viagem`() {
        val viagens = listOf(viagem("v1", rotaConcedida.id))

        assertEquals(
            viagens,
            viagens.noEscopo(
                EscopoDoPool.Concedido(atuacao),
                mapOf(rotaConcedida.id to rotaConcedida.copy(ativo = true)),
            ),
        )
    }

    /**
     * A plataforma **continua vendo a órfã**: é ela quem cura o pool, e o que ela não vê, não conserta.
     */
    @Test
    fun `a plataforma ve ate a viagem orfa`() {
        val orfa = listOf(viagem("v1", "rota-que-nao-existe"))

        assertEquals(orfa, orfa.noEscopo(EscopoDoPool.Todo, emptyMap()))
    }

    /** E vê também a de rota inativada — é ela quem reativa a rota, e não veria o que precisa reativar. */
    @Test
    fun `a plataforma ve a viagem de rota inativada`() {
        val inativada = rotaConcedida.copy(ativo = false)
        val viagens = listOf(viagem("v1", inativada.id))

        assertEquals(
            viagens,
            viagens.noEscopo(EscopoDoPool.Todo, mapOf(inativada.id to inativada)),
        )
    }

    @Test
    fun `escopo nenhum nao ve viagem alguma`() {
        val viagens = listOf(viagem("v1", rotaConcedida.id))

        assertTrue(viagens.noEscopo(EscopoDoPool.Nenhum, rotasPorId).isEmpty())
    }

    /** O preço aceito da decisão: sem concessão, tela vazia — provisionar virou pré-requisito. */
    @Test
    fun `empresa sem concessao ve tela vazia`() {
        val semNada = AtuacaoDaEmpresa(atuacao = Atuacao.AGENCIAMENTO)
        val viagens = listOf(viagem("v1", rotaConcedida.id))

        assertTrue(viagens.noEscopo(EscopoDoPool.Concedido(semNada), rotasPorId).isEmpty())
        assertTrue(rotasPorId.values.toList().noEscopo(EscopoDoPool.Concedido(semNada)).isEmpty())
    }
}